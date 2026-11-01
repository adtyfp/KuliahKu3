package com.kuliahku.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

object DocumentExtractor {

    /**
     * Ekstraksi teks dari PDF/DOCX/PPTX.
     * Batasan: Apache POI di Android hanya mendukung subset fitur.
     * Untuk file > 10 MB atau rumit, gunakan server-side extraction.
     */
    fun extract(context: Context, uri: Uri, mime: String): Result<String> = runCatching {
        val input: InputStream = context.contentResolver.openInputStream(uri)
            ?: error("Tidak bisa membuka file.")

        when {
            mime.contains("pdf") -> extractPdf(context, input)
            mime.contains("word") || mime.contains("officedocument.wordprocessing") ->
                extractDocx(input)
            mime.contains("presentation") || mime.contains("officedocument.presentation") ->
                extractPptx(input)
            else -> error("Tipe file tidak didukung untuk ekstraksi.")
        }
    }

    private fun extractPdf(context: Context, input: InputStream): String {
        PDFBoxResourceLoader.init(context)
        return PDDocument.load(input).use { doc ->
            PDFTextStripper().getText(doc)
        }
    }

    private fun extractDocx(input: InputStream): String {
        return XWPFDocument(input).use { doc ->
            XWPFWordExtractor(doc).text
        }
    }

    private fun extractPptx(input: InputStream): String {
        return XMLSlideShow(input).use { ppt ->
            ppt.slides.joinToString("\n\n") { slide ->
                slide.shapes.filterIsInstance<org.apache.poi.xslf.usermodel.XSLFTextShape>()
                    .joinToString("\n") { it.text }
            }
        }
    }
}