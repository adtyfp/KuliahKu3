package com.kuliahku.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Event
import com.kuliahku.data.model.Task
import com.kuliahku.data.repository.EventRepository
import com.kuliahku.data.repository.TaskRepository
import com.kuliahku.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed interface CalendarEntry {
    val id: String
    val title: String
    val startMillis: Long
    val endMillis: Long
    data class Acara(
        override val id: String, override val title: String,
        override val startMillis: Long, override val endMillis: Long,
        val event: Event
    ) : CalendarEntry
    data class Tugas(
        override val id: String, override val title: String,
        override val startMillis: Long, override val endMillis: Long,
        val task: Task
    ) : CalendarEntry
}

data class CalendarUiState(
    val currentMonth: Calendar = Calendar.getInstance(),
    val selectedDate: Calendar = Calendar.getInstance(),
    val events: List<Event> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val daysWithEntries: Set<String> = emptySet(), // yyyy-MM-dd
    val selectedDayEntries: List<CalendarEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val pendingDeleteEvent: Event? = null,
    val pendingDeleteOccurrenceKey: String? = null,
    val showRepeatDeleteDialog: Boolean = false
)

sealed interface CalendarEvent {
    data class ShowMessage(val message: String) : CalendarEvent
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepo: EventRepository,
    private val taskRepo: TaskRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(eventRepo.observeAll(), taskRepo.observeAllTasks()) { events, tasks ->
                events to tasks
            }.catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (events, tasks) ->
                    _uiState.update { it.copy(events = events, tasks = tasks, isLoading = false, error = null) }
                    recompute()
                }
        }
    }

    fun previousMonth() {
        _uiState.update {
            val cal = it.currentMonth.clone() as Calendar
            cal.add(Calendar.MONTH, -1)
            it.copy(currentMonth = cal)
        }
        recompute()
    }

    fun nextMonth() {
        _uiState.update {
            val cal = it.currentMonth.clone() as Calendar
            cal.add(Calendar.MONTH, 1)
            it.copy(currentMonth = cal)
        }
        recompute()
    }

    fun selectDate(date: Calendar) {
        _uiState.update { it.copy(selectedDate = date) }
        recompute()
    }

    fun requestDelete(event: Event, occurrenceKey: String?) {
        _uiState.update {
            it.copy(
                pendingDeleteEvent = event,
                pendingDeleteOccurrenceKey = occurrenceKey,
                showRepeatDeleteDialog = event.repeatEnum != com.kuliahku.data.model.EventRepeat.NONE
            )
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeleteEvent = null, pendingDeleteOccurrenceKey = null, showRepeatDeleteDialog = false) }
    }

    fun deleteOnlyThis() {
        val ev = _uiState.value.pendingDeleteEvent ?: return
        val key = _uiState.value.pendingDeleteOccurrenceKey ?: return
        viewModelScope.launch {
            eventRepo.excludeOccurrence(ev.id, key)
                .onSuccess {
                    scheduler.scheduleEventReminder(ev) // Reschedule untuk occurrence berikutnya
                    _events.send(CalendarEvent.ShowMessage("Kemunculan ini dihapus."))
                }
                .onFailure { _events.send(CalendarEvent.ShowMessage(it.message ?: "Gagal.")) }
            cancelDelete()
        }
    }

    fun deleteAllOccurrences() {
        val ev = _uiState.value.pendingDeleteEvent ?: return
        viewModelScope.launch {
            eventRepo.delete(ev.id)
                .onSuccess {
                    scheduler.cancelEventReminders(ev.id)
                    _events.send(CalendarEvent.ShowMessage("Acara dihapus."))
                }
                .onFailure { _events.send(CalendarEvent.ShowMessage(it.message ?: "Gagal.")) }
            cancelDelete()
        }
    }

    private fun recompute() {
        val s = _uiState.value

        // Hitung hari dengan entri (Range: 1 bulan sebelum sampai 1 bulan sesudah currentMonth untuk animasi smooth)
        val daysWithEntries = mutableSetOf<String>()
        val rangeStart = (s.currentMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val rangeEnd = (s.currentMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, 2) // Sampai awal bulan berikutnya lagi
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val tempCal = rangeStart.clone() as Calendar
        while (tempCal.before(rangeEnd)) {
            val key = EventRepository.keyOf(tempCal)
            val hasEvent = s.events.any { eventRepo.occursOn(it, tempCal) }
            val hasTask = s.tasks.any { task ->
                task.deadline?.toDate()?.let { sameDay(it, tempCal.time) } ?: false
            }
            if (hasEvent || hasTask) daysWithEntries.add(key)
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Entri untuk tanggal terpilih
        val selected = s.selectedDate
        val entries = mutableListOf<CalendarEntry>()
        s.events.filter { eventRepo.occursOn(it, selected) }.forEach { event ->
            val startCal = Calendar.getInstance().apply { time = event.start?.toDate() ?: return@forEach }
            val startOnSel = Calendar.getInstance().apply {
                time = selected.time
                set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, startCal.get(Calendar.MINUTE))
            }
            val endCal = Calendar.getInstance().apply {
                time = event.end?.toDate() ?: event.start?.toDate() ?: return@forEach
                set(Calendar.YEAR, selected.get(Calendar.YEAR))
                set(Calendar.MONTH, selected.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH))
            }
            entries.add(
                CalendarEntry.Acara(
                    id = event.id,
                    title = event.title,
                    startMillis = startOnSel.timeInMillis,
                    endMillis = endCal.timeInMillis,
                    event = event
                )
            )
        }
        s.tasks.filter { task ->
            task.deadline?.toDate()?.let { sameDay(it, selected.time) } ?: false
        }.forEach { task ->
            val d = task.deadline!!.toDate()
            entries.add(
                CalendarEntry.Tugas(
                    id = task.id,
                    title = task.title,
                    startMillis = d.time,
                    endMillis = d.time,
                    task = task
                )
            )
        }
        entries.sortBy { it.startMillis }

        _uiState.update { it.copy(daysWithEntries = daysWithEntries, selectedDayEntries = entries) }
    }

    private fun sameDay(a: java.util.Date, b: java.util.Date): Boolean {
        val ca = Calendar.getInstance().apply { time = a }
        val cb = Calendar.getInstance().apply { time = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}