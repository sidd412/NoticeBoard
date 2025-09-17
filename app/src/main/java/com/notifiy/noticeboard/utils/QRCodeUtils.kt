package com.notifiy.noticeboard.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.notifiy.noticeboard.data.model.NoticeBoard

object QRCodeUtils {
    
    data class QRBoardData(
        val boardId: String,
        val organizationName: String,
        val organizationCode: String,
        val organizationEmail: String,
        val organizationWhatsapp: String,
        val organizationLocation: String
    )
    
    fun generateQRCodeBitmap(board: NoticeBoard, size: Int = 300): Bitmap? {
        return try {
            // Try simple format first (more reliable for scanning)
            val qrText = "NOTICEBOARD:${board.id}:${board.organizationName}:${board.organizationCode}:${board.organizationEmail}:${board.organizationWhatsapp}:${board.organizationLocation}"
            
            println("DEBUG: Generating QR Code for board: ${board.organizationName}")
            println("DEBUG: QR Code data: $qrText")
            
            val writer = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0  // Remove margin for tight fit
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
            
            val bitMatrix = writer.encode(qrText, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            println("DEBUG: QR Code bitmap generated successfully - Size: ${width}x${height}")
            bitmap
        } catch (e: Exception) {
            println("DEBUG: QR Code generation failed: ${e.message}")
            e.printStackTrace()
            
            // Fallback to JSON format if simple format fails
            try {
                val qrData = QRBoardData(
                    boardId = board.id,
                    organizationName = board.organizationName,
                    organizationCode = board.organizationCode,
                    organizationEmail = board.organizationEmail,
                    organizationWhatsapp = board.organizationWhatsapp,
                    organizationLocation = board.organizationLocation
                )
                
                val gson = Gson()
                val jsonQrText = gson.toJson(qrData)
                
                val writer = QRCodeWriter()
                val hints = hashMapOf<EncodeHintType, Any>()
                hints[EncodeHintType.MARGIN] = 0
                hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
                
                val bitMatrix = writer.encode(jsonQrText, BarcodeFormat.QR_CODE, size, size, hints)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                
                println("DEBUG: Fallback JSON QR Code generated successfully")
                bitmap
            } catch (fallbackException: Exception) {
                println("DEBUG: Fallback QR generation also failed: ${fallbackException.message}")
                null
            }
        }
    }
    
    fun parseQRCodeData(qrText: String): QRBoardData? {
        return try {
            println("DEBUG: Attempting to parse QR data: $qrText")
            
            // Try JSON parsing first
            if (qrText.startsWith("{") && qrText.endsWith("}")) {
                val gson = Gson()
                val parsedData = gson.fromJson(qrText, QRBoardData::class.java)
                println("DEBUG: JSON QR parsing successful - Board: ${parsedData?.organizationName}, Code: ${parsedData?.organizationCode}")
                return parsedData
            }
            
            // If JSON parsing fails, try simple format parsing
            // Expected format: "NOTICEBOARD:boardId:orgName:orgCode:email:whatsapp:location"
            if (qrText.startsWith("NOTICEBOARD:")) {
                val parts = qrText.split(":")
                if (parts.size >= 7) {
                    val parsedData = QRBoardData(
                        boardId = parts[1],
                        organizationName = parts[2],
                        organizationCode = parts[3],
                        organizationEmail = parts[4],
                        organizationWhatsapp = parts[5],
                        organizationLocation = parts[6]
                    )
                    println("DEBUG: Simple QR parsing successful - Board: ${parsedData.organizationName}, Code: ${parsedData.organizationCode}")
                    return parsedData
                }
            }
            
            println("DEBUG: QR data format not recognized")
            null
        } catch (e: Exception) {
            println("DEBUG: QR parsing failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }
} 