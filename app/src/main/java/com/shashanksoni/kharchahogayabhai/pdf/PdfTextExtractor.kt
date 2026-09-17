package com.shashanksoni.kharchahogayabhai.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

/**
 * Extracts plain text from a text-based PDF.
 *
 * Kept separate from transaction parsing so OCR can later replace this step
 * without touching the statement parsers.
 */
class PdfTextExtractor(context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extractText(inputStream: InputStream): String {
        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }
            return stripper.getText(document)
        }
    }
}
