package com.kuliahku.ui.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.kuliahku.data.model.FinanceCategories
import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import com.kuliahku.data.repository.TransactionRepository
import com.kuliahku.util.CurrencyFormatter
import com.kuliahku.util.DateFormatters
import com.kuliahku.util.MonthKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddTransactionUiState(
    val txId: String? = null,
    val isEdit: Boolean = false,
    val type: TxType = TxType.EXPENSE,
    val amountText: String = "",
    val amountLong: Long = 0L,
    val category: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val dateLabel: String = "",
    val description: String = "",
    val categories: List<String> = FinanceCategories.EXPENSE,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

sealed interface AddTransactionEvent {
    data object Saved : AddTransactionEvent
    data class ShowMessage(val message: String) : AddTransactionEvent
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo: TransactionRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddTransactionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        updateDateLabel()
        savedState.get<String>("editId")?.let { load(it) }
    }

    fun onTypeChange(t: TxType) {
        _uiState.update {
            it.copy(
                type = t,
                categories = FinanceCategories.forType(t),
                category = "",
                categoryError = null
            )
        }
    }

    fun onAmountChange(input: String) {
        // hanya angka
        val digits = input.filter { it.isDigit() }
        val long = digits.toLongOrNull() ?: 0L
        _uiState.update {
            it.copy(
                amountText = if (digits.isEmpty()) "" else CurrencyFormatter.formatPlain(long),
                amountLong = long,
                amountError = null
            )
        }
    }

    fun onCategoryChange(c: String) { _uiState.update { it.copy(category = c, categoryError = null) } }
    fun onDescriptionChange(v: String) { _uiState.update { it.copy(description = v) } }

    fun onDateChange(millis: Long) {
        _uiState.update { it.copy(dateMillis = millis) }
        updateDateLabel()
    }

    private fun updateDateLabel() {
        val d = java.util.Date(_uiState.value.dateMillis)
        _uiState.update { it.copy(dateLabel = DateFormatters.formatDateTime(d)) }
    }

    fun save() {
        val s = _uiState.value
        val errors = mutableMapOf<String, String?>()
        if (s.amountLong <= 0L) errors["amount"] = "Jumlah uang wajib diisi."
        if (s.category.isBlank()) errors["category"] = "Pilih kategori."
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(amountError = errors["amount"], categoryError = errors["category"]) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val date = java.util.Date(s.dateMillis)
            val tx = Transaction(
                id = s.txId ?: "",
                type = s.type.value,
                category = s.category,
                amount = s.amountLong,
                description = s.description.ifBlank { null },
                date = Timestamp(date),
                monthKey = MonthKey.of(date),
                yearKey = MonthKey.yearOf(date),
                source = "manual"
            )
            repo.upsert(tx)
                .onSuccess { _events.send(AddTransactionEvent.Saved) }
                .onFailure { _events.send(AddTransactionEvent.ShowMessage(it.message ?: "Gagal menyimpan.")) }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            repo.observeAll().first().firstOrNull { it.id == id }?.let { tx ->
                _uiState.update {
                    it.copy(
                        txId = id,
                        isEdit = true,
                        type = tx.typeEnum,
                        amountLong = tx.amount,
                        amountText = CurrencyFormatter.formatPlain(tx.amount),
                        category = tx.category,
                        dateMillis = tx.date?.toDate()?.time ?: System.currentTimeMillis(),
                        description = tx.description ?: "",
                        categories = FinanceCategories.forType(tx.typeEnum)
                    )
                }
                updateDateLabel()
            }
        }
    }
}