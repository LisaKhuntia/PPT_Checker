package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import com.example.data.DocumentEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    private const val TAG = "PdfExporter"
    
    // Page sizes in Points (1/72 inch). A4: 595 x 842.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 50f
    private const val MARGIN_RIGHT = 50f
    private const val MARGIN_TOP = 60f
    private const val MARGIN_BOTTOM = 60f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    fun exportDocumentToPdf(context: Context, document: DocumentEntity, outputStream: OutputStream): Boolean {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        
        try {
            // Paint configurations
            val titlePaint = Paint().apply {
                color = Color.rgb(103, 80, 164) // Sleek brand primary color
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val subtitlePaint = Paint().apply {
                color = Color.rgb(73, 69, 79)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            
            val headingPaint = Paint().apply {
                color = Color.rgb(103, 80, 164)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val boldBodyPaint = Paint().apply {
                color = Color.rgb(28, 27, 31)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(28, 27, 31)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val metaLabelPaint = Paint().apply {
                color = Color.rgb(103, 80, 164)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val badgePaint = Paint().apply {
                color = Color.rgb(234, 221, 255) // light lavender bg
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val badgeTextPaint = Paint().apply {
                color = Color.rgb(33, 0, 93) // onSecondaryContainer
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val dividerPaint = Paint().apply {
                color = Color.rgb(202, 196, 208) // light outline
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }

            // Create first page
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = MARGIN_TOP

            // Draw Header
            canvas.drawText("DeckDoc AI — Document Report", MARGIN_LEFT, currentY, titlePaint)
            currentY += 25f
            
            val formattedDate = SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault()).format(Date(document.timestamp))
            canvas.drawText("Generated on $formattedDate", MARGIN_LEFT, currentY, subtitlePaint)
            currentY += 15f
            
            canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
            currentY += 25f

            // Document Meta Table/Badges
            canvas.drawText("File Name:", MARGIN_LEFT, currentY, metaLabelPaint)
            currentY = drawWrappedText(canvas, document.title, MARGIN_LEFT + 70f, currentY, CONTENT_WIDTH - 70f, bodyPaint) {
                // page overflow logic during drawing
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                canvas
            }
            currentY += 10f

            canvas.drawText("Slide Count:", MARGIN_LEFT, currentY, metaLabelPaint)
            canvas.drawText("${document.slideCount} slides", MARGIN_LEFT + 75f, currentY, bodyPaint)
            canvas.drawText("Language:", MARGIN_LEFT + 180f, currentY, metaLabelPaint)
            canvas.drawText(document.language, MARGIN_LEFT + 250f, currentY, bodyPaint)
            currentY += 20f

            canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
            currentY += 25f

            // SECTION 1: AI PLAGIARISM / INTEGRITY REPORT
            if (document.aiPlagiarismScore != null) {
                canvas.drawText("AI Integrity & Originality Report", MARGIN_LEFT, currentY, headingPaint)
                currentY += 18f
                
                val score = document.aiPlagiarismScore
                val scoreText = "AI Likelihood Score: $score%"
                canvas.drawRect(MARGIN_LEFT, currentY - 12f, MARGIN_LEFT + 150f, currentY + 6f, badgePaint)
                canvas.drawText(scoreText, MARGIN_LEFT + 8f, currentY, badgeTextPaint)
                currentY += 18f
                
                if (document.aiAnalysis != null) {
                    canvas.drawText("Integrity Analysis:", MARGIN_LEFT, currentY, boldBodyPaint)
                    currentY += 14f
                    currentY = drawWrappedText(canvas, document.aiAnalysis, MARGIN_LEFT, currentY, CONTENT_WIDTH, bodyPaint) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas
                    }
                    currentY += 15f
                }
                
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
                currentY += 25f
            }

            // SECTION 2: EXECUTIVE SUMMARY
            if (!document.summary.isNullOrBlank()) {
                canvas.drawText("Executive Summary", MARGIN_LEFT, currentY, headingPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, document.summary, MARGIN_LEFT, currentY, CONTENT_WIDTH, bodyPaint) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas
                }
                currentY += 15f
                
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
                currentY += 25f
            }

            // SECTION 3: GRAMMAR ANALYSIS
            if (!document.grammarAnalysis.isNullOrBlank()) {
                canvas.drawText("Grammar & Clarity Corrections", MARGIN_LEFT, currentY, headingPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, document.grammarAnalysis, MARGIN_LEFT, currentY, CONTENT_WIDTH, bodyPaint) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas
                }
                currentY += 15f
                
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
                currentY += 25f
            }

            // SECTION 4: SPELLING ANALYSIS
            if (!document.spellingAnalysis.isNullOrBlank()) {
                canvas.drawText("Spelling Verification", MARGIN_LEFT, currentY, headingPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, document.spellingAnalysis, MARGIN_LEFT, currentY, CONTENT_WIDTH, bodyPaint) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas
                }
                currentY += 15f
                
                canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, dividerPaint)
                currentY += 25f
            }

            // SECTION 5: EXTRACTED CONTENT (ORIGINAL TEXT)
            canvas.drawText("Extracted Slide Content", MARGIN_LEFT, currentY, headingPaint)
            currentY += 18f
            currentY = drawWrappedText(canvas, document.originalText, MARGIN_LEFT, currentY, CONTENT_WIDTH, bodyPaint) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                canvas
            }

            // Finish the final page
            pdfDocument.finishPage(page)

            // Save PDF
            pdfDocument.writeTo(outputStream)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write PDF", e)
            return false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Helper to draw text word by word, wrapping inside bounds, and advancing to a new page
     * if the drawn content exceeds bottom boundary.
     */
    private fun drawWrappedText(
        canvas: Canvas, 
        text: String, 
        startX: Float, 
        startY: Float, 
        width: Float, 
        paint: Paint,
        onPageOverflow: () -> Canvas
    ): Float {
        var activeCanvas = canvas
        var currentX = startX
        var currentY = startY
        val fontMetrics = paint.fontMetrics
        val lineHeight = fontMetrics.bottom - fontMetrics.top + 4f
        
        // Split input into lines by explicit line breaks to preserve user paragraph structure
        val paragraphs = text.split("\n")
        
        for (paragraph in paragraphs) {
            val words = paragraph.split(" ")
            var lineBuilder = StringBuilder()
            
            for (word in words) {
                val testLine = if (lineBuilder.isEmpty()) word else "${lineBuilder} $word"
                val textWidth = paint.measureText(testLine)
                
                if (textWidth > width) {
                    // Draw current line
                    if (currentY + lineHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                        activeCanvas = onPageOverflow()
                        currentY = MARGIN_TOP
                    }
                    activeCanvas.drawText(lineBuilder.toString(), startX, currentY, paint)
                    currentY += lineHeight
                    
                    lineBuilder = StringBuilder(word)
                } else {
                    lineBuilder.append(if (lineBuilder.isEmpty()) "" else " ").append(word)
                }
            }
            
            // Draw remaining line for this paragraph
            if (lineBuilder.isNotEmpty()) {
                if (currentY + lineHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    activeCanvas = onPageOverflow()
                    currentY = MARGIN_TOP
                }
                activeCanvas.drawText(lineBuilder.toString(), startX, currentY, paint)
                currentY += lineHeight
            }
            
            // Extra line break for paragraph spacing
            currentY += 4f
        }
        
        return currentY
    }
}
