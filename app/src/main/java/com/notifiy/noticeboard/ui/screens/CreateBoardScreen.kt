package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
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
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.YourBoardsViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.getErrorMessage
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBoardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    var organizationName by remember { mutableStateOf("") }
    var organizationCode by remember { mutableStateOf("") }
    var organizationEmail by remember { mutableStateOf("") }
    var organizationLocation by remember { mutableStateOf("") }
    var organizationWhatsapp by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data

    val yourBoardsViewModel: YourBoardsViewModel = cachedViewModel(YourBoardsViewModel::class.java)
    val errorMessage by yourBoardsViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    ShowErrorSnackbar(
        error = errorMessage?.let { getErrorMessage(Exception(it)) },
        snackbarHostState = snackbarHostState,
        onErrorShown = { yourBoardsViewModel.clearError() })

    // Handle validation errors
    LaunchedEffect(validationError) {
        if (validationError.isNotEmpty()) {
            snackbarHostState.showSnackbar(validationError)
            validationError = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Content
        LazyColumn(
            modifier = Modifier,
//                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 50.dp, 16.dp, 50.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        )
        {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Create Notice Board", fontWeight = FontWeight.Bold, fontSize = 22.sp
                    )
                    IconButton(onClick = {}) {}
                }
            }
            // Header
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
                            text = "Create Your Notice Board",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fill in the details to create a new notice board for your institute",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Form Fields
            item {
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    label = { Text("Organization Name") },
                    placeholder = { Text("Enter your institute name") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }

            item {
                OutlinedTextField(
                    value = organizationCode,
                    onValueChange = { organizationCode = it },
                    label = { Text("Organization Code") },
                    placeholder = { Text("Enter unique code (e.g., ABC001)") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }

            item {
                OutlinedTextField(
                    value = organizationEmail,
                    onValueChange = { organizationEmail = it },
                    label = { Text("Organization Email") },
                    placeholder = { Text("contact@yourinstitute.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            item {
                OutlinedTextField(
                    value = organizationLocation,
                    onValueChange = { organizationLocation = it },
                    label = { Text("Organization Location") },
                    placeholder = { Text("Enter city, state") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }

            item {
                OutlinedTextField(
                    value = organizationWhatsapp,
                    onValueChange = { organizationWhatsapp = it },
                    label = { Text("WhatsApp Number") },
                    placeholder = { Text("+1234567890") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            // Authentication Status
            if (currentUser == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Please sign in to create a notice board",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Error/Success Messages
            if (errorMessage?.isNotEmpty() == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Create Button
            item {
                Button(
                    onClick = {
                        println("DEBUG: CreateBoardScreen - Create button clicked")

                        // Check if user is authenticated
                        if (currentUser == null) {
                            println("DEBUG: CreateBoardScreen - User not authenticated")
                            validationError = "Please sign in to create a notice board"
                            return@Button
                        }

                        println("DEBUG: CreateBoardScreen - User authenticated: ${currentUser.id}")

                        // Validate form
                        if (organizationName.isBlank() || organizationCode.isBlank() || organizationEmail.isBlank() || organizationLocation.isBlank() || organizationWhatsapp.isBlank()) {
                            println("DEBUG: CreateBoardScreen - Form validation failed: empty fields")
                            validationError = "Please fill all fields"
                            return@Button
                        }

                        // Validate email format
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(organizationEmail)
                                .matches()
                        ) {
                            println("DEBUG: CreateBoardScreen - Email validation failed")
                            validationError = "Please enter a valid email address"
                            return@Button
                        }

                        // Validate WhatsApp number format
                        if (!organizationWhatsapp.startsWith("+") || organizationWhatsapp.length < 10) {
                            println("DEBUG: CreateBoardScreen - WhatsApp validation failed")
                            validationError =
                                "Please enter a valid WhatsApp number with country code"
                            return@Button
                        }

                        println("DEBUG: CreateBoardScreen - Form validation passed")
                        isCreating = true

                        // Create board in Firebase
                        val newBoard = NoticeBoard(
                            id = UUID.randomUUID().toString(),
                            organizationName = organizationName,
                            organizationCode = organizationCode,
                            organizationEmail = organizationEmail,
                            organizationLocation = organizationLocation,
                            organizationWhatsapp = organizationWhatsapp,
                            createdBy = currentUser?.id ?: ""
                        )

                        println("DEBUG: CreateBoardScreen - Calling createNoticeBoard with: $newBoard")

                        yourBoardsViewModel.createNoticeBoard(newBoard) { success ->
                            println("DEBUG: CreateBoardScreen - createNoticeBoard result: $success")
                            isCreating = false
                            if (success) {
                                println("DEBUG: CreateBoardScreen - Board created successfully, navigating back")
                                navController.popBackStack()
                            } else {
                                println("DEBUG: CreateBoardScreen - Board creation failed")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating && currentUser != null
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), color = Color.White
                        )
                    } else {
                        Text(
                            text = "Create Notice Board",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Subscription Info
//            item {
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                )
//                {
//                    Column(
//                        modifier = Modifier.padding(16.dp)
//                    ) {
//                        Text(
//                            text = "Subscription Information",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = "Your notice board will be created with a free subscription. You can upgrade to premium later for additional features.",
//                            fontSize = 14.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Button(
//                            onClick = {
//                                // TODO: Navigate to subscription page
//                            },
//                            modifier = Modifier.fillMaxWidth(),
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.primary
//                            )
//                        ) {
//                            Text("View Subscription Plans")
//                        }
//                    }
//                }
//            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )

    }
}
