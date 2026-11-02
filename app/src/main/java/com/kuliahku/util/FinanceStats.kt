package com.kuliahku.util

import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import java.util.Calendar

data class MonthlySummary(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val todayExpense: Long = 0L,
    val weeklyExpense: Map<String, Long> = emptyMap()
)

data class YearlyPoint(val monthIndex: Int, val income: Long, val expense: Long)
data class YearlySummary(val points: List<YearlyPoint> = emptyList())

object FinanceStats {

    fun monthlySummary(list: List<Transaction>): MonthlySummary {
        var income = 0L
        var expense = 0L
        var todayExpense = 0L
        val weekly = mutableMapOf<String, Long>()
        MonthKey.BUCKETS.forEach { weekly[it] = 0L }

        val today = Calendar.getInstance()
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        val todayMonthKey = MonthKey.of(today)

        for (t in list) {
            when (t.typeEnum) {
                TxType.INCOME -> income += t.amount
                TxType.EXPENSE -> {
                    expense += t.amount
                    val cal = t.date?.toDate()?.let {
                        Calendar.getInstance().apply { time = it }
                    } ?: continue
                    
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val bucket = MonthKey.weekBucket(day)
                    weekly[bucket] = (weekly[bucket] ?: 0L) + t.amount

                    if (day == todayDay && t.monthKey == todayMonthKey) {
                        todayExpense += t.amount
                    }
                }
            }
        }
        return MonthlySummary(income, expense, todayExpense, weekly)
    }

    fun yearlySummary(list: List<Transaction>): YearlySummary {
        val income = LongArray(12)
        val expense = LongArray(12)
        for (t in list) {
            val cal = t.date?.toDate()?.let { Calendar.getInstance().apply { time = it } } ?: continue
            val m = cal.get(Calendar.MONTH)
            when (t.typeEnum) {
                TxType.INCOME -> income[m] += t.amount
                TxType.EXPENSE -> expense[m] += t.amount
            }
        }
        return YearlySummary(points = (0..11).map { YearlyPoint(it, income[it], expense[it]) })
    }
}