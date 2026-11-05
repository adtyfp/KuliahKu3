
package com.kuliahku.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.kuliahku.data.model.Event
import com.kuliahku.data.model.EventRepeat
import com.kuliahku.data.model.EventType
import com.kuliahku.data.model.ReminderStyle
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.notification.ReminderScheduler
import com.kuliahku.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AddEventUiState(
    val eventId: String? = null,
    val type: EventType = EventType.ACARA,
    val title: String = "",
    val allDay: Boolean = false,
    val startMillis: Long = System.currentTimeMillis(),
    val endMillis: Long = System.currentTimeMillis() + 60 * 60 * 1000,
    val startLabel: String = "",
    val endLabel: String = "",
    val repeat: EventRepeat = EventRepeat.NONE,
    val reminderStyle: ReminderStyle = ReminderStyle.BANNER,
    val reminderOffsetMinutes: Int? = 10,
    val titleError: String? = null,
    val timeError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEdit: Boolean = false
)

sealed interface AddEventEvent {
    data object Saved : AddEventEvent
    data class ShowMessage(val message: String) : AddEventEvent
}

@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val repo: EventRepository,
    private val scheduler: ReminderScheduler,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEventUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddEventEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val editId: String? = savedState.get("editId")
        val dateKey: String? = savedState.get("date")

        if (!dateKey.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsed = sdf.parse(dateKey)

                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val end = (cal.clone() as Calendar).apply {
                        add(Calendar.HOUR_OF_DAY, 1)
                    }

                    _uiState.update {
                        it.copy(
                            startMillis = cal.timeInMillis,
                            endMillis = end.timeInMillis,
                            startLabel = formatLabel(cal.timeInMillis),
                            endLabel = formatLabel(end.timeInMillis)
                        )
                    }
                }
            } catch (_: Exception) {
                // Abaikan dateKey yang tidak valid.
            }
        }

        if (!editId.isNullOrBlank()) {
            load(editId)
        } else {
            updateLabels()
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isEdit = true,
                    eventId = id
                )
            }

            try {
                val event = repo.observeAll()
                    .first()
                    .firstOrNull { it.id == id }

                if (event != null) {
                    val start = event.start?.toDate()?.time
                        ?: System.currentTimeMillis()
                    val end = event.end?.toDate()?.time
                        ?: start + 60 * 60 * 1000

                    _uiState.update {
                        it.copy(
                            eventId = event.id,
                            type = event.typeEnum,
                            title = event.title,
                            allDay = event.allDay,
                            startMillis = start,
                            endMillis = end,
                            repeat = event.repeatEnum,
                            reminderStyle = event.reminderStyleEnum,
                            reminderOffsetMinutes = event.reminderOffsetMinutes,
                            isLoading = false,
                            isEdit = true,
                            startLabel = formatLabel(start),
                            endLabel = formatLabel(end)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    _events.send(
                        AddEventEvent.ShowMessage("Acara tidak ditemukan.")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false)
                }
                _events.send(
                    AddEventEvent.ShowMessage(
                        e.message ?: "Gagal memuat acara."
                    )
                )
            }
        }
    }

    fun onTypeChange(type: EventType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onTitleChange(value: String) {
        _uiState.update {
            it.copy(title = value, titleError = null)
        }
    }

    fun onAllDayChange(value: Boolean) {
        _uiState.update { it.copy(allDay = value) }
    }

    fun onRepeatChange(repeat: EventRepeat) {
        _uiState.update { it.copy(repeat = repeat) }
    }

    fun onStyleChange(style: ReminderStyle) {
        _uiState.update { it.copy(reminderStyle = style) }
    }

    fun onOffsetChange(minutes: Int?) {
        _uiState.update {
            it.copy(reminderOffsetMinutes = minutes)
        }
    }

    fun onStartChange(millis: Long) {
        _uiState.update { state ->
            val newEnd = if (state.endMillis <= millis) {
                millis + 60 * 60 * 1000
            } else {
                state.endMillis
            }

            state.copy(
                startMillis = millis,
                endMillis = newEnd,
                startLabel = formatLabel(millis),
                endLabel = formatLabel(newEnd),
                timeError = null
            )
        }
    }

    fun onEndChange(millis: Long) {
        val state = _uiState.value

        if (millis < state.startMillis) {
            _uiState.update {
                it.copy(
                    timeError = "Waktu berakhir tidak boleh sebelum waktu mulai."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                endMillis = millis,
                endLabel = formatLabel(millis),
                timeError = null
            )
        }
    }

    fun save() {
        val state = _uiState.value

        if (state.isSaving) return

        if (state.title.isBlank()) {
            _uiState.update {
                it.copy(titleError = "Judul wajib diisi.")
            }
            return
        }

        if (state.endMillis < state.startMillis) {
            _uiState.update {
                it.copy(timeError = "Waktu berakhir tidak valid.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true)
            }

            try {
                val event = Event(
                    id = state.eventId ?: "",
                    type = state.type.value,
                    title = state.title.trim(),
                    allDay = state.allDay,
                    start = Timestamp(Date(state.startMillis)),
                    end = Timestamp(Date(state.endMillis)),
                    repeat = state.repeat.value,
                    reminderStyle = state.reminderStyle.value,
                    reminderOffsetMinutes =
                        if (state.reminderStyle == ReminderStyle.BANNER) {
                            null
                        } else {
                            state.reminderOffsetMinutes
                        }
                )

                repo.upsert(event)
                    .onSuccess { savedId ->
                        // Gunakan ID dokumen Firestore yang sebenarnya.
                        val savedEvent = event.copy(id = savedId)

                        // Simpan ID agar perubahan berikutnya tetap memakai ID yang sama.
                        _uiState.update {
                            it.copy(
                                eventId = savedId,
                                isEdit = true
                            )
                        }

                        // Jadwalkan pengingat dengan ID yang valid.
                        scheduler.scheduleEventReminder(savedEvent)

                        _events.send(AddEventEvent.Saved)
                    }
                    .onFailure { error ->
                        _events.send(
                            AddEventEvent.ShowMessage(
                                error.message ?: "Gagal menyimpan acara."
                            )
                        )
                    }
            } catch (e: Exception) {
                _events.send(
                    AddEventEvent.ShowMessage(
                        e.message ?: "Terjadi kesalahan saat menyimpan acara."
                    )
                )
            } finally {
                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    private fun updateLabels() {
        val state = _uiState.value

        _uiState.update {
            it.copy(
                startLabel = formatLabel(state.startMillis),
                endLabel = formatLabel(state.endMillis)
            )
        }
    }

    private fun formatLabel(millis: Long): String {
        val date = Date(millis)

        return "${DateFormatters.formatMediumDate(date)} " +
                DateFormatters.formatTime(date)
    }
}
