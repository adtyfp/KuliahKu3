package com.kuliahku.notification.parser

import com.kuliahku.data.model.TxType
import javax.inject.Inject
import javax.inject.Singleton

/** Helper: konversi "1.500.000", "Rp1.500.000,00", atau "IDR 5,000.00" ke Long. */
private val BLACKLIST_KEYWORDS = listOf(
    "saldo", "sisa", "limit", "poin", "point", "informasi", "rekening", 
    "otp", "keamanan", "aktivasi", "biaya admin", "syarat", "ketentuan",
    "promo", "diskon", "cashback", "suku bunga"
)

private fun isNotTransaction(text: String): Boolean {
    // Abaikan jika mengandung kata kunci informasi saldo/limit atau info keamanan/promo
    return BLACKLIST_KEYWORDS.any { text.contains(it, ignoreCase = true) }
}

private fun parseAmount(s: String): Long? {
    var clean = s.replace("Rp", "", true)
        .replace("IDR", "", true)
        .replace(" ", "")
        .trim()

    // Cari desimal di akhir (misal .00 atau ,00)
    val decimalMatch = Regex("""[.,](\d{2})$""").find(clean)
    if (decimalMatch != null) {
        // Hapus bagian desimal
        clean = clean.substring(0, decimalMatch.range.first)
    }

    // Ambil hanya angka yang tersisa
    val digits = clean.filter { it.isDigit() }
    return digits.toLongOrNull()
}

@Singleton
class MyBcaParser @Inject constructor() : NotificationParser {
    override val packageName = "com.bca.mybca.omni.android"
    override val displayName = "myBCA"

    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        android.util.Log.d("MyBcaParser", "Mencoba parsing myBCA: $text")

        val amountRx = Regex("""(?:Rp|IDR)\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
        val match = amountRx.find(text) ?: return null
        val amount = parseAmount(match.groupValues[1]) ?: return null
        
        val isIncome = text.contains("masuk", true) || 
                       text.contains("terima", true) || 
                       text.contains("pemasukan", true) ||
                       text.contains("kredit", true) ||
                       text.contains("income", true)
                       
        val isExpense = text.contains("keluar", true) || 
                        text.contains("pengeluaran", true) || 
                        text.contains("transfer ke", true) ||
                        text.contains("bayar", true) ||
                        text.contains("debit", true) ||
                        text.contains("pembelian", true) ||
                        text.contains("paid", true)

        // Tentukan tipe secara eksplisit
        val type = when {
            isIncome && !isExpense -> TxType.INCOME
            isExpense -> TxType.EXPENSE
            else -> return null // Bukan transaksi yang jelas (bisa jadi saldo)
        }
        val merchant = extractMerchant(text)
        
        return ParsedTransaction(type, amount, if (type == TxType.INCOME) "Lainnya" else categoryFromMerchant(merchant), merchant)
    }

    private fun extractMerchant(text: String): String? {
        val clean = text.replace(Regex("""(?:Rp|IDR)\s*[\d.,]+""", RegexOption.IGNORE_CASE), "")
            .replace("Catatan Finansial", "", true)
            .replace("berhasil", "", true)
            .replace("sebesar", "", true)
            .replace("|", "")
            .trim()
            
        return when {
            clean.contains(" dari ", true) -> clean.substringAfter(" dari ").substringBefore(" di ").substringBefore(" kategori ").trim()
            clean.contains(" ke ", true) -> clean.substringAfter(" ke ").substringBefore(" di ").substringBefore(" kategori ").trim()
            clean.contains(" transfer ", true) -> clean.substringAfter(" transfer ").substringBefore(" di ").trim()
            else -> null
        }
    }

    private fun categoryFromMerchant(m: String?): String = when {
        m == null -> "Lainnya"
        m.contains("GO", true) || m.contains("GRAB", true) -> "Transport"
        m.contains("INDO", true) || m.contains("ALFA", true) || m.contains("MART", true) -> "Makan"
        m.contains("PLN", true) || m.contains("LISTRIK", true) -> "Tagihan"
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
    
    private val incomeRegex = Regex("""(?:menerima|masuk|transfer masuk|dana masuk|kredit|cr|dari).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val expenseRegex = Regex("""(?:transaksi|pembayaran|pembelian|transfer|debit|db|ke|bayar).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)

    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        android.util.Log.d("BcaParser", "Mencoba parsing: $text")

        // Cek apakah ini benar-benar transaksi masuk
        val isIncome = text.contains("masuk", true) || text.contains("menerima", true) || text.contains("dana masuk", true)
        
        if (isIncome) {
            incomeRegex.find(text)?.let { m ->
                val amount = parseAmount(m.groupValues[1]) ?: return@let
                return ParsedTransaction(TxType.INCOME, amount, "Lainnya", extractMerchant(text, isIncome = true))
            }
        }

        expenseRegex.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val merchant = extractMerchant(text, isIncome = false)
            return ParsedTransaction(TxType.EXPENSE, amount, categoryFromMerchant(merchant), merchant)
        }
        return null
    }

    private fun extractMerchant(text: String, isIncome: Boolean): String? {
        val cleanText = text.replace(Regex("""Rp\.?\s*[\d.,]+""", RegexOption.IGNORE_CASE), "")
        return when {
            cleanText.contains(" di ", true) -> cleanText.substringAfter(" di ").trim().split(" ").take(2).joinToString(" ")
            cleanText.contains(" ke ", true) -> cleanText.substringAfter(" ke ").trim().split(" ").take(2).joinToString(" ")
            cleanText.contains(" dari ", true) -> cleanText.substringAfter(" dari ").trim().split(" ").take(2).joinToString(" ")
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
        val rx = Regex("""(?:terima|masuk|transfer|bayar|keluar).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
        val m = rx.find(text) ?: return null
        val amount = parseAmount(m.groupValues[1]) ?: return null
        val isIncome = text.contains("terima", true) || text.contains("masuk", true) || text.contains("diterima", true)
        
        val merchant = when {
            text.contains(" ke ", true) -> text.substringAfter(" ke ").trim().split(" ").take(2).joinToString(" ")
            text.contains(" dari ", true) -> text.substringAfter(" dari ").trim().split(" ").take(2).joinToString(" ")
            else -> null
        }
        
        return ParsedTransaction(
            if (isIncome) TxType.INCOME else TxType.EXPENSE,
            amount, "Lainnya", merchant
        )
    }
}

@Singleton
class BrimoParser @Inject constructor() : NotificationParser {
    override val packageName = "id.co.bri.brimo"
    override val displayName = "BRImo"
    private val rx = Regex("""(?:transaksi|debit|kredit|transfer).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:menerima|masuk|kredit).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        rx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val merchant = Regex("""Merchant:\s*([^.,]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
            return ParsedTransaction(TxType.EXPENSE, amount, "Lainnya", merchant)
        }
        return null
    }
}

@Singleton
class LivinParser @Inject constructor() : NotificationParser {
    override val packageName = "id.bmri.livin"
    override val displayName = "Livin' Mandiri"
    private val rx = Regex("""(?:transaksi|pembayaran|transfer|debit).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:kredit|menerima|masuk).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        rx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.EXPENSE, amount, "Lainnya", null)
        }
        return null
    }
}

@Singleton
class DanaParser @Inject constructor() : NotificationParser {
    override val packageName = "id.dana"
    override val displayName = "DANA"
    private val outRx = Regex("""(?:pembayaran|bayar|kirim|transfer|topup|top up|pulsa).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:menerima|masuk|diterima|refund|dana kaget).*?(?:Rp|IDR)?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        outRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val merchant = Regex("""(?:ke|di)\s+([^.,|]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
            val cat = when {
                text.contains("pulsa", true) || text.contains("internet", true) -> "Pulsa/Internet"
                text.contains("game", true) -> "Hiburan"
                else -> "Lainnya"
            }
            return ParsedTransaction(TxType.EXPENSE, amount, cat, merchant)
        }
        return null
    }
}

@Singleton
class GopayParser @Inject constructor() : NotificationParser {
    override val packageName = "com.gojek.app"
    override val displayName = "GoPay"
    private val outRx = Regex("""(?:pembayaran|bayar|kirim|transfer|topup|top up|pulsa).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:menerima|masuk|diterima|refund).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        outRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val cat = when {
                text.contains("gojek", true) || text.contains("goride", true) || text.contains("gocar", true) -> "Transport"
                text.contains("gofood", true) -> "Makan"
                else -> "Lainnya"
            }
            return ParsedTransaction(TxType.EXPENSE, amount, cat, null)
        }
        return null
    }
}

@Singleton
class OvoParser @Inject constructor() : NotificationParser {
    override val packageName = "com.ovo.app"
    override val displayName = "OVO"
    private val rx = Regex("""(?:pembayaran|transaksi|bayar|kirim).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:menerima|masuk|diterima|top up|topup).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        rx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.EXPENSE, amount, "Lainnya", null)
        }
        return null
    }
}

@Singleton
class ShopeeParser @Inject constructor() : NotificationParser {
    override val packageName = "com.shopee.id"
    override val displayName = "ShopeePay"
    private val rx = Regex("""(?:pembayaran|bayar|transaksi).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inRx = Regex("""(?:menerima|masuk|refund|pengembalian).*?Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    override fun parse(text: String): ParsedTransaction? {
        if (isNotTransaction(text)) return null
        inRx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.INCOME, amount, "Lainnya", null)
        }
        rx.find(text)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            return ParsedTransaction(TxType.EXPENSE, amount, "Lainnya", null)
        }
        return null
    }
}
