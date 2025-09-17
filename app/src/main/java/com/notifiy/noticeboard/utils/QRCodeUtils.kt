package com.notifiy.noticeboard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.notifiy.noticeboard.data.model.NoticeBoard
import java.io.InputStream

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
    
    fun decodeQRCodeFromImage(context: Context, imageUri: Uri): String? {
        return try {
            println("DEBUG: Attempting to decode QR from image URI: $imageUri")
            
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) {
                println("DEBUG: Failed to decode bitmap from URI")
                return null
            }
            
            println("DEBUG: Bitmap decoded successfully - Size: ${bitmap.width}x${bitmap.height}")
            
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            val reader = MultiFormatReader()
            val hints = hashMapOf<DecodeHintType, Any>()
            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
            hints[DecodeHintType.TRY_HARDER] = true
            
            val result = reader.decode(binaryBitmap, hints)
            val qrText = result.text
            
            println("DEBUG: QR Code decoded successfully from image: $qrText")
            qrText
        } catch (e: Exception) {
            println("DEBUG: Failed to decode QR from image: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    fun parseQRCodeData(qrText: String): QRCodeUtils.QRBoardData? {
        return try {
            println("DEBUG: Attempting to parse QR data: $qrText")
            
            // Try JSON parsing first
            if (qrText.startsWith("{") && qrText.endsWith("}")) {
                val gson = Gson()
                val parsedData = gson.fromJson(qrText, QRCodeUtils.QRBoardData::class.java)
                println("DEBUG: JSON QR parsing successful - Board: ${parsedData?.organizationName}, Code: ${parsedData?.organizationCode}")
                return parsedData
            }
            
            // If JSON parsing fails, try simple format parsing
            // Expected format: "NOTICEBOARD:boardId:orgName:orgCode:email:whatsapp:location"
            if (qrText.startsWith("NOTICEBOARD:")) {
                val parts = qrText.split(":")
                if (parts.size >= 7) {
                    val parsedData = QRCodeUtils.QRBoardData(
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