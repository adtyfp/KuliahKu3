package com.kuliahku.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class TxType(val value: String) {
    INCOME("income"), EXPENSE("expense");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: EXPENSE }
}

enum class TxSource(val value: String) {
    MANUAL("manual"), NOTIFICATION("notification");
    companion object { fun from(v: String?) = entries.firstOrNull { it.value == v } ?: MANUAL }
}

data class Transaction(
    @DocumentId val id: String = "",
    val type: String = TxType.EXPENSE.value,
    val category: String = "",
    val amount: Long = 0L,
    val description: String? = null,
    val date: Timestamp? = null,
    val monthKey: String = "",
    val yearKey: String = "",
    val source: String = TxSource.MANUAL.value,
    val dedupHash: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val typeEnum: TxType get() = TxType.from(type)
    val sourceEnum: TxSource get() = TxSource.from(source)
}

object FinanceCategories {
    val EXPENSE = listOf(
        "Makan", "Transport", "Kos", "Pulsa/Internet",
        "Kuliah/Buku", "Hiburan", "Kesehatan", "Lainnya"
    )
    val INCOME = listOf("Uang Saku", "Beasiswa", "Gaji/Freelance", "Lainnya")

    fun forType(type: TxType) = when (type) {
        TxType.EXPENSE -> EXPENSE
        TxType.INCOME -> INCOME
    }
}