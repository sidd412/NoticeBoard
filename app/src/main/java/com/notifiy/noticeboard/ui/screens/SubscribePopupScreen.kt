package com.notifiy.noticeboard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.notifiy.noticeboard.data.model.SubscriptionMethod
import com.notifiy.noticeboard.data.model.SubscriptionRequest
import com.notifiy.noticeboard.ui.components.QRScannerComponent
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.HomeViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.QRCodeUtils
import com.notifiy.noticeboard.utils.ValidationUtils
import com.notifiy.noticeboard.utils.isValidPhoneNumber

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 35.dp, bottom = 50.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Subscribe A Board",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    IconButton(onClick = {}) {}
                }
            }

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
                    title = "QR Code",
                    description = "Scan the QR code provided by the institute",
                    icon = Icons.Default.QrCodeScanner,
                    isSelected = selectedMethod == SubscriptionMethod.QR_CODE,
                    onClick = {
                        selectedMethod = SubscriptionMethod.QR_CODE
                        errorMessage = ""
                    })
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
                    })
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
                    })
            }

            // Input Fields based on selection
            when (selectedMethod) {
                SubscriptionMethod.WHATSAPP -> {
                    item {
                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it },
                            label = { Text("WhatsApp Number") },
                            placeholder = { Text("1234567890") },
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
                        if (isLoading) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Subscribing to board...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            QRScannerComponent(
                            onQRCodeScanned = { scannedData ->
                                println("DEBUG: QR Code scanned - Raw data: $scannedData")
                                // Parse the QR code data
                                val qrBoardData = QRCodeUtils.parseQRCodeData(scannedData)
                                if (qrBoardData != null) {
                                    println("DEBUG: QR Code parsed successfully - Board: ${qrBoardData.organizationName}, Code: ${qrBoardData.organizationCode}")
                                    qrCodeData = qrBoardData.organizationCode
                                    successMessage = "QR Code scanned successfully! Board: ${qrBoardData.organizationName}"
                                    errorMessage = ""
                                    
                                    // Auto-subscribe after successful QR scan
                                    if (currentUser != null && qrBoardData.organizationCode.isNotBlank()) {
                                        // First check if already subscribed
                                        coroutineScope.launch {
                                            val isAlreadySubscribed = homeViewModel.isUserSubscribedToBoard(
                                                currentUser.id, qrBoardData.organizationCode
                                            )
                                            
                                            if (isAlreadySubscribed) {
                                                Toast.makeText(
                                                    context,
                                                    "You are already subscribed to ${qrBoardData.organizationName}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                errorMessage = ""
                                                successMessage = ""
                                            } else {
                                                isLoading = true
                                                homeViewModel.subscribeToBoard(
                                                    currentUser.id, qrBoardData.organizationCode
                                                ) { result ->
                                                    isLoading = false
                                                    result.fold(onSuccess = { success ->
                                                        if (success) {
                                                            println("DEBUG: Auto-subscription successful")
                                                                                                                    Toast.makeText(
                                                            context,
                                                            "Successfully subscribed to ${qrBoardData.organizationName}!",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        // Force refresh the home screen
                                                        homeViewModel.forceRefreshSubscribedBoards(currentUser.id)
                                                        // Navigate back after successful subscription
                                                        navController.popBackStack()
                                                        } else {
                                                            errorMessage = "Failed to subscribe to ${qrBoardData.organizationName}"
                                                        }
                                                    }, onFailure = { exception ->
                                                        println("DEBUG: Auto-subscription failed: ${exception.message}")
                                                        errorMessage = exception.message ?: "Failed to subscribe to board"
                                                    })
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    println("DEBUG: QR Code parsing failed")
                                    errorMessage = "Invalid QR code format. Please scan a valid board QR code."
                                    qrCodeData = ""
                                }
                            },
                            onError = { error ->
                                println("DEBUG: QR Scanner error: $error")
                                errorMessage = error
                                qrCodeData = ""
                            }
                        )
                        }
                    }
                }

                else -> {}
            }

            // Error/Success Messages
            if (errorMessage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
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
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
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

        }
        Button(
            onClick = {
                if (selectedMethod == SubscriptionMethod.NONE) {
                    errorMessage = "Please select a subscription method"
                    return@Button
                }

                // Validate WhatsApp number if WhatsApp method is selected
                if (selectedMethod == SubscriptionMethod.WHATSAPP && !isValidPhoneNumber(whatsappNumber)) {
                    errorMessage = "Please enter a valid 10-digit WhatsApp number"
                    return@Button
                }
                
                // Validate email if EMAIL method is selected
                if (selectedMethod == SubscriptionMethod.EMAIL && !ValidationUtils.isValidEmail(email)) {
                    errorMessage = "Please enter a valid email address"
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

                // First check if already subscribed
                coroutineScope.launch {
                    val isAlreadySubscribed = homeViewModel.isUserSubscribedToBoard(
                        currentUser.id, instituteCode
                    )
                    
                    if (isAlreadySubscribed) {
                        isLoading = false
                        Toast.makeText(
                            context,
                            "You are already subscribed to this board",
                            Toast.LENGTH_LONG
                        ).show()
                        errorMessage = ""
                        successMessage = ""
                    } else {
                        // Call the repository to subscribe
                        homeViewModel.subscribeToBoard(
                            currentUser.id, instituteCode
                        ) { result ->
                            isLoading = false
                            result.fold(onSuccess = { success ->
                                if (success) {
                                    println("DEBUG: SubscribePopupScreen - Subscription successful")
                                    Toast.makeText(
                                        context,
                                        "Successfully subscribed to notice board!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Force refresh the home screen
                                    homeViewModel.forceRefreshSubscribedBoards(currentUser.id)
                                    // Navigate back after successful subscription
                                    navController.popBackStack()
                                } else {
                                    errorMessage = "Failed to subscribe to board"
                                }
                            }, onFailure = { exception ->
                                println("DEBUG: SubscribePopupScreen - Subscription failed: ${exception.message}")
                                errorMessage = exception.message ?: "Failed to subscribe to board"
                            })
                        }
                    }
                }
            },
            enabled = !isLoading && selectedMethod != SubscriptionMethod.NONE,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = Color.White
                )
            } else {
                Text(
                    text = "Subscribe", fontSize = 16.sp, fontWeight = FontWeight.Medium
                )
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
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ), onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Via $title",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                RadioButton(
                    selected = true, onClick = null, colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}
