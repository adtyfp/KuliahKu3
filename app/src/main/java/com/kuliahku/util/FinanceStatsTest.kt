package com.kuliahku.util

import com.google.firebase.Timestamp
import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FinanceStatsTest {

    private fun tx(day: Int, amount: Long, type: TxType): Transaction {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, day, 12, 0)
        }
        return Transaction(
            type = type.value,
            amount = amount,
            date = Timestamp(cal.time),
            monthKey = MonthKey.of(cal),
            yearKey = MonthKey.yearOf(cal.time)
        )
    }

    @Test fun weekly_bucket_1_6_includes_day_1_to_6() {
        val list = listOf(
            tx(1, 100_000, TxType.EXPENSE),
            tx(6, 200_000, TxType.EXPENSE)
        )
        val summary = FinanceStats.monthlySummary(list)
        assertEquals(300_000L, summary.weeklyExpense["1-6"])
        assertEquals(0L, summary.weeklyExpense["7-13"])
    }

    @Test fun weekly_bucket_28_31_includes_day_28_to_31() {
        val list = listOf(
            tx(28, 50_000, TxType.EXPENSE),
            tx(31, 30_000, TxType.EXPENSE)
        )
        val summary = FinanceStats.monthlySummary(list)
        assertEquals(80_000L, summary.weeklyExpense["28-31"])
    }

    @Test fun monthly_summary_separates_income_and_expense() {
        val list = listOf(
            tx(5, 100_000, TxType.INCOME),
            tx(6, 40_000, TxType.EXPENSE)
        )
        val summary = FinanceStats.monthlySummary(list)
        assertEquals(100_000L, summary.totalIncome)
        assertEquals(40_000L, summary.totalExpense)
    }

    @Test fun yearly_summary_distributes_by_month() {
        val list = listOf(
            tx(5, 100_000, TxType.INCOME), // Oktober (index 9)
            tx(6, 40_000, TxType.EXPENSE)
        )
        val year = FinanceStats.yearlySummary(list)
        assertEquals(100_000L, year.points[9].income)
        assertEquals(40_000L, year.points[9].expense)
        assertEquals(0L, year.points[0].income)
    }
}