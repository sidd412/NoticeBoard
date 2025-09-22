package com.notifiy.noticeboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import java.io.File
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.navigation.BottomNavScreen
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.YourBoardsViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.QRCodeUtils
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.getErrorMessage
import com.notifiy.noticeboard.utils.PDFGenerator
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Top-level function for showing download notification
private fun showDownloadNotification(context: android.content.Context, boardName: String, filePath: String) {
    try {
        // Check if we have permission to post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                println("DEBUG: POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
        }
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pdf_downloads",
                "PDF Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for PDF downloads"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intents for both actions
        val file = File(filePath)
        val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        // Intent to open PDF
        val openPdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Intent to share PDF
        val sharePdfIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Create pending intents for both actions
        val openPdfPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openPdfIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val sharePdfPendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() + 1).toInt(),
            Intent.createChooser(sharePdfIntent, "Share PDF"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create notification with both action buttons
        val notification = NotificationCompat.Builder(context, "pdf_downloads")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PDF Downloaded")
            .setContentText("Notice board PDF for $boardName is ready")
            .setContentIntent(openPdfPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open PDF",
                openPdfPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_share,
                "Share PDF",
                sharePdfPendingIntent
            )
            .build()
        
        // Show notification with permission check
        val notificationManager = NotificationManagerCompat.from(context)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            println("DEBUG: Download notification shown for: $boardName")
        } else {
            println("DEBUG: Notifications are disabled by user")
        }
        
    } catch (e: SecurityException) {
        println("DEBUG: SecurityException when showing notification: ${e.message}")
    } catch (e: Exception) {
        println("DEBUG: Failed to show notification: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourBoardsScreen(
    navController: NavController, mainNavController: NavController, authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    val context = LocalContext.current
    val yourBoardsViewModel: YourBoardsViewModel = cachedViewModel(YourBoardsViewModel::class.java)
    val userBoardsState by yourBoardsViewModel.userBoards.collectAsState()
    val errorMessage by yourBoardsViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Function to show dialog suggesting PDF viewer installation
    fun showPDFViewerInstallDialog() {
        // Show a toast with installation instructions
        Toast.makeText(
            context, 
            "No PDF viewer found! Please install Adobe Reader, Google PDF Viewer, or WPS Office from Play Store.", 
            Toast.LENGTH_LONG
        ).show()
        
        // Also try to open Play Store
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://search?q=pdf+reader")
                setPackage("com.android.vending")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            try {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://play.google.com/store/search?q=pdf+reader")
                }
                context.startActivity(webIntent)
            } catch (webException: Exception) {
                println("DEBUG: Failed to open Play Store: ${webException.message}")
            }
        }
    }
    
    // Function to open PDF file using share intent (most reliable approach)
    fun openPDFFile(filePath: String) {
        try {
            println("DEBUG: openPDFFile called with path: $filePath")
            
            val file = File(filePath)
            println("DEBUG: File path: ${file.absolutePath}")
            println("DEBUG: File exists: ${file.exists()}")
            println("DEBUG: File size: ${file.length()}")
            println("DEBUG: File readable: ${file.canRead()}")
            
            if (!file.exists()) {
                Toast.makeText(context, "PDF file not found at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                return
            }
            
            if (!file.canRead()) {
                Toast.makeText(context, "Cannot read PDF file. Check permissions.", Toast.LENGTH_LONG).show()
                return
            }
            
            // Use share intent - this is the most reliable way to open files
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Check if any apps can handle this
            val packageManager = context.packageManager
            val resolveInfo = packageManager.queryIntentActivities(shareIntent, 0)
            println("DEBUG: Share intent - Apps found: ${resolveInfo.size}")
            
            if (resolveInfo.isNotEmpty()) {
                // Filter for PDF viewers and document apps
                val pdfApps = resolveInfo.filter { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName.lowercase()
                    packageName.contains("pdf") || 
                    packageName.contains("adobe") || 
                    packageName.contains("foxit") ||
                    packageName.contains("wps") ||
                    packageName.contains("office") ||
                    packageName.contains("drive") ||
                    packageName.contains("docs") ||
                    packageName.contains("viewer") ||
                    packageName.contains("reader")
                }
                
                if (pdfApps.isNotEmpty()) {
                    // Try to open with a specific PDF app
                    val specificIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                        setPackage(pdfApps.first().activityInfo.packageName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    try {
                        context.startActivity(specificIntent)
                        println("DEBUG: Successfully opened with PDF app: ${pdfApps.first().activityInfo.packageName}")
                        return
                    } catch (e: Exception) {
                        println("DEBUG: Failed to open with specific app: ${e.message}")
                    }
                }
                
                // Fallback: Show share dialog
                val chooserIntent = Intent.createChooser(shareIntent, "Open PDF with")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
                println("DEBUG: Showing share dialog")
                return
            }
            
            Toast.makeText(context, "No app found to open PDF. Please install a PDF viewer.", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            println("DEBUG: Error opening PDF: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Handle snackbar action result
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            println("DEBUG: Showing snackbar")
            val result = snackbarHostState.showSnackbar(
                message = "PDF downloaded successfully!",
                actionLabel = "View",
                duration = SnackbarDuration.Long
            )
            println("DEBUG: Snackbar result: $result")
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                println("DEBUG: Action performed, sharing PDF")
                downloadedFilePath?.let { filePath: String ->
                    println("DEBUG: File path to share: $filePath")
                    openPDFFile(filePath)
                }
            } else {
                println("DEBUG: Action not performed, result: $result")
            }
            showSnackbar = false
        }
    }
    
    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted! You can now download PDFs.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Cannot download PDFs.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Permission launcher for notifications (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. You won't see download notifications.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Function to check and request permissions
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we don't need WRITE_EXTERNAL_STORAGE permission
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Function to check notification permission
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications are allowed by default on older Android versions
        }
    }
    
    // Function to download PDF
    fun downloadBoardPDF(board: NoticeBoard) {
        if (!checkStoragePermission()) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        
        // Request notification permission if needed (Android 13+)
        if (!checkNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Continue with download even if notification permission is denied
        }
        
        Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
        
        // Generate QR code bitmap
        val qrBitmap = QRCodeUtils.generateQRCodeBitmap(board, 300) // Larger size for PDF
        
        PDFGenerator.generateBoardInfoPDF(
            context = context,
            board = board,
            qrBitmap = qrBitmap,
            onSuccess = { filePath ->
                downloadedFilePath = filePath
                
                // Show toast notification
                Toast.makeText(context, "Successfully downloaded!", Toast.LENGTH_SHORT).show()
                
                // Show top notification (will check permission internally)
                showDownloadNotification(context, board.organizationName, filePath)
                
                // Show snackbar
                showSnackbar = true
            },
            onError = { error ->
                Toast.makeText(context, "Failed to generate PDF: $error", Toast.LENGTH_LONG).show()
            }
        )
    }
    

    // Handle mobile back button
    BackHandler {
        println("DEBUG: YourBoardsScreen - Mobile back button pressed")
        // Navigate to Home tab instead of popBackStack
        navController.navigate(BottomNavScreen.Home.route)
    }

    // Load user boards when screen is displayed
    LaunchedEffect(currentUser) {
        try {
            currentUser?.let { user ->
                yourBoardsViewModel.loadUserBoards(user.id)
            }
        } catch (e: Exception) {
            // Don't crash the app, just handle the error silently
        }
    }

    // Cleanup when navigating away
    DisposableEffect(Unit) {
        onDispose {
            try {
                yourBoardsViewModel.clearError()
            } catch (e: Exception) {
                // Handle cleanup error silently
            }
        }
    }

    // Show error messages
    ShowErrorSnackbar(
        error = errorMessage?.let { getErrorMessage(Exception(it)) },
        snackbarHostState = snackbarHostState,
        onErrorShown = { yourBoardsViewModel.clearError() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )
        {
            // Back button and header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            println("DEBUG: YourBoardsScreen - Back icon clicked")
                            // Navigate to Home tab instead of popBackStack
                            navController.navigate(BottomNavScreen.Home.route)
                        }) {
                        Icon(
                            Icons.Default.ArrowBack, contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Your Notice Boards",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Refresh button
                    IconButton(
                        onClick = {
                            println("DEBUG: YourBoardsScreen - Refresh button clicked")
                            currentUser?.let { user ->
                                println("DEBUG: YourBoardsScreen - Refreshing boards for user: ${user.id}")
                                yourBoardsViewModel.loadUserBoards(user.id)
                            } ?: run {
                                println("DEBUG: YourBoardsScreen - No current user for refresh")
                            }
                        }) {
                        Icon(
                            Icons.Default.Refresh, contentDescription = "Refresh"
                        )
                    }
                }
            }

            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Manage Your Boards",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create and update your institute's notice boards",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // My Existing Boards Section
            item {
                Text(
                    text = "My Running Boards",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Boards List
            if (userBoardsState.isLoading) {
                item {
                    println("DEBUG: YourBoardsScreen - Showing loading indicator")
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (userBoardsState.data?.isEmpty() == true) {
                item {
                    println("DEBUG: YourBoardsScreen - No boards found, showing empty state")
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No boards created yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first notice board to get started",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                println("DEBUG: YourBoardsScreen - Showing ${userBoardsState.data?.size ?: 0} boards")
                items(userBoardsState.data ?: emptyList()) { board ->
                    println("DEBUG: YourBoardsScreen - Rendering board: ${board.organizationName}")
                    YourBoardCard(
                        board = board, 
                        onUpdateClick = {
                            println("DEBUG: YourBoardsScreen - Update clicked for board: ${board.id}")
                            mainNavController.navigate(Screen.BoardDetails.createRoute(board.id))
                        },
                        onDeleteClick = { boardToDelete ->
                            println("DEBUG: YourBoardsScreen - Delete clicked for board: ${boardToDelete.id}")
                            currentUser?.let { user ->
                                yourBoardsViewModel.deleteNoticeBoard(boardToDelete.id, user.id) { success ->
                                    if (success) {
                                        println("DEBUG: YourBoardsScreen - Board deleted successfully")
                                        Toast.makeText(context, "Notice board \"${boardToDelete.organizationName}\" deleted successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        println("DEBUG: YourBoardsScreen - Failed to delete board")
                                        Toast.makeText(context, "Failed to delete notice board. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onDownloadClick = { boardToDownload ->
                            println("DEBUG: YourBoardsScreen - Download clicked for board: ${boardToDownload.id}")
                            downloadBoardPDF(boardToDownload)
                        }
                    )
                }
            }
        }

        // Fixed Create New Board Button
        Button(
            onClick = { mainNavController.navigate(Screen.CreateBoard.route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create New Board",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create New Board", fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun YourBoardCard(
    board: NoticeBoard, 
    onUpdateClick: () -> Unit,
    onDeleteClick: (NoticeBoard) -> Unit,
    onDownloadClick: (NoticeBoard) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onUpdateClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with board name and action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Board Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = board.organizationName.take(2).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = board.organizationName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = board.organizationLocation,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Action buttons
                    Row {
                        IconButton(
                            onClick = onUpdateClick
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Board",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Board",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Board Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardDetailRow(
                        label = "Code", value = board.organizationCode
                    )
                    BoardDetailRow(
                        label = "WhatsApp", value = board.organizationWhatsapp
                    )
                    BoardDetailRow(
                        label = "Email", value = board.organizationEmail
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status and Download Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (board.isActive) Color(0xFF4CAF50)
                                else Color(0xFFF44336)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (board.isActive) "Active" else "Inactive",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    }
                    
                    // Download Button
                    IconButton(
                        onClick = { onDownloadClick(board) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Board Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp)
                    .size(90.dp)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ){
                val qrBitmap = remember(board.id) { 
                    QRCodeUtils.generateQRCodeBitmap(board, 90) 
                }
                
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code for ${board.organizationName}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp).clip(RoundedCornerShape(2.dp)) // Small padding to prevent touching borders
                    )
                } else {
                    Text(
                        text = "QR",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Notice Board") },
                text = { 
                    Text("Are you sure you want to delete \"${board.organizationName}\"? This action cannot be undone and will permanently remove the board and all its data. All subscribers will lose access to this board and its notices.")
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDeleteDialog = false
                            onDeleteClick(board)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BoardDetailRow(
    label: String, value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
