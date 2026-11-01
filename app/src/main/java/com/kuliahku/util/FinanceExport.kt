package com.kuliahku.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kuliahku.data.model.TxType
import com.kuliahku.data.model.Transaction
import java.io.File
import java.io.FileOutputStream

object FinanceExport {

    /** CSV + share URI. */
    fun buildCsv(context: Context, transactions: List<Transaction>): Triple<android.net.Uri, String, String> {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "keuangan_${System.currentTimeMillis()}.csv")
        FileOutputStream(file).bufferedWriter().use { w ->
            w.write("Tanggal,Tipe,Kategori,Jumlah,Deskripsi,Sumber\n")
            transactions.sortedBy { it.date?.toDate()?.time ?: 0L }.forEach { t ->
                val date = t.date?.toDate()?.let { DateFormatters.formatDateTime(it) } ?: ""
                val tipe = if (t.typeEnum == TxType.INCOME) "Pemasukan" else "Pengeluaran"
                val desc = (t.description ?: "").replace(",", ";")
                w.write("$date,$tipe,${t.category},${t.amount},$desc,${t.source}\n")
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Triple(uri, "text/csv", "Ekspor Keuangan CSV")
    }

    /** PDF sederhana. */
    fun buildPdf(context: Context, transactions: List<Transaction>): Triple<android.net.Uri, String, String> {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "keuangan_${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var canvas = page.canvas
        val paint = android.graphics.Paint().apply { textSize = 12f }
        var y = 40f
        canvas.drawText("Laporan Keuangan", 40f, y, paint.apply { textSize = 18f; isFakeBoldText = true })
        y += 30f
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Tanggal", 40f, y, paint)
        canvas.drawText("Tipe", 160f, y, paint)
        canvas.drawText("Kategori", 230f, y, paint)
        canvas.drawText("Jumlah", 330f, y, paint)
        canvas.drawText("Sumber", 440f, y, paint)
        paint.isFakeBoldText = false
        y += 16f

        transactions.sortedBy { it.date?.toDate()?.time ?: 0L }.forEach { t ->
            if (y > 800f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                canvas = page.canvas
                y = 40f
            }
            val date = t.date?.toDate()?.let { DateFormatters.formatDateTime(it) } ?: ""
            canvas.drawText(date, 40f, y, paint)
            canvas.drawText(if (t.typeEnum == TxType.INCOME) "Masuk" else "Keluar", 160f, y, paint)
            canvas.drawText(t.category, 230f, y, paint)
            canvas.drawText(CurrencyFormatter.format(t.amount), 330f, y, paint)
            canvas.drawText(t.source, 440f, y, paint)
            y += 14f
        }
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Triple(uri, "application/pdf", "Ekspor Keuangan PDF")
    }
}