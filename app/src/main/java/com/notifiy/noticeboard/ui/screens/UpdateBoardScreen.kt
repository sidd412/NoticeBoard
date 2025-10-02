package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.notifiy.noticeboard.ui.components.LocationTextField
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.ValidationUtils
import com.notifiy.noticeboard.utils.isValidPhoneNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBoardScreen(
    navController: NavController,
    boardId: String,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    var organizationName by remember { mutableStateOf("") }
    var organizationCode by remember { mutableStateOf("") }
    var organizationEmail by remember { mutableStateOf("") }
    var organizationLocation by remember { mutableStateOf("") }
    var organizationWhatsapp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isDataLoaded by remember { mutableStateOf(false) }
    
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    
    // Load board data
    LaunchedEffect(boardId) {
        // TODO: Load board data from repository
        // For now, simulate with sample data
        organizationName = "Sample Institute"
        organizationCode = "SAMPLE001"
        organizationEmail = "contact@sample.edu"
        organizationLocation = "Sample City"
        organizationWhatsapp = "1234567890"
        isDataLoaded = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Update Notice Board",
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
        
        if (!isDataLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                                text = "Update Notice Board",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Update the details of your notice board",
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
                    LocationTextField(
                        value = organizationLocation,
                        onValueChange = { organizationLocation = it },
                        label = "Organization Location",
                        placeholder = "Enter city, state"
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = organizationWhatsapp,
                        onValueChange = { organizationWhatsapp = it },
                        label = { Text("WhatsApp Number") },
                        placeholder = { Text("1234567890") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
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
                
                // Update Button
                item {
                    Button(
                        onClick = {
                            // Validate form
                            if (organizationName.isBlank() || organizationCode.isBlank() || 
                                organizationEmail.isBlank() || organizationLocation.isBlank() || 
                                organizationWhatsapp.isBlank()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            
                            // Validate email format
                            if (!ValidationUtils.isValidEmail(organizationEmail)) {
                                errorMessage = "Please enter a valid email address"
                                return@Button
                            }
                            
                            // Validate WhatsApp number format
                            if (!isValidPhoneNumber(organizationWhatsapp)) {
                                errorMessage = "Please enter a valid 10-digit WhatsApp number"
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = ""
                            
                            // TODO: Update board in Firebase
                            // For now, simulate update
                            val updatedBoard = NoticeBoard(
                                id = boardId,
                                organizationName = organizationName,
                                organizationCode = organizationCode,
                                organizationEmail = organizationEmail,
                                organizationLocation = organizationLocation,
                                organizationWhatsapp = organizationWhatsapp,
                                createdBy = currentUser?.id ?: ""
                            )
                            
                            // Simulate API call
                            successMessage = "Notice board updated successfully!"
                            isLoading = false
                            
                            // Navigate back after successful update
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Update Notice Board",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Delete Button
                item {
                    Button(
                        onClick = {
                            // TODO: Implement delete functionality
                            errorMessage = "Delete functionality not implemented yet"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "Delete Notice Board",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
