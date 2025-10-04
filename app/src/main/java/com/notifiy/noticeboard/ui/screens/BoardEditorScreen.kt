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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.NoticePriority
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.components.PageLimitDialog
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
    var boardCode by remember { mutableStateOf("0") }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPageLimitDialog by remember { mutableStateOf(false) }
    var currentPlanPages by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val authState by boardEditorViewModel.authState.collectAsState()
    val currentUser = authState.data
    val context = LocalContext.current

    // Load existing page if editing
    LaunchedEffect(boardId) {
        boardId?.let { id ->
            if (id.startsWith("new_")) {
                // Extract board code from route
                val codeStr = id.substringAfter("new_")
                boardCode = codeStr
                android.util.Log.d(
                    "sidxp",
                    "BoardEditorScreen - Extracted boardCode from route: '$boardCode' (length: ${boardCode.length})"
                )
                boardEditorViewModel.loadPage("new")
            } else {
                boardEditorViewModel.loadPage(id)
            }
        }
    }

    // Refresh board data when returning to this screen (e.g., from subscription)
    LaunchedEffect(Unit) {
        boardId?.let { id ->
            if (id.startsWith("new_")) {
                // Extract board code from route
                val codeStr = id.substringAfter("new_")
                boardCode = codeStr
                android.util.Log.d(
                    "sidxp", "BoardEditorScreen - Refreshing board data for code: '$boardCode'"
                )
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
            println("DEBUG: BoardEditorScreen - Loaded existing page with boardCode: ${page.code}")
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
            .padding(top = 20.dp, bottom = 50.dp)
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
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.clickable(onClick = { navController.popBackStack() })
                    )
                    Text(
                        text = if (boardId?.startsWith("new_") == true) "Create New Page" else "Edit this Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    // Show delete button only when editing existing page
                    if (boardId != null && !boardId.startsWith("new_")) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Page",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable(onClick = {
                                showDeleteDialog = true
                            })
                        )
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
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 15.dp)
                    ) {
                        item {
                            Text(
                                text = "Priority", fontSize = 16.sp, fontWeight = FontWeight.Medium
                            )
                        }
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

            // Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= 30) {
                            title = it
                        }
                    },
                    label = { Text("Page Title * (max 30 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
//                        supportingText = {
//                            Text(
//                                text = "${title.length}/30",
//                                color = if (title.length > 30) MaterialTheme.colorScheme.error
//                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//                            )
//                        }
                )
            }

            // Subtitle
            item {
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (Optional)") },
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
                                text = "Notice Points",
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = point,
                                    onValueChange = { newValue ->
                                        val newPoints = infoPoints.toMutableList()
                                        newPoints[index] = newValue
                                        infoPoints = newPoints
                                    },
                                    label = { Text("Notice Point ${index + 1} (Optional)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (infoPoints.size > 1) {
//                                        IconButton(
//                                            onClick = {
//                                                val newPoints = infoPoints.toMutableList()
//                                                newPoints.removeAt(index)
//                                                infoPoints = newPoints
//                                            }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.clickable(onClick = {
                                            val newPoints = infoPoints.toMutableList()
                                            newPoints.removeAt(index)
                                            infoPoints = newPoints
                                        })
                                    )
//                                        }
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
                    label = { Text("Additional Information (Optional)") },
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

                if (title.length > 30) {
                    validationError = "Title cannot be more than 30 characters"
                    return@Button
                }

                if (currentUser == null) {
                    validationError = "You must be logged in to create pages"
                    return@Button
                }

                // Check page limit for new pages only
                if (boardId?.startsWith("new_") == true) {
                    android.util.Log.d(
                        "sidxp", "BoardEditorScreen - Using boardCode as String: '$boardCode'"
                    )
                    android.util.Log.d("sidxp", "BoardEditorScreen - boardId: '$boardId'")
                    android.util.Log.d(
                        "sidxp",
                        "BoardEditorScreen - Calling checkPageLimit with boardCode: '$boardCode'"
                    )
                    boardEditorViewModel.checkPageLimit(boardCode) { canCreate, planPages ->
                        if (canCreate) {
                            isPublishing = true
                            val page = Page(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                subtitle = subtitle,
                                infoPoints = infoPoints.filter { it.isNotBlank() },
                                additionalInfo = additionalInfo,
                                priority = priority,
                                code = boardCode,
                                userId = currentUser.id
                            )

                            println("DEBUG: BoardEditorScreen - Creating new page with boardCode: '$boardCode'")
                            boardEditorViewModel.savePage(page) { success ->
                                isPublishing = false
                                if (success) {
                                    val message = "Page \"${title}\" created successfully!"
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else {
                                    validationError = "Failed to save page. Please try again."
                                }
                            }
                        } else {
                            currentPlanPages = planPages
                            showPageLimitDialog = true
                        }
                    }
                } else {
                    isPublishing = true
                    println("DEBUG: BoardEditorScreen - Using boardCode as String for existing page: '$boardCode'")
                    val page = Page(
                        id = boardId ?: UUID.randomUUID().toString(),
                        title = title,
                        subtitle = subtitle,
                        infoPoints = infoPoints.filter { it.isNotBlank() },
                        additionalInfo = additionalInfo,
                        priority = priority,
                        code = boardCode,
                        userId = currentUser.id
                    )

                    println("DEBUG: BoardEditorScreen - Updating existing page with boardCode: '$boardCode'")
                    boardEditorViewModel.savePage(page) { success ->
                        isPublishing = false
                        if (success) {
                            val message = "Page \"${title}\" updated successfully!"
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            navController.popBackStack()
                        } else {
                            validationError = "Failed to save page. Please try again."
                        }
                    }
                }
            },
            enabled = !isPublishing && currentUser != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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
                                        Toast.makeText(
                                            context, "Page deleted successfully!", Toast.LENGTH_LONG
                                        ).show()
                                        navController.popBackStack()
                                    } else {
                                        validationError = "Failed to delete page. Please try again."
                                    }
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(
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
                })
        }

        // Page Limit Dialog
        PageLimitDialog(
            isVisible = showPageLimitDialog,
            currentPlanPages = currentPlanPages,
            onUpgrade = {
                showPageLimitDialog = false
                // Navigate to subscription screen with the board ID
                // For new pages, we need to get the actual board ID from the board code
                if (boardId?.startsWith("new_") == true) {
                    // For new pages, we need to find the board by code
                    // Get the actual board ID from the board code
                    coroutineScope.launch {
                        try {
                            val board =
                                boardEditorViewModel.repository.getNoticeBoardByCode(boardCode)
                            if (board != null) {
                                android.util.Log.d(
                                    "sidxp",
                                    "BoardEditorScreen - Found board for upgrade: ${board.id}"
                                )
                                navController.navigate(Screen.Subscription.route)
                            } else {
                                android.util.Log.d(
                                    "sidxp",
                                    "BoardEditorScreen - Board not found for code: $boardCode"
                                )
                                // Fallback to subscription screen
                                navController.navigate(Screen.Subscription.route)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "sidxp", "BoardEditorScreen - Error finding board: ${e.message}"
                            )
                            // Fallback to subscription screen
                            navController.navigate(Screen.Subscription.route)
                        }
                    }
                } else {
                    navController.navigate(Screen.Subscription.route)
                }
            },
            onBack = {
                showPageLimitDialog = false
            })
    }
}

