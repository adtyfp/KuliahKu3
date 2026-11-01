package com.kuliahku.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val localeId = Locale("id", "ID")
    private val nf: NumberFormat = NumberFormat.getNumberInstance(localeId)

    /** "Rp. 1.500.000" */
    fun format(amount: Long): String = "Rp. ${nf.format(amount)}"

    /** "1.500.000" tanpa prefix (untuk input) */
    fun formatPlain(amount: Long): String = nf.format(amount)

    /** Parse input "1.500.000" -> 1500000 */
    fun parse(text: String): Long? {
        val clean = text.replace(".", "").replace(",", "").trim()
        return clean.toLongOrNull()
    }

    /** Format untuk marker chart: "1.100k" */
    fun formatCompact(amount: Long): String = when {
        amount >= 1_000_000 -> "${amount / 1_000_000}M"
        amount >= 1_000 -> "${amount / 1_000}k"
        else -> amount.toString()
    }
}