package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.SubscriptionMethod
import com.notifiy.noticeboard.data.model.SubscriptionRequest
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.HomeViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribePopupScreen(
    navController: NavController,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java),
    homeViewModel: HomeViewModel = cachedViewModel(HomeViewModel::class.java)
) {
    var selectedMethod by remember { mutableStateOf(SubscriptionMethod.NONE) }
    var whatsappNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var noticeBoardCode by remember { mutableStateOf("") }
    var qrCodeData by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Subscribe to Notice Board",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Choose Subscription Method",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select how you want to subscribe to a notice board",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // QR Code Option
            item {
                SubscriptionMethodCard(
                    title = "Scan QR Code",
                    description = "Scan the QR code provided by the institute",
                    icon = Icons.Default.Star,
                    isSelected = selectedMethod == SubscriptionMethod.QR_CODE,
                    onClick = { 
                        selectedMethod = SubscriptionMethod.QR_CODE
                        errorMessage = ""
                    }
                )
            }
            
            // WhatsApp Option
            item {
                SubscriptionMethodCard(
                    title = "WhatsApp Number",
                    description = "Enter the WhatsApp number of the institute",
                    icon = Icons.Default.Warning,
                    isSelected = selectedMethod == SubscriptionMethod.WHATSAPP,
                    onClick = { 
                        selectedMethod = SubscriptionMethod.WHATSAPP
                        errorMessage = ""
                    }
                )
            }
            
            // Email Option
            item {
                SubscriptionMethodCard(
                    title = "Email Address",
                    description = "Enter the email address of the institute",
                    icon = Icons.Default.Email,
                    isSelected = selectedMethod == SubscriptionMethod.EMAIL,
                    onClick = { 
                        selectedMethod = SubscriptionMethod.EMAIL
                        errorMessage = ""
                    }
                )
            }
            
            // Code Option
            item {
                SubscriptionMethodCard(
                    title = "Notice Board Code",
                    description = "Enter the unique code provided by the institute",
                    icon = Icons.Default.Email, // Using email icon as placeholder
                    isSelected = selectedMethod == SubscriptionMethod.CODE,
                    onClick = { 
                        selectedMethod = SubscriptionMethod.CODE
                        errorMessage = ""
                    }
                )
            }
            
            // Input Fields based on selection
            when (selectedMethod) {
                SubscriptionMethod.WHATSAPP -> {
                    item {
                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it },
                            label = { Text("WhatsApp Number") },
                            placeholder = { Text("+1234567890") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                }
                SubscriptionMethod.EMAIL -> {
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("institute@example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                }
                SubscriptionMethod.CODE -> {
                    item {
                        OutlinedTextField(
                            value = noticeBoardCode,
                            onValueChange = { noticeBoardCode = it },
                            label = { Text("Notice Board Code") },
                            placeholder = { Text("Enter unique code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                }
                SubscriptionMethod.QR_CODE -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Star,
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
                                    text = "Point your camera at the QR code to scan",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        // TODO: Implement QR code scanning
                                        qrCodeData = "sample_qr_data"
                                    }
                                ) {
                                    Text("Scan QR Code")
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
            
            // Error/Success Messages
            if (errorMessage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            if (successMessage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = successMessage,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Subscribe Button
            item {
                Button(
                    onClick = {
                        if (selectedMethod == SubscriptionMethod.NONE) {
                            errorMessage = "Please select a subscription method"
                            return@Button
                        }
                        
                        val request = SubscriptionRequest(
                            whatsappNumber = whatsappNumber,
                            email = email,
                            noticeBoardCode = noticeBoardCode,
                            qrCodeData = qrCodeData,
                            subscriptionMethod = selectedMethod
                        )
                        
                        println("DEBUG: SubscribePopupScreen - Button clicked, selectedMethod: $selectedMethod")
                        println("DEBUG: SubscribePopupScreen - noticeBoardCode: $noticeBoardCode")
                        println("DEBUG: SubscribePopupScreen - whatsappNumber: $whatsappNumber")
                        println("DEBUG: SubscribePopupScreen - email: $email")
                        println("DEBUG: SubscribePopupScreen - qrCodeData: $qrCodeData")
                        println("DEBUG: SubscribePopupScreen - currentUser: $currentUser")
                        println("DEBUG: SubscribePopupScreen - isLoading: $isLoading")
                        println("DEBUG: SubscribePopupScreen - Button enabled: ${!isLoading && selectedMethod != SubscriptionMethod.NONE}")
                        
                        // Implement real subscription logic
                        isLoading = true
                        errorMessage = ""
                        
                        if (currentUser == null) {
                            errorMessage = "Please sign in to subscribe to boards"
                            isLoading = false
                            return@Button
                        }
                        
                        // Get the institute code based on the subscription method
                        val instituteCode = when (selectedMethod) {
                            SubscriptionMethod.CODE -> noticeBoardCode
                            SubscriptionMethod.EMAIL -> {
                                // For now, use email as code (this should be improved)
                                email
                            }
                            SubscriptionMethod.WHATSAPP -> {
                                // For now, use WhatsApp as code (this should be improved)
                                whatsappNumber
                            }
                            SubscriptionMethod.QR_CODE -> {
                                // Extract code from QR code data
                                qrCodeData
                            }
                            else -> ""
                        }
                        
                        println("DEBUG: SubscribePopupScreen - Calculated instituteCode: $instituteCode")
                        
                        if (instituteCode.isBlank()) {
                            errorMessage = "Please enter a valid code or contact information"
                            isLoading = false
                            return@Button
                        }
                        
                        println("DEBUG: SubscribePopupScreen - Subscribing to board with code: $instituteCode")
                        
                        // Call the repository to subscribe
                        homeViewModel.subscribeToBoard(currentUser?.id ?: "", instituteCode) { result ->
                            isLoading = false
                            result.fold(
                                onSuccess = { success ->
                                    if (success) {
                                        println("DEBUG: SubscribePopupScreen - Subscription successful")
                                        successMessage = "Successfully subscribed to notice board!"
                                        // Navigate back after successful subscription
                                        navController.popBackStack()
                                    } else {
                                        errorMessage = "Failed to subscribe to board"
                                    }
                                },
                                onFailure = { exception ->
                                    println("DEBUG: SubscribePopupScreen - Subscription failed: ${exception.message}")
                                    errorMessage = exception.message ?: "Failed to subscribe to board"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && selectedMethod != SubscriptionMethod.NONE
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Subscribe",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}
