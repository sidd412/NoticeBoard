package com.notifiy.noticeboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.components.SubscriptionRequiredDialog
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.BoardDetailsViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.getErrorMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailsScreen(
    navController: NavController,
    boardId: String,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java),
    boardDetailsViewModel: BoardDetailsViewModel = cachedViewModel(BoardDetailsViewModel::class.java)
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data


    // Load board details and pages
    LaunchedEffect(boardId) {
        boardDetailsViewModel.loadBoardDetails(boardId)
    }

    // Refresh data when screen becomes visible (e.g., returning from BoardEditor)
    DisposableEffect(Unit) {
        boardDetailsViewModel.loadBoardDetails(boardId)
        onDispose { }
    }

    val boardState by boardDetailsViewModel.boardState.collectAsState()
    val pagesState by boardDetailsViewModel.pagesState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if subscription is active
    fun isSubscriptionActive(): Boolean {
        val board = boardState.data
        if (board == null) return false

        val currentTime = System.currentTimeMillis()
        return board.subscriptionType.isNotEmpty() && board.subscriptionType != "null" && board.subscriptionExpiry > currentTime
    }

    // Handle page card click
    fun onPageCardClick(pageId: String) {
        if (isSubscriptionActive()) {
            navController.navigate(Screen.BoardEditor.createRoute(pageId))
        } else {
            navController.navigate(Screen.Subscription.createRoute(boardId))
        }
    }

    // Handle create new page click
    fun onCreateNewPageClick() {
        if (isSubscriptionActive()) {
            val boardCode = boardState.data?.organizationCode?.toIntOrNull() ?: 0
            println("DEBUG: BoardDetailsScreen - Creating new page with boardCode: $boardCode")
            navController.navigate(Screen.BoardEditor.createRoute("new_${boardCode}"))
        } else {
            navController.navigate(Screen.Subscription.createRoute(boardId))
        }
    }

    // Show error messages
    val errorMessage by boardDetailsViewModel.errorMessage.collectAsState()
    ShowErrorSnackbar(
        error = errorMessage?.let { getErrorMessage(Exception(it)) },
        snackbarHostState = snackbarHostState,
        onErrorShown = { boardDetailsViewModel.clearError() })

    // Handle back button
    BackHandler {
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 35.dp, bottom = 50.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button and header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Board Details",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Edit button
                    IconButton(
                        onClick = { 
                            navController.navigate(Screen.EditNoticeBoard.createRoute(boardId))
                        }
                    ) {
                        Icon(
                            Icons.Default.Edit, contentDescription = "Edit Board"
                        )
                    }
                }
            }

            // Board info card
            item {
                boardState.data?.let { board ->
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(7.dp,0.dp,7.dp,7.dp)
                        ) {
                            Text(
                                text = "Your Board Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            BoardInfoRow(
                                label = "Institute Name", value = board.organizationName
                            )

                            BoardInfoRow(
                                label = "Code", value = board.organizationCode
                            )
                            BoardInfoRow(
                                label = "Email", value = board.organizationEmail
                            )
                            BoardInfoRow(
                                label = "WhatsApp", value = board.organizationWhatsapp
                            )
                            BoardInfoRow(
                                label = "Location", value = board.organizationLocation
                            )
                        }
                    }
                }
            }

            // Pages section
            item {
                Text(
                    text = "Pages On this Board",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 7.dp)
                )
            }

            // Loading state
            if (pagesState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            // Empty state
            else if (pagesState.data.isNullOrEmpty()) {
                item {
                    println("DEBUG: BoardDetailsScreen - Showing empty state. pagesState.data: ${pagesState.data}")
                    println("DEBUG: BoardDetailsScreen - pagesState.isLoading: ${pagesState.isLoading}")
                    println("DEBUG: BoardDetailsScreen - pagesState.error: ${pagesState.error}")
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No pages found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first page to get started",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            // Pages list
            else {
                items(pagesState.data ?: emptyList()) { page ->
                    println("DEBUG: BoardDetailsScreen - Rendering page: ${page.title}")
                    PageCard(
                        page = page, 
                        onClick = { onPageCardClick(page.id) }
                    )
                }
            }
        }

        // Fixed Create New Page Button
        Button(
            onClick = { onCreateNewPageClick() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create New Page",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create New Page", fontSize = 16.sp, fontWeight = FontWeight.Medium
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)
        )

    }
}

@Composable
fun BoardInfoRow(
    label: String, value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PageCard(
    page: Page, 
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when (page.priority.uppercase()) {
                            "HIGH", "URGENT" -> Color(0xFFF44336)
                            "NORMAL" -> Color(0xFF2196F3)
                            "LOW" -> Color(0xFF4CAF50)
                            else -> Color(0xFF2196F3)
                        }
                    )
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "${page.priority.uppercase()}-Priority",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = page.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (page.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = page.subtitle,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info points preview
            if (page.infoPoints.isNotEmpty()) {
                Text(
                    text = "• ${page.infoPoints.first()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (page.infoPoints.size > 1) {
                    Text(
                        text = "+${page.infoPoints.size - 1} more points",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Created date
            Text(
                text = "Created: ${
                    SimpleDateFormat(
                        "MMM dd, yyyy", Locale.getDefault()
                    ).format(Date(page.createdAt))
                }", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
