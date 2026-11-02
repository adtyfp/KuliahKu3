package com.kuliahku.ui.finance

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kuliahku.R
import com.kuliahku.data.model.UserSettings
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.repository.NotificationRepository
import com.kuliahku.data.repository.UserRepository
import com.kuliahku.data.repository.TransactionRepository
import com.kuliahku.notification.NotificationChannels
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.ai.AiRepository
import com.kuliahku.ai.AiResult
import com.kuliahku.util.FinanceStats
import com.kuliahku.util.MonthKey
import com.kuliahku.util.MonthlySummary
import com.kuliahku.util.YearlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class FinanceUiState(
    val currentMonthCal: Calendar = Calendar.getInstance(),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val monthTransactions: List<Transaction> = emptyList(),
    val yearTransactions: List<Transaction> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(),
    val yearlySummary: YearlySummary = YearlySummary(),
    val latestTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val chartTab: ChartTab = ChartTab.CHART,
    val chartType: TxChartType = TxChartType.EXPENSE,
    val pendingDelete: Transaction? = null,
    val exportInProgress: Boolean = false,
    val aiInsight: String? = null,
    val isAiLoading: Boolean = false,
    val settings: UserSettings = UserSettings()
)

enum class ChartTab { CHART, TABLE }
enum class TxChartType { INCOME, EXPENSE }

sealed interface FinanceEvent {
    data class ShowMessage(val message: String) : FinanceEvent
    data class Share(val uri: android.net.Uri, val mime: String, val title: String) : FinanceEvent
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val aiRepo: AiRepository,
    private val userRepo: UserRepository,
    private val notifRepo: NotificationRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<FinanceEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.observeAll()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    val s = _uiState.value
                    val monthKey = MonthKey.of(s.currentMonthCal)
                    val yearKey = s.currentYear.toString()
                    val monthList = list.filter { it.monthKey == monthKey }
                    val yearList = list.filter { it.yearKey == yearKey }
                    val monthlySummary = FinanceStats.monthlySummary(monthList)
                    _uiState.update {
                        it.copy(
                            monthTransactions = monthList,
                            yearTransactions = yearList,
                            monthlySummary = monthlySummary,
                            yearlySummary = FinanceStats.yearlySummary(yearList),
                            latestTransactions = monthList.sortedByDescending { t -> t.date?.toDate()?.time ?: 0L }.take(3),
                            isLoading = false,
                            error = null
                        )
                    }
                    if (monthList.isNotEmpty()) fetchAiInsight(monthlySummary)
                    checkDailyLimit(monthlySummary, s.settings.dailyLimit)
                }
        }
        viewModelScope.launch {
            auth.currentUser?.uid?.let { uid ->
                userRepo.observeUser(uid).collect { user ->
                    user?.let { u ->
                        _uiState.update { it.copy(settings = u.settings) }
                    }
                }
            }
        }
    }

    private fun fetchAiInsight(summary: MonthlySummary) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val result = aiRepo.getFinanceInsight(summary)
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiInsight = if (result is AiResult.Success) result.data else null
                )
            }
        }
    }

    fun setMonth(cal: Calendar) {
        _uiState.update { it.copy(currentMonthCal = cal) }
        refreshFromCache()
    }

    fun previousMonth() {
        _uiState.update {
            val c = it.currentMonthCal.clone() as Calendar
            c.add(Calendar.MONTH, -1)
            it.copy(currentMonthCal = c)
        }
        refreshFromCache()
    }

    fun nextMonth() {
        _uiState.update {
            val c = it.currentMonthCal.clone() as Calendar
            c.add(Calendar.MONTH, 1)
            it.copy(currentMonthCal = c)
        }
        refreshFromCache()
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(currentYear = year) }
        refreshFromCache()
    }

    fun setChartTab(tab: ChartTab) { _uiState.update { it.copy(chartTab = tab) } }
    fun setChartType(t: TxChartType) { _uiState.update { it.copy(chartType = t) } }

    fun requestDelete(tx: Transaction) { _uiState.update { it.copy(pendingDelete = tx) } }
    fun cancelDelete() { _uiState.update { it.copy(pendingDelete = null) } }

    fun confirmDelete() {
        val tx = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            repo.delete(tx.id)
                .onSuccess { _events.send(FinanceEvent.ShowMessage("Transaksi dihapus.")) }
                .onFailure { _events.send(FinanceEvent.ShowMessage(it.message ?: "Gagal menghapus.")) }
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    fun undoDelete(tx: Transaction) {
        viewModelScope.launch {
            repo.restore(tx).onFailure {
                _events.send(FinanceEvent.ShowMessage(it.message ?: "Gagal memulihkan."))
            }
        }
    }

    private fun refreshFromCache() {
        val s = _uiState.value
        val monthKey = MonthKey.of(s.currentMonthCal)
        val yearKey = s.currentYear.toString()
        viewModelScope.launch {
            repo.observeAll().first().let { list ->
                val monthList = list.filter { it.monthKey == monthKey }
                val yearList = list.filter { it.yearKey == yearKey }
                _uiState.update {
                    it.copy(
                        monthTransactions = monthList,
                        yearTransactions = yearList,
                        monthlySummary = FinanceStats.monthlySummary(monthList),
                        yearlySummary = FinanceStats.yearlySummary(yearList),
                        latestTransactions = monthList.sortedByDescending { t -> t.date?.toDate()?.time ?: 0L }.take(3)
                    )
                }
            }
        }
    }

    fun exportCsv(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportInProgress = true) }
            try {
                val result = com.kuliahku.util.FinanceExport.buildCsv(context, _uiState.value.monthTransactions)
                _events.send(FinanceEvent.Share(result.component1(), result.component2(), result.component3()))
            } catch (e: Exception) {
                _events.send(FinanceEvent.ShowMessage(e.message ?: "Gagal ekspor."))
            } finally {
                _uiState.update { it.copy(exportInProgress = false) }
            }
        }
    }

    fun updateDailyLimit(limit: Long) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val newSettings = _uiState.value.settings.copy(dailyLimit = limit)
            userRepo.updateSettings(uid, newSettings)
                .onFailure { _events.send(FinanceEvent.ShowMessage("Gagal memperbarui batas harian")) }
            
            // Re-check limit immediately after update
            checkDailyLimit(_uiState.value.monthlySummary, limit, forceNotify = true)
        }
    }

    private var lastNotifiedKey = ""

    private fun checkDailyLimit(summary: MonthlySummary, limit: Long, forceNotify: Boolean = false) {
        if (limit <= 0) return
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val key = "$today:$limit"

        if (summary.todayExpense > limit) {
            if (forceNotify || lastNotifiedKey != key) {
                lastNotifiedKey = key
                viewModelScope.launch {
                    val title = "Batas Harian Terlampaui!"
                    val body = "Pengeluaran hari ini Rp ${summary.todayExpense} telah melebihi batas Rp $limit."
                    
                    // 1. Simpan ke inbox in-app
                    notifRepo.push(
                        title = title,
                        body = body,
                        type = "finance",
                        deepLink = "kuliahku://finance"
                    )

                    // 2. Tampilkan notifikasi sistem
                    showSystemNotification(title, body)
                }
            }
        } else {
            // Reset if spending is back under limit to allow re-triggering later
            if (lastNotifiedKey == key) {
                lastNotifiedKey = ""
            }
        }
    }

    private fun showSystemNotification(title: String, body: String) {
        val intent = android.content.Intent(context, com.kuliahku.MainActivity::class.java).apply {
            data = android.net.Uri.parse("kuliahku://finance")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = android.app.PendingIntent.getActivity(
            context,
            1001,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.FINANCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) { }
    }

}