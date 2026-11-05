package com.kuliahku.notification.parser

import com.kuliahku.data.model.TxType
import javax.inject.Inject
import javax.inject.Singleton

/** Helper: konversi "1.500.000", "Rp1.500.000,00", atau "IDR 5,000.00" ke Long. */
private val BLACKLIST_KEYWORDS = listOf(
    "otp", "keamanan", "aktivasi", "syarat", "ketentuan",
    "promo", "diskon", "cashback", "suku bunga", "poin", "point"
)

private fun isNotTransaction(text: String): Boolean {
    if (BLACKLIST_KEYWORDS.any { text.contains(it, ignoreCase = true) }) return true
    
    val txKeywords = listOf(
        "masuk", "terima", "kredit", "transfer", "bayar", 
        "keluar", "debit", "pembelian", "berhasil", "paid", "sent", "received",
        "pemasukan", "pengeluaran", "dana masuk"
    )
    val hasTx = txKeywords.any { text.contains(it, ignoreCase = true) }
    
    val infoKeywords = listOf("saldo anda", "sisa limit", "informasi rekening", "status rekening")
    val hasInfo = infoKeywords.any { text.contains(it, ignoreCase = true) }
    
    if (hasInfo && !hasTx) return true
    
    return false
}

private fun parseAmount(s: String): Long? {
    var clean = s.replace("Rp", "", true)
        .replace("IDR", "", true)
        .replace(" ", "")
        .trim()

    val decimalMatch = Regex("""[.,](\d{2})$""").find(clean)
    if (decimalMatch != null) {
        clean = clean.substring(0, decimalMatch.range.first)
    }

    val digits = clean.filter { it.isDigit() }
    return digits.toLongOrNull()
}

/** Regex umum untuk menangkap nominal uang setelah mata uang */
private val AMOUNT_REGEX = Regex("""(?:Rp|IDR)\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)

private fun cleanForMerchant(text: String): String {
    return text.replace(Regex("""(?:Rp|IDR)\.?\s*[\d.,]+""", RegexOption.IGNORE_CASE), "")
        .replace("Catatan Finansial", "", true)
        .replace("berhasil", "", true)
        .replace("sebesar", "", true)
        .replace("pemasukan", "", true)
        .replace("pengeluaran", "", true)
        .replace("transaksi", "", true)
        .replace("|", "")
        .trim()
}

@Singleton
class MyBcaParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bca.mybca.omni.android"
    override val displayName = "myBCA"

    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        
        val isIncome = text.contains("masuk", true) || 
                       text.contains("terima", true) || 
                       text.contains("pemasukan", true) ||
                       text.contains("kredit", true)
                       
        val isExpense = text.contains("keluar", true) || 
                        text.contains("pengeluaran", true) || 
                        text.contains("transfer ke", true) ||
                        text.contains("bayar", true) ||
                        text.contains("debit", true)

        val type = when {
            isIncome && !isExpense -> TxType.INCOME
            isExpense -> TxType.EXPENSE
            else -> if (isIncome) TxType.INCOME else return null
        }
        
        val merchant = extractMerchant(text)
        return ParsedTransaction(type, amount, if (type == TxType.INCOME) "Lainnya" else categoryFromMerchant(merchant), merchant)
    }

    private fun extractMerchant(text: String): String? {
        val clean = cleanForMerchant(text)
        return when {
            clean.contains(" dari ", true) -> clean.substringAfter(" dari ").substringBefore(" di ").substringBefore(" kategori ").trim()
            clean.contains(" ke ", true) -> clean.substringAfter(" ke ").substringBefore(" di ").substringBefore(" kategori ").trim()
            else -> null
        }
    }

    private fun categoryFromMerchant(m: String?): String = when {
        m == null -> "Lainnya"
        m.contains("GO", true) || m.contains("GRAB", true) -> "Transport"
        m.contains("INDO", true) || m.contains("ALFA", true) || m.contains("MART", true) -> "Makan"
        else -> "Lainnya"
    }
}

@Singleton
class MyBcaAlternativeParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bca.mybca"
    override val displayName = "myBCA"
    private val delegate = MyBcaParser()
    override fun parse(text: String) = delegate.parse(text)
}

@Singleton
class BcaParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bca.mobile"
    override val displayName = "m-BCA"
    
    private val incomeRegex = Regex("""(?:menerima|masuk|transfer masuk|dana masuk|kredit|cr|dari|pemasukan).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val expenseRegex = Regex("""(?:transaksi|pembayaran|pembelian|transfer|debit|db|ke|bayar|pengeluaran).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)

    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null

        val isIncome = text.contains("masuk", true) || 
                       text.contains("menerima", true) || 
                       text.contains("pemasukan", true)
        
        if (isIncome) {
            incomeRegex.find(text)?.let { m ->
                val amount = parseAmount(m.groupValues[1]) ?: return@let
                return ParsedTransaction(TxType.INCOME, amount, "Lainnya", extractMerchant(text))
            }
        }

        expenseRegex.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.EXPENSE, amount, "Lainnya", extractMerchant(text))
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        val clean = cleanForMerchant(text)
        return when {
            clean.contains(" dari ", true) -> clean.substringAfter(" dari ").trim().split(" ").take(2).joinToString(" ")
            clean.contains(" ke ", true) -> clean.substringAfter(" ke ").trim().split(" ").take(2).joinToString(" ")
            else -> null
        }
    }
}

@Singleton
class BcaLegacyParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bca"
    override val displayName = "BCA"
    private val delegate = BcaParser()
    override fun parse(text: String) = delegate.parse(text)
}

@Singleton
class BcaDigitalParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bcadigital.android"
    override val displayName = "blu by BCA"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("terima", true) || text.contains("masuk", true) || text.contains("pemasukan", true)
        
        val clean = cleanForMerchant(text)
        val merchant = when {
            clean.contains(" ke ", true) -> clean.substringAfter(" ke ").trim().split(" ").take(2).joinToString(" ")
            clean.contains(" dari ", true) -> clean.substringAfter(" dari ").trim().split(" ").take(2).joinToString(" ")
            else -> null
        }
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", merchant)
    }
}

@Singleton
class BrimoParser @Inject constructor() : NotificationParser {
    override val packageName = "id.co.bri.brimo"
    override val displayName = "BRImo"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("kredit", true) || text.contains("pemasukan", true)
        
        val merchant = Regex("""Merchant:\s*([^.,]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", merchant)
    }
}

@Singleton
class LivinParser @Inject constructor() : NotificationParser {
    override val packageName = "id.bmri.livin"
    override val displayName = "Livin' Mandiri"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("kredit", true) || text.contains("pemasukan", true)
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class DanaParser @Inject constructor() : NotificationParser {
    override val packageName = "id.dana"
    override val displayName = "DANA"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("terima", true) || text.contains("refund", true)
        
        val merchant = Regex("""(?:ke|di|dari)\s+([^.,|]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", merchant)
    }
}

@Singleton
class GopayParser @Inject constructor() : NotificationParser {
    override val packageName = "com.gojek.app"
    override val displayName = "GoPay"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("terima", true) || text.contains("refund", true)
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class OvoParser @Inject constructor() : NotificationParser {
    override val packageName = "com.ovo.app"
    override val displayName = "OVO"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("terima", true) || text.contains("topup", true) || text.contains("top up", true)
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class ShopeeParser @Inject constructor() : NotificationParser {
    override val packageName = "com.shopee.id"
    override val displayName = "ShopeePay"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("masuk", true) || text.contains("terima", true) || text.contains("refund", true)
        
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class BniParser @Inject constructor() : NotificationParser {
    override val packageName = "src.bni.mobile.android"
    override val displayName = "BNI Mobile"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("kredit", true) || text.contains("masuk", true) || text.contains("pemasukan", true)
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class SeabankParser @Inject constructor() : NotificationParser {
    override val packageName = "com.seabank.id"
    override val displayName = "SeaBank"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("terima", true) || text.contains("masuk", true) || text.contains("pemasukan", true)
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}

@Singleton
class JagoParser @Inject constructor() : NotificationParser {
    override val packageName = "com.jago.id"
    override val displayName = "Bank Jago"
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        val match = AMOUNT_REGEX.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        val isIncome = text.contains("terima", true) || text.contains("masuk", true) || text.contains("pemasukan", true)
        return ParsedTransaction(if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, "Lainnya", null)
    }
}
