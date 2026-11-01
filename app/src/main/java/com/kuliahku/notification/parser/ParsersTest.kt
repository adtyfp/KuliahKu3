package com.kuliahku.notification.parser

import com.kuliahku.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParsersTest {

    @Test fun bca_expense() {
        val p = BcaParser()
        val r = p.parse("Transaksi Kartu Kredit Rp 250.000 di TOKOPEDIA berhasil.")
        assertNotNull(r)
        assertEquals(TxType.EXPENSE, r!!.type)
        assertEquals(250_000L, r.amount)
    }

    @Test fun bca_income() {
        val p = BcaParser()
        val r = p.parse("Anda menerima transfer masuk Rp 500.000 dari BUDI.")
        assertNotNull(r)
        assertEquals(TxType.INCOME, r!!.type)
        assertEquals(500_000L, r.amount)
    }

    @Test fun brimo_with_merchant() {
        val p = BrimoParser()
        val r = p.parse("Transaksi debit Rp100.000 berhasil. Merchant: SHOPEE")
        assertNotNull(r)
        assertEquals(100_000L, r!!.amount)
        assertEquals("SHOPEE", r.merchant)
    }

    @Test fun dana_pulsa_kategori() {
        val p = DanaParser()
        val r = p.parse("Pembayaran pulsa Rp50.000 ke TELKOMSEL berhasil.")
        assertNotNull(r)
        assertEquals("Pulsa/Internet", r!!.category)
    }

    @Test fun gopay_gofood_kategori_makan() {
        val p = GopayParser()
        val r = p.parse("Pembayaran GoFood Rp35.000 berhasil.")
        assertNotNull(r)
        assertEquals("Makan", r!!.category)
    }

    @Test fun non_transaction_text_returns_null() {
        val p = BcaParser()
        assertNull(p.parse("Selamat datang di m-BCA"))
    }

    @Test fun dedup_hash_stable_within_5_min() {
        val r = ParsedTransaction(TxType.EXPENSE, 250_000L, "Lainnya", null)
        val h1 = r.dedupHash("com.bca", 1_700_000_000_000L)
        val h2 = r.dedupHash("com.bca", 1_700_000_000_000L + 1000)
        val h3 = r.dedupHash("com.bca", 1_700_000_000_000L + 6 * 60 * 1000)
        assertEquals(h1, h2)
        assert(h1 != h3)
    }
}