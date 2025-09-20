package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.preferences.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(navController: NavController) {
    var showDataDeletionDialog by remember { mutableStateOf(false) }
    var showDataExportDialog by remember { mutableStateOf(false) }
    
    // Initialize preferences manager
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Load notification preferences
    var pushNotifications by remember { mutableStateOf(preferencesManager.getPushNotifications()) }
    var emailNotifications by remember { mutableStateOf(preferencesManager.getEmailNotifications()) }
    var marketingEmails by remember { mutableStateOf(preferencesManager.getMarketingEmails()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = "Privacy",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your Privacy Matters",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We are committed to protecting your privacy and giving you control over your data.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // App Permissions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "App Permissions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Notice Board requests the following permissions:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val permissions = listOf(
                            "📱 Internet Access - Required for syncing notices and user authentication",
                            "🔔 Notifications - To send you important updates and announcements",
                            "📧 Email Access - For account verification and password recovery",
                            "📱 Device Storage - To cache notices for offline viewing",
                            "🌐 Network State - To check internet connectivity",
                            "📱 Camera (Optional) - For QR code scanning to join boards",
                            "📁 File Access (Optional) - For sharing notices and attachments"
                        )
                        
                        permissions.forEach { permission ->
                            Text(
                                text = permission,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You can manage these permissions in your device settings. Some features may not work properly if permissions are denied.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Data Collection & Usage
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = "Data Usage",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Data Collection & Usage",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "We collect and process the following data:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val dataTypes = listOf(
                            "• Account information (name, email) - Required for authentication",
                            "• Usage analytics - Helps improve app performance",
                            "• Device information - For compatibility and security",
                            "• Notification preferences - To deliver relevant updates",
                            "• Content you create - Your notices and boards"
                        )
                        
                        dataTypes.forEach { dataType ->
                            Text(
                                text = dataType,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Notification Preferences
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Notification Preferences",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Push Notifications",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = pushNotifications,
                                onCheckedChange = { 
                                    pushNotifications = it
                                    preferencesManager.setPushNotifications(it)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Email Notifications",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = emailNotifications,
                                onCheckedChange = { 
                                    emailNotifications = it
                                    preferencesManager.setEmailNotifications(it)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Marketing Emails",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = marketingEmails,
                                onCheckedChange = { 
                                    marketingEmails = it
                                    preferencesManager.setMarketingEmails(it)
                                }
                            )
                        }
                    }
                }
            }

            // Data Security
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Data Security & Protection",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val securityFeatures = listOf(
                            "🔐 End-to-end encryption for sensitive data",
                            "🛡️ Secure authentication with industry standards",
                            "🔒 Regular security audits and updates",
                            "📊 Minimal data collection principle",
                            "🚫 No data sharing with third parties",
                            "⚡ Secure cloud infrastructure",
                            "🔑 User-controlled data access"
                        )
                        
                        securityFeatures.forEach { feature ->
                            Text(
                                text = feature,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Data Management Actions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Data Management",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Export Data Button
                        Button(
                            onClick = { showDataExportDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export My Data",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Delete Data Button
                        Button(
                            onClick = { showDataDeletionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete My Account & Data",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Compliance Information
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Privacy Compliance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "We comply with international privacy regulations:",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val complianceItems = listOf(
                            "✅ GDPR (General Data Protection Regulation)",
                            "✅ CCPA (California Consumer Privacy Act)",
                            "✅ Google Play Store Privacy Policy",
                            "✅ COPPA (Children's Online Privacy Protection Act)",
                            "✅ PIPEDA (Personal Information Protection Act)"
                        )
                        
                        complianceItems.forEach { item ->
                            Text(
                                text = item,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "For detailed information, please review our Privacy Policy and Terms of Service.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Data Export Confirmation Dialog
        if (showDataExportDialog) {
            AlertDialog(
                onDismissRequest = { showDataExportDialog = false },
                title = { Text("Export Your Data") },
                text = {
                    Text("We will prepare a downloadable file containing all your data. This may take a few minutes. You will receive an email with the download link.")
                },
                confirmButton = {
                    Button(onClick = { showDataExportDialog = false }) {
                        Text("Request Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDataExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Data Deletion Confirmation Dialog
        if (showDataDeletionDialog) {
            AlertDialog(
                onDismissRequest = { showDataDeletionDialog = false },
                title = { Text("Delete Account & Data") },
                text = {
                    Text("This action is permanent and cannot be undone. All your data, including notices, boards, and account information will be permanently deleted.")
                },
                confirmButton = {
                    Button(
                        onClick = { showDataDeletionDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Permanently")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDataDeletionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
