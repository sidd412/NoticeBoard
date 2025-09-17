package com.notifiy.noticeboard.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.notifiy.noticeboard.utils.QRCodeUtils

@Composable
fun QRScannerComponent(
    onQRCodeScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onError("Camera permission is required to scan QR codes")
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val qrText: String? = QRCodeUtils.decodeQRCodeFromImage(context, uri)
                if (!qrText.isNullOrEmpty()) {
                    onQRCodeScanned(qrText)
                } else {
                    onError("No QR code found in the selected image")
                }
            } catch (e: Exception) {
                onError("Error reading image: ${e.message}")
            }
        } else {
            onError("No image selected")
        }
    }
    
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            onQRCodeScanned(result.contents)
        } else {
            onError("QR code scanning was cancelled")
        }
    }
    
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "QR Code Scanner",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose how to scan the QR code",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            // Camera Scanner Button
            if (hasCameraPermission) {
                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setPrompt("Scan a QR code for board subscription")
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setCameraId(0)
                            setBeepEnabled(true)
                            setBarcodeImageEnabled(false)
                            setOrientationLocked(true) // Lock to portrait
                            setTimeout(30000) // 30 second timeout
                            
                            // Use custom portrait activity
                            captureActivity = PortraitCaptureActivity::class.java
                        }
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Camera Scanner")
                }
            } else {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Camera Permission")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Gallery Selection Button
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select from Gallery")
            }
        }
    }
} 