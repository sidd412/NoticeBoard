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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.navigation.BottomNavScreen
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.YourBoardsViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.QRCodeUtils
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.getErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourBoardsScreen(
    navController: NavController, mainNavController: NavController, authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    val yourBoardsViewModel: YourBoardsViewModel = cachedViewModel(YourBoardsViewModel::class.java)
    val userBoardsState by yourBoardsViewModel.userBoards.collectAsState()
    val errorMessage by yourBoardsViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle mobile back button
    BackHandler {
        println("DEBUG: YourBoardsScreen - Mobile back button pressed")
        // Navigate to Home tab instead of popBackStack
        navController.navigate(BottomNavScreen.Home.route)
    }

    // Load user boards when screen is displayed
    LaunchedEffect(currentUser) {
        println("DEBUG: YourBoardsScreen - LaunchedEffect triggered")
        try {
            currentUser?.let { user ->
                println("DEBUG: YourBoardsScreen - Loading boards for user: ${user.id}")
                yourBoardsViewModel.loadUserBoards(user.id)
            } ?: run {
                println("DEBUG: YourBoardsScreen - No current user found")
            }
        } catch (e: Exception) {
            println("DEBUG: YourBoardsScreen - Error in LaunchedEffect: ${e.message}")
            // Don't crash the app, just log the error
        }
    }

    // Cleanup when navigating away
    DisposableEffect(Unit) {
        onDispose {
            println("DEBUG: YourBoardsScreen - Disposing screen")
            try {
                yourBoardsViewModel.clearError()
            } catch (e: Exception) {
                println("DEBUG: YourBoardsScreen - Error during cleanup: ${e.message}")
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
                                    } else {
                                        println("DEBUG: YourBoardsScreen - Failed to delete board")
                                    }
                                }
                            }
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
            hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun YourBoardCard(
    board: NoticeBoard, 
    onUpdateClick: () -> Unit,
    onDeleteClick: (NoticeBoard) -> Unit
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

                // Status
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
                    Text("Are you sure you want to delete \"${board.organizationName}\"? This action cannot be undone and will permanently remove the board and all its data.")
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
