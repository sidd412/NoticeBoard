package com.notifiy.noticeboard.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.notifiy.noticeboard.data.model.NoticeBoard
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PDFGenerator {
    
    fun generateBoardInfoPDF(
        context: Context,
        board: NoticeBoard,
        qrBitmap: Bitmap?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            // Set up paint objects
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                isAntiAlias = true
                isFakeBoldText = true
            }
            
            val subtitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isAntiAlias = true
                isFakeBoldText = true
            }
            
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isAntiAlias = true
            }
            
            val smallTextPaint = Paint().apply {
                color = Color.GRAY
                textSize = 12f
                isAntiAlias = true
            }
            
            val instructionPaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isAntiAlias = true
                isFakeBoldText = true
            }
            
            var yPosition = 60f
            
            // Title - Organization Name (centered)
            val titleWidth = titlePaint.measureText(board.organizationName)
            canvas.drawText(board.organizationName, (595f - titleWidth) / 2f, yPosition, titlePaint)
            yPosition += 50f
            
            // Subtitle - Meaningful description (centered)
            val subtitleText = "Digital Notice Board - Scan to Subscribe"
            val subtitleWidth = subtitlePaint.measureText(subtitleText)
            canvas.drawText(subtitleText, (595f - subtitleWidth) / 2f, yPosition, subtitlePaint)
            yPosition += 80f
            
            // Middle Row: Board Details + QR Code (vertically centered)
            val leftColumnStart = 50f
            val leftColumnWidth = 280f
            val rightColumnStart = 350f
            val qrSize = 200f
            
            // Calculate center Y position for the middle section
            val middleSectionHeight = 200f // Approximate height of details + QR
            val middleSectionStartY = yPosition
            
            // Left side - Board Details (vertically centered)
            val detailsStartY = middleSectionStartY + (middleSectionHeight - (5 * 25f)) / 2f // Center the details
            
            canvas.drawText("Board Information:", leftColumnStart, detailsStartY, instructionPaint)
            var detailY = detailsStartY + 30f
            
            val details = listOf(
                "Organization: ${board.organizationName}",
                "Location: ${board.organizationLocation}",
                "Board Code: ${board.organizationCode}",
                "Email: ${board.organizationEmail}",
                "WhatsApp: ${board.organizationWhatsapp}"
            )
            
            details.forEach { detail ->
                canvas.drawText(detail, leftColumnStart + 20f, detailY, textPaint)
                detailY += 25f
            }
            
            // Right side - QR Code (vertically centered)
            qrBitmap?.let { qr ->
                val qrY = middleSectionStartY + (middleSectionHeight - qrSize) / 2f
                canvas.drawBitmap(qr, null, android.graphics.RectF(rightColumnStart, qrY, rightColumnStart + qrSize, qrY + qrSize), null)
                
                // QR Code label (centered below QR)
                val qrLabelText = "Scan to Subscribe"
                val qrLabelWidth = textPaint.measureText(qrLabelText)
                canvas.drawText(qrLabelText, rightColumnStart + (qrSize - qrLabelWidth) / 2f, qrY + qrSize + 25f, textPaint)
            }
            
            // Move to bottom section with sufficient gap
            yPosition = middleSectionStartY + middleSectionHeight + 100f
            
            // Instructions at bottom with more spacing
            canvas.drawText("Instructions:", leftColumnStart, yPosition, instructionPaint)
            yPosition += 40f
            
            val instructions = listOf(
                "1. Print this page and display it at your institute",
                "2. Students can scan the QR code to subscribe instantly",
                "3. Share your board code: ${board.organizationCode}",
                "4. Students can also subscribe using email: ${board.organizationEmail}",
                "5. Keep your institute updated with digital notices!"
            )
            
            instructions.forEach { instruction ->
                canvas.drawText(instruction, leftColumnStart + 20f, yPosition, textPaint)
                yPosition += 30f
            }
            
            yPosition += 60f
            
            // Footer (centered)
            val footerText1 = "Generated by NoticeBoard App"
            val footerText2 = "Connect your institute with digital communication"
            val footer1Width = smallTextPaint.measureText(footerText1)
            val footer2Width = smallTextPaint.measureText(footerText2)
            
            canvas.drawText(footerText1, (595f - footer1Width) / 2f, yPosition, smallTextPaint)
            canvas.drawText(footerText2, (595f - footer2Width) / 2f, yPosition + 20f, smallTextPaint)
            
            pdfDocument.finishPage(page)
            
            // Save PDF using traditional file system for better compatibility
            val fileName = "NoticeBoard_${board.organizationName.replace(" ", "_")}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            println("DEBUG: PDFGenerator - Saving to: ${file.absolutePath}")
            println("DEBUG: PDFGenerator - Downloads dir exists: ${downloadsDir.exists()}")
            println("DEBUG: PDFGenerator - Downloads dir writable: ${downloadsDir.canWrite()}")
            
            // Ensure downloads directory exists
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
                println("DEBUG: PDFGenerator - Created downloads directory")
            }
            
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            fileOutputStream.close()
            
            val filePath = file.absolutePath
            
            pdfDocument.close()
            
            println("DEBUG: PDFGenerator - File created successfully: ${file.exists()}")
            println("DEBUG: PDFGenerator - File size: ${file.length()}")
            println("DEBUG: PDFGenerator - Final file path: $filePath")
            
            onSuccess(filePath)
            
        } catch (e: IOException) {
            onError("Failed to generate PDF: ${e.message}")
        } catch (e: Exception) {
            onError("Error generating PDF: ${e.message}")
        }
    }
}
