package com.kuliahku.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.repository.TransactionRepository
import com.kuliahku.ai.AiRepository
import com.kuliahku.ai.AiResult
import com.kuliahku.util.FinanceStats
import com.kuliahku.util.MonthKey
import com.kuliahku.util.MonthlySummary
import com.kuliahku.util.YearlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isAiLoading: Boolean = false
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
    private val aiRepo: AiRepository
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

}