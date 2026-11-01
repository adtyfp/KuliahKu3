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
import com.kuliahku.util.DateFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
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
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEventUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddEventEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val editId: String? = savedState.get<String>("editId")
        val dateKey: String? = savedState.get<String>("date")
        // Prefill dari dateKey
        if (!dateKey.isNullOrBlank()) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val parsed = sdf.parse(dateKey)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }
                    val end = (cal.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
                    _uiState.update {
                        it.copy(
                            startMillis = cal.timeInMillis,
                            endMillis = end.timeInMillis,
                            startLabel = formatLabel(cal.timeInMillis),
                            endLabel = formatLabel(end.timeInMillis)
                        )
                    }
                }
            } catch (_: Exception) { }
        }
        if (!editId.isNullOrBlank()) load(editId)
        else updateLabels()
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEdit = true, eventId = id) }
            repo.observeAll().first().firstOrNull { it.id == id }?.let { ev ->
                _uiState.update {
                    it.copy(
                        type = ev.typeEnum,
                        title = ev.title,
                        allDay = ev.allDay,
                        startMillis = ev.start?.toDate()?.time ?: System.currentTimeMillis(),
                        endMillis = ev.end?.toDate()?.time ?: System.currentTimeMillis(),
                        repeat = ev.repeatEnum,
                        reminderStyle = ev.reminderStyleEnum,
                        reminderOffsetMinutes = ev.reminderOffsetMinutes,
                        isLoading = false,
                        startLabel = formatLabel(ev.start?.toDate()?.time ?: 0L),
                        endLabel = formatLabel(ev.end?.toDate()?.time ?: 0L)
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTypeChange(t: EventType) { _uiState.update { it.copy(type = t) } }
    fun onTitleChange(v: String) { _uiState.update { it.copy(title = v, titleError = null) } }
    fun onAllDayChange(v: Boolean) { _uiState.update { it.copy(allDay = v) } }
    fun onRepeatChange(r: EventRepeat) { _uiState.update { it.copy(repeat = r) } }
    fun onStyleChange(s: ReminderStyle) { _uiState.update { it.copy(reminderStyle = s) } }
    fun onOffsetChange(m: Int?) { _uiState.update { it.copy(reminderOffsetMinutes = m) } }

    fun onStartChange(millis: Long) {
        _uiState.update {
            it.copy(
                startMillis = millis,
                endMillis = if (it.endMillis <= millis) millis + 60 * 60 * 1000 else it.endMillis,
                startLabel = formatLabel(millis),
                timeError = null
            )
        }
        updateLabels()
    }

    fun onEndChange(millis: Long) {
        val s = _uiState.value
        if (millis < s.startMillis) {
            _uiState.update { it.copy(timeError = "Waktu berakhir tidak boleh sebelum waktu mulai.") }
            return
        }
        _uiState.update { it.copy(endMillis = millis, endLabel = formatLabel(millis), timeError = null) }
    }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) { _uiState.update { it.copy(titleError = "Judul wajib diisi.") }; return }
        if (s.endMillis < s.startMillis) { _uiState.update { it.copy(timeError = "Waktu berakhir tidak valid.") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val ev = Event(
                id = s.eventId ?: "",
                type = s.type.value,
                title = s.title.trim(),
                allDay = s.allDay,
                start = Timestamp(java.util.Date(s.startMillis)),
                end = Timestamp(java.util.Date(s.endMillis)),
                repeat = s.repeat.value,
                reminderStyle = s.reminderStyle.value,
                reminderOffsetMinutes = if (s.reminderStyle == ReminderStyle.BANNER) null else s.reminderOffsetMinutes
            )
            repo.upsert(ev)
                .onSuccess { _events.send(AddEventEvent.Saved) }
                .onFailure { _events.send(AddEventEvent.ShowMessage(it.message ?: "Gagal menyimpan.")) }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun updateLabels() {
        val s = _uiState.value
        _uiState.update {
            it.copy(startLabel = formatLabel(s.startMillis), endLabel = formatLabel(s.endMillis))
        }
    }

    private fun formatLabel(millis: Long): String {
        val d = java.util.Date(millis)
        return "${DateFormatters.formatMediumDate(d)} ${DateFormatters.formatTime(d)}"
    }
}