package com.notifiy.noticeboard.ui.screens

/**
 * Name: Siddharth
 * Package Name: com.notifiy.noticeboard.ui.screens.PurchaseDetailScreen
 * Date: 10-02-2025
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.notifiy.noticeboard.data.model.Purchase
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.PDFGenerator
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.notifiy.noticeboard.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    navController: NavController,
    purchaseId: String,
    purchase: Purchase
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_animation")
    val headerScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "header_scale"
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Function that performs the actual PDF generation
    fun performDownload(purchase: Purchase) {
        android.util.Log.d("orderHistory", "📄 Starting PDF download for purchase: ${purchase.orderId}")
        
        // Show loading toast
        android.widget.Toast.makeText(context, "Generating PDF...", android.widget.Toast.LENGTH_SHORT).show()
        
        // Generate PDF
        PDFGenerator.generatePurchaseItineraryPDF(
            context = context,
            purchase = purchase,
            onSuccess = { filePath ->
                android.util.Log.d("orderHistory", "✅ PDF generated successfully: $filePath")
                android.widget.Toast.makeText(context, "PDF Downloaded to Downloads!", android.widget.Toast.LENGTH_SHORT).show()
                // Show notification
                showPurchaseDownloadNotification(context, purchase.planName, filePath)
            },
            onError = { error ->
                android.util.Log.e("orderHistory", "❌ PDF generation failed: $error")
                android.widget.Toast.makeText(context, "Failed to generate PDF: $error", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }
    
    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                performDownload(purchase)
            }
        }
    )
    
    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Continue with download regardless of notification permission
            performDownload(purchase)
        }
    )
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        content = { paddingValues ->
            // Create accessible variables and functions within content scope
            val localContext = context
            val localPurchase = purchase
            val localStoragePermissionLauncher = storagePermissionLauncher
            val localNotificationPermissionLauncher = notificationPermissionLauncher
            
            val downloadAction = {
                android.util.Log.d("orderHistory", "📄 Initiating download for purchase: ${localPurchase.orderId}")
                
                // Request storage permission
                if (!checkStoragePermission(localContext)) {
                    localStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    // Request notification permission if needed (Android 13+)
                    if (!checkNotificationPermission(localContext)) {
                        localNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    // Start download since we have permissions
                    performDownload(localPurchase)
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(headerScale),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Purchase Confirmed!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = purchase.planName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "${purchase.subscriptionPeriod.replaceFirstChar { it.uppercase() }} Plan",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Purchase Details Section
                item {
                    DetailSectionCard(
                        title = "Purchase Information",
                        icon = Icons.Default.Receipt,
                        iconColor = MaterialTheme.colorScheme.primary
                    ) {
                        PurchaseDetailRow(
                            icon = Icons.Default.ShoppingBag,
                            title = "Order ID",
                            value = purchase.orderId
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.Star,
                            title = "Plan Name",
                            value = purchase.planName
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.Label,
                            title = "Product ID",
                            value = purchase.planId
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.CalendarToday,
                            title = "Billing Period",
                            value = purchase.subscriptionPeriod.replaceFirstChar { it.uppercase() }
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.Money,
                            title = "Amount Paid",
                            value = "${purchase.currency} ${purchase.price}"
                        )
                        
                        StatusDetailRow(purchase.purchaseState)
                    }
                }
                
                // Transaction Details Section
                item {
                    DetailSectionCard(
                        title = "Transaction Details",
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFF4CAF50)
                    ) {
                        PurchaseDetailRow(
                            icon = Icons.Default.Schedule,
                            title = "Purchase Date",
                            value = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(purchase.purchaseTime))
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.DateRange,
                            title = "Valid Until",
                            value = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(purchase.expiryTime))
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.Refresh,
                            title = "Auto Renewal",
                            value = if (purchase.autoRenewing) "Enabled" else "Disabled"
                        )
                        
                        PurchaseDetailRow(
                            icon = Icons.Default.Category,
                            title = "Package",
                            value = purchase.packageName
                        )
                    }
                }
                
                // Benefits Section
                item {
                    DetailSectionCard(
                        title = "Plan Benefits",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFF9800)
                    ) {
                        Text(
                            text = "Your ${purchase.planName} subscription includes:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        PlanBenefitItem("📱 Create Unlimited Notice Boards")
                        PlanBenefitItem("📄 Unlimited Pages per Board")
                        PlanBenefitItem("🔔 Real-time Notifications")
                        PlanBenefitItem("📊 Advanced Analytics")
                        PlanBenefitItem("🤖 AI-Powered Note Generation")
                        PlanBenefitItem("💰 Monetization Features")
                        PlanBenefitItem("👥 Team Collaboration Tools")
                        PlanBenefitItem("🔐 Enhanced Security")
                    }
                }
                
                // Support Section
                item {
                    SupportCard(
                        navController = navController,
                        onDownloadClick = downloadAction
                    )
                }
                
                // Bottom Spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
fun PurchaseDetailRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun StatusDetailRow(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Status",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Status",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.width(120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        val statusColor = when (status.lowercase()) {
            "purchased" -> Color(0xFF4CAF50)
            "pending" -> Color(0xFFFFC107)
            "cancelled", "refunded" -> Color(0xFFF44336)
            else -> Color.Gray
        }
        
        Surface(
            color = statusColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
        ) {
            Text(
                text = status.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun PlanBenefitItem(benefit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Benefit",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = benefit,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun SupportCard(
    navController: NavController,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Support,
                contentDescription = "Support",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Need Help?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Contact us if you have any questions about your purchase",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        // Navigate to Help and Support screen
                        navController.navigate(Screen.HelpSupport.route)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Help")
                }
                
                OutlinedButton(
                    onClick = { 
                        onDownloadClick()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreenWrapper(
    navController: NavController,
    purchaseId: String
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    var purchase by remember { mutableStateOf<Purchase?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(purchaseId) {
        scope.launch {
            try {
                android.util.Log.d("orderHistory", "🔍 Loading purchase detail for ID: $purchaseId")
                isLoading = true
                errorMessage = null

                // Fetch the specific purchase by ID
                val fetchedPurchase = repository.getPurchaseById(purchaseId)
                
                if (fetchedPurchase != null) {
                    android.util.Log.d("orderHistory", "✅ Purchase found: ${fetchedPurchase.orderId}")
                    purchase = fetchedPurchase
                } else {
                    android.util.Log.w("orderHistory", "❌ Purchase not found for ID: $purchaseId")
                    errorMessage = "Purchase not found"
                }
                
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("orderHistory", "❌ Error loading purchase detail: ${e.message}")
                android.util.Log.e("orderHistory", "❌ Stack trace:", e)
                errorMessage = "Error loading purchase details: ${e.message}"
                isLoading = false
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading purchase details...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error Loading Purchase",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
        
        purchase != null -> {
            PurchaseDetailScreen(
                navController = navController,
                purchaseId = purchaseId,
                purchase = purchase!!
            )
        }
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ - Use scoped storage (no permission needed)
        true
    } else {
        // Android 10 and below - Check WRITE_EXTERNAL_STORAGE permission
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun showPurchaseDownloadNotification(context: Context, planName: String, filePath: String) {
    try {
        android.util.Log.d("orderHistory", "🔔 Showing download notification for: $planName")
        
        // Check if we have permission to post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.w("orderHistory", "🔔 POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
        }
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "purchase_pdf_downloads",
                "Purchase PDF Downloads",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for purchase PDF downloads"
            }
            
            val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intents for both actions
        val file = java.io.File(filePath)
        val pdfUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        // Intent to open PDF
        val openPdfIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Intent to share PDF
        val sharePdfIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Create pending intents for both actions
        val openPdfPendingIntent = android.app.PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openPdfIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val sharePdfPendingIntent = android.app.PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() + 1).toInt(),
            android.content.Intent.createChooser(sharePdfIntent, "Share Purchase Receipt"),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create notification with both action buttons
        val notification = androidx.core.app.NotificationCompat.Builder(context, "purchase_pdf_downloads")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Purchase Receipt Downloaded")
            .setContentText("PDF receipt for $planName is ready")
            .setContentIntent(openPdfPendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
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
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d("orderHistory", "🔔 Download notification shown")
        } else {
            android.util.Log.w("orderHistory", "🔔 Notifications are disabled")
        }
        
    } catch (e: Exception) {
        android.util.Log.e("orderHistory", "🔔 Error showing download notification: ${e.message}")
    }
}
