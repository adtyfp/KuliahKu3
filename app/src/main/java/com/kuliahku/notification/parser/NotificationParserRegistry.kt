package com.kuliahku.notification.parser

import com.kuliahku.data.model.TxType
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTransaction(
    val type: TxType,
    val amount: Long,
    val category: String,
    val merchant: String?
) {
    fun dedupHash(pkg: String, postTime: Long): String {
        // 5 menit bucket untuk deduplikasi, tambahkan type agar tidak bentrok Pemasukan & Pengeluaran
        val bucket = postTime / (5 * 60 * 1000L)
        return "$pkg|$type|$amount|$bucket"
    }
}

interface NotificationParser {
    val packageName: String
    val displayName: String
    fun parse(text: String): ParsedTransaction?
}

@Singleton
class NotificationParserRegistry @Inject constructor(
    parsers: Set<@JvmSuppressWildcards NotificationParser>
) {
    private val byPackage = parsers.associateBy { it.packageName }

    fun isAllowed(pkg: String) = byPackage.containsKey(pkg)
    fun displayName(pkg: String) = byPackage[pkg]?.displayName ?: pkg
    fun parse(pkg: String, text: String) = byPackage[pkg]?.parse(text)
    fun all() = byPackage.values.toList()
}