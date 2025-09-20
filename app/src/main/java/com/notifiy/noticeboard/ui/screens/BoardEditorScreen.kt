package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.NoticePriority
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.components.SubscriptionRequiredDialog
import com.notifiy.noticeboard.ui.viewmodel.BoardEditorViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.getErrorMessage
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorScreen(
    navController: NavController,
    boardId: String? = null,
    boardEditorViewModel: BoardEditorViewModel = cachedViewModel(BoardEditorViewModel::class.java)
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var infoPoints by remember { mutableStateOf(listOf("")) }
    var priority by remember { mutableStateOf("LOW") }
    var isPublishing by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }
    var boardCode by remember { mutableStateOf(0) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val authState by boardEditorViewModel.authState.collectAsState()
    val currentUser = authState.data

    // Load existing page if editing
    LaunchedEffect(boardId) {
        boardId?.let { id ->
            if (id.startsWith("new_")) {
                // Extract board code from route
                val codeStr = id.substringAfter("new_")
                boardCode = codeStr.toIntOrNull() ?: 0
                println("DEBUG: BoardEditorScreen - Extracted boardCode from route: $boardCode")
                boardEditorViewModel.loadPage("new")
            } else {
                boardEditorViewModel.loadPage(id)
            }
        }
    }

    // Update UI when page is loaded
    val currentPage by boardEditorViewModel.currentPage.collectAsState()
    LaunchedEffect(currentPage) {
        currentPage?.let { page ->
            title = page.title
            subtitle = page.subtitle
            additionalInfo = page.additionalInfo
            infoPoints = if (page.infoPoints.isEmpty()) listOf("") else page.infoPoints
            priority = page.priority
            boardCode = page.code
        }
    }

    // Show error messages
    val errorMessage by boardEditorViewModel.errorMessage.collectAsState()
    ShowErrorSnackbar(
        error = errorMessage?.let { getErrorMessage(Exception(it)) },
        snackbarHostState = snackbarHostState,
        onErrorShown = { boardEditorViewModel.clearError() })

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
            .padding(top = 35.dp, bottom = 50.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
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
                            text = if (boardId?.startsWith("new_") == true) "Create New Page" else "Edit this Page",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        // Show delete button only when editing existing page
                        if (boardId != null && !boardId.startsWith("new_")) {
                            IconButton(
                                onClick = { 
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete Page",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            IconButton(onClick = {}) {}
                        }
                    }
                }

                // Priority
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Priority", fontSize = 16.sp, fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("LOW", "NORMAL", "HIGH", "URGENT").forEach { prio ->
                                    item {
                                        FilterChip(
                                            onClick = { priority = prio },
                                            label = { Text(prio) },
                                            selected = priority == prio
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Page Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Subtitle
                item {
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Info Points
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Info Points",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = {
                                        infoPoints = infoPoints + ""
                                    }) {
                                    Icon(
                                        Icons.Default.Add, contentDescription = "Add Info Point"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            infoPoints.forEachIndexed { index, point ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = point,
                                        onValueChange = { newValue ->
                                            val newPoints = infoPoints.toMutableList()
                                            newPoints[index] = newValue
                                            infoPoints = newPoints
                                        },
                                        label = { Text("Info Point ${index + 1}") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    if (infoPoints.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val newPoints = infoPoints.toMutableList()
                                                newPoints.removeAt(index)
                                                infoPoints = newPoints
                                            }) {
                                            Icon(
                                                Icons.Default.Delete, contentDescription = "Delete"
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Additional Info
                item {
                    OutlinedTextField(
                        value = additionalInfo,
                        onValueChange = { additionalInfo = it },
                        label = { Text("Additional Information") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            }
//        }

        Button(
            onClick = {
                if (title.isBlank()) {
                    validationError = "Please enter a title"
                    return@Button
                }

                if (currentUser == null) {
                    validationError = "You must be logged in to create pages"
                    return@Button
                }

                isPublishing = true
                val page = Page(
                    id = if (boardId?.startsWith("new_") == true) UUID.randomUUID()
                        .toString() else boardId ?: UUID.randomUUID().toString(),
                    title = title,
                    subtitle = subtitle,
                    infoPoints = infoPoints.filter { it.isNotBlank() },
                    additionalInfo = additionalInfo,
                    priority = priority,
                    code = boardCode,
                    userId = currentUser.id
                )

                println("DEBUG: BoardEditorScreen - Creating page with boardCode: $boardCode")
                println("DEBUG: BoardEditorScreen - Page data: $page")
                boardEditorViewModel.savePage(page) { success ->
                    isPublishing = false
                    if (success) {
                        navController.popBackStack()
                    } else {
                        validationError = "Failed to save page. Please try again."
                    }
                }
            },
            enabled = !isPublishing && currentUser != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Send, contentDescription = "Save")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (boardId?.startsWith("new_") == true) "Create" else "Update")
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Subscription Required Dialog
        SubscriptionRequiredDialog(
            isVisible = showSubscriptionDialog,
            onDismiss = { showSubscriptionDialog = false },
            onSubscribe = {
                showSubscriptionDialog = false
                // Navigate back to BoardDetails and then to Subscription
                navController.popBackStack()
                // Note: We need to get the boardId from the route or pass it as parameter
                // For now, we'll just close the dialog
            },
            boardName = "Notice Board"
        )
        
        // Delete Page Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Page") },
                text = { 
                    Text("Are you sure you want to delete this page? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDeleteDialog = false
                            boardId?.let { id ->
                                boardEditorViewModel.deletePage(id) { success ->
                                    if (success) {
                                        navController.popBackStack()
                                    } else {
                                        validationError = "Failed to delete page. Please try again."
                                    }
                                }
                            }
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
