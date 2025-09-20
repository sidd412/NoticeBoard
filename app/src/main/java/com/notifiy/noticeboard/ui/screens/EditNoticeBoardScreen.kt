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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import com.notifiy.noticeboard.data.model.BoardDeletionRequest
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.BoardDetailsViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.isValidPhoneNumber
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoticeBoardScreen(
    navController: NavController,
    boardId: String,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java),
    boardDetailsViewModel: BoardDetailsViewModel = cachedViewModel(BoardDetailsViewModel::class.java)
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
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    var showDeleteRequestDialog by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data

    val boardState by boardDetailsViewModel.boardState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseRepository = remember { FirebaseRepository(context) }

    // Load board data
    LaunchedEffect(boardId) {
        boardDetailsViewModel.loadBoardDetails(boardId)
    }

    // Update form fields when board data is loaded
    LaunchedEffect(boardState.data) {
        boardState.data?.let { board ->
            organizationName = board.organizationName
            organizationCode = board.organizationCode
            organizationEmail = board.organizationEmail
            organizationLocation = board.organizationLocation
            organizationWhatsapp = board.organizationWhatsapp
            isDataLoaded = true
        }
    }

    // Check for changes
    LaunchedEffect(
        organizationName,
        organizationEmail,
        organizationLocation,
        organizationWhatsapp,
        boardState.data
    ) {
        boardState.data?.let { board ->
            hasChanges =
                organizationName != board.organizationName || organizationEmail != board.organizationEmail || organizationLocation != board.organizationLocation || organizationWhatsapp != board.organizationWhatsapp
        }
    }

    // Handle navigation back after successful update
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(2000)
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background).padding(vertical = 45.dp)
        ) {
            // Top App Bar
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
                    text = "Edit Notice Board", fontWeight = FontWeight.Bold, fontSize = 20.sp
                )

                IconButton(onClick = {}) {}
            }

            if (!isDataLoaded || boardState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Content with sticky buttons at bottom
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollable content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
                            16.dp, 16.dp, 16.dp, 200.dp
                        ), // Extra padding for sticky buttons
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Organization Name
                        item {
                            OutlinedTextField(
                                value = organizationName,
                                onValueChange = { organizationName = it },
                                label = { Text("Organization Name *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = organizationName.isEmpty()
                            )
                        }

                        // Organization Code (Read-only)
                        item {
                            OutlinedTextField(
                                value = organizationCode,
                                onValueChange = { }, // Disabled
                                label = { Text("Organization Code") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    ),
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    ),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        // Organization Email
                        item {
                            OutlinedTextField(
                                value = organizationEmail,
                                onValueChange = { organizationEmail = it },
                                label = { Text("Organization Email *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email, contentDescription = "Email"
                                    )
                                },
                                isError = organizationEmail.isEmpty()
                            )
                        }

                        // Organization Location
                        item {
                            OutlinedTextField(
                                value = organizationLocation,
                                onValueChange = { organizationLocation = it },
                                label = { Text("Organization Location *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.LocationOn, contentDescription = "Location"
                                    )
                                },
                                isError = organizationLocation.isEmpty()
                            )
                        }

                        // Organization WhatsApp
                        item {
                            OutlinedTextField(
                                value = organizationWhatsapp,
                                onValueChange = { organizationWhatsapp = it },
                                label = { Text("WhatsApp Number") },
                                placeholder = { Text("1234567890") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Phone, contentDescription = "Phone"
                                    )
                                })
                        }

                        // Error Message
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
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Success Message
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
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Sticky buttons at bottom
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Update Button
                        Button(
                            onClick = {
                                // Validate form
                                if (organizationName.isEmpty()) {
                                    errorMessage = "Organization name is required"
                                    return@Button
                                }
                                if (organizationEmail.isEmpty()) {
                                    errorMessage = "Organization email is required"
                                    return@Button
                                }
                                if (organizationLocation.isEmpty()) {
                                    errorMessage = "Organization location is required"
                                    return@Button
                                }
                                if (organizationWhatsapp.isEmpty()) {
                                    errorMessage = "WhatsApp number is required"
                                    return@Button
                                }
                                if (!isValidPhoneNumber(organizationWhatsapp)) {
                                    errorMessage = "Please enter a valid 10-digit WhatsApp number"
                                    return@Button
                                }

                                // Clear previous messages
                                errorMessage = ""
                                successMessage = ""
                                isLoading = true

                                // Create updated board object
                                val currentBoard = boardState.data
                                if (currentBoard != null) {
                                    val updatedBoard = currentBoard.copy(
                                        organizationName = organizationName,
                                        organizationEmail = organizationEmail,
                                        organizationLocation = organizationLocation,
                                        organizationWhatsapp = organizationWhatsapp,
                                        updatedAt = System.currentTimeMillis()
                                    )

                                    // Update the board
                                    boardDetailsViewModel.updateNoticeBoard(updatedBoard) { success ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                "Successfully updated!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.popBackStack()
                                        } else {
                                            errorMessage =
                                                "Failed to update notice board. Please try again."
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    errorMessage = "Board data not found. Please try again."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && isDataLoaded && hasChanges
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Update Notice Board",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Request Board Deletion Button
                        Button(
                            onClick = { showDeleteRequestDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Request Board Deletion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

        }

        // Delete Request Confirmation Dialog
        if (showDeleteRequestDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteRequestDialog = false },
                title = { Text("Request Board Deletion") },
                text = {
                    Text("Are you sure you want to request deletion of this notice board? This action will send a deletion request to the administrators. The board will remain active until the request is approved.")
                },
                confirmButton = {
                    Button(
                    onClick = {
                        showDeleteRequestDialog = false
                        
                        // Check if request already exists
                        coroutineScope.launch {
                            try {
                                val existingRequest = firebaseRepository.getBoardDeletionRequestByBoardId(boardId)
                                
                                if (existingRequest != null) {
                                    // Request already exists
                                    Toast.makeText(
                                        context,
                                        "Your request already exists! We will sort it soon.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Create new deletion request
                                    val board = boardState.data
                                    if (board != null && currentUser != null) {
                                        val deletionRequest = BoardDeletionRequest(
                                            id = UUID.randomUUID().toString(),
                                            boardId = boardId,
                                            organizationName = board.organizationName,
                                            organizationCode = board.organizationCode,
                                            organizationEmail = board.organizationEmail,
                                            organizationLocation = board.organizationLocation,
                                            organizationWhatsapp = board.organizationWhatsapp,
                                            requestedBy = currentUser.id,
                                            requestReason = "User requested board deletion",
                                            status = com.notifiy.noticeboard.data.model.DeletionRequestStatus.PENDING,
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        
                                        val result = firebaseRepository.createBoardDeletionRequest(deletionRequest)
                                        if (result.isSuccess) {
                                            Toast.makeText(
                                                context,
                                                "Your request has been raised successfully! Soon we will contact you.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to submit request. Please try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Unable to process request. Please try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error processing request: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Submit Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteRequestDialog = false }) {
                        Text("Cancel")
                    }
                })
        }
    }
}
