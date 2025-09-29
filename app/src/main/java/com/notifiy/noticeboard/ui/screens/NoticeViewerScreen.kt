package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.foundation.border
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.ui.components.HorizontalPagesCarousel
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.HomeViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel

@Composable
fun NoticeViewerScreen(
    navController: NavController,
    boardId: String,
    homeViewModel: HomeViewModel = cachedViewModel(HomeViewModel::class.java),
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    var pages by remember { mutableStateOf<List<Page>>(emptyList()) }
    var noticeBoard by remember { mutableStateOf<NoticeBoard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    val context = LocalContext.current

    LaunchedEffect(boardId) {
        try {
            isLoading = true
            println("DEBUG: NoticeViewerScreen - Loading pages for boardId: $boardId")

            // Load board details first
            noticeBoard = homeViewModel.getNoticeBoardById(boardId)
            println("DEBUG: NoticeViewerScreen - Loaded board: $noticeBoard")

            if (noticeBoard != null) {
                val boardCode = noticeBoard!!.organizationCode
                println("DEBUG: NoticeViewerScreen - Board code: '$boardCode'")

                // Load pages for this board
                pages = homeViewModel.getPagesByBoardCode(boardCode)
                println("DEBUG: NoticeViewerScreen - Loaded ${pages.size} pages")

                // Mark notification as read when user views the board
                currentUser?.let { user ->
                    homeViewModel.markNotificationAsRead(user.id, boardId)
                }
            }

            isLoading = false
        } catch (e: Exception) {
            println("DEBUG: NoticeViewerScreen - Error: ${e.message}")
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 50.dp)
    )
    {

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (pages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxHeight(.65f)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Pages Available",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "There are no pages available for this board at the moment.",
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else {// Horizontal Pages Carousel
                    HorizontalPagesCarousel(
                        pages = pages,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        onPageClick = { page ->
                            // Navigate to page details or do something with the page
                            println("DEBUG: NoticeViewerScreen - Page clicked: ${page.title}")
                        })
                }

                // Header with board info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${noticeBoard?.organizationName} Notice Board",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total ${pages.size} Notice available",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Unsubscribe button
                Button(
                    onClick = { showUnsubscribeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(
                        text = "Unsubscribe from Board",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ) }

                // Close button
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Back To Home", fontSize = 16.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    
    // Unsubscribe confirmation dialog
    if (showUnsubscribeDialog) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeDialog = false },
            title = { Text("Unsubscribe from Board") },
            text = { 
                Text("Are you sure you want to unsubscribe from \"${noticeBoard?.organizationName}\"? You will no longer receive updates from this board.")
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showUnsubscribeDialog = false
                        currentUser?.let { user ->
                            noticeBoard?.let { board ->
                                homeViewModel.unsubscribeFromBoard(user.id, board.organizationCode) { result ->
                                    result.fold(
                                        onSuccess = {
                                            println("DEBUG: Successfully unsubscribed from ${board.organizationName}")
                                            Toast.makeText(context, "Successfully unsubscribed from ${board.organizationName}!", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        },
                                        onFailure = { exception ->
                                            println("DEBUG: Failed to unsubscribe: ${exception.message}")
                                            Toast.makeText(context, "Failed to unsubscribe. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unsubscribe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}