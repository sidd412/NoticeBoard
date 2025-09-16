package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.NoticePriority
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.components.HorizontalPagesCarousel
import com.notifiy.noticeboard.ui.viewmodel.HomeViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import kotlinx.coroutines.launch

@Composable
fun NoticeViewerScreen(
    navController: NavController,
    boardId: String,
    homeViewModel: HomeViewModel = cachedViewModel(HomeViewModel::class.java)
) {
    var pages by remember { mutableStateOf<List<Page>>(emptyList()) }
    var noticeBoard by remember { mutableStateOf<NoticeBoard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(boardId) {
        try {
            isLoading = true
            println("DEBUG: NoticeViewerScreen - Loading pages for boardId: $boardId")
            
            // Load board details first
            noticeBoard = homeViewModel.getNoticeBoardById(boardId)
            println("DEBUG: NoticeViewerScreen - Loaded board: $noticeBoard")
            
            if (noticeBoard != null) {
                val boardCode = noticeBoard!!.organizationCode.toIntOrNull() ?: 0
                println("DEBUG: NoticeViewerScreen - Board code: $boardCode")
                
                // Load pages for this board
                pages = homeViewModel.getPagesByBoardCode(boardCode)
                println("DEBUG: NoticeViewerScreen - Loaded ${pages.size} pages")
            }
            
            isLoading = false
        } catch (e: Exception) {
            println("DEBUG: NoticeViewerScreen - Error: ${e.message}")
            errorMessage = e.message ?: "Failed to load pages"
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Close button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (pages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Pages Available",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "There are no pages available for this board at the moment.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with board info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = noticeBoard?.organizationName ?: "Notice Board",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pages.size} pages available",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Horizontal Pages Carousel
                HorizontalPagesCarousel(
                    pages = pages,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    onPageClick = { page ->
                        // Navigate to page details or do something with the page
                        println("DEBUG: NoticeViewerScreen - Page clicked: ${page.title}")
                    }
                )
            }
        }
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Priority indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (notice.priority) {
                                NoticePriority.URGENT -> Color.Red
                                NoticePriority.HIGH -> Color(0xFFFF9800)
                                NoticePriority.NORMAL -> Color(0xFF4CAF50)
                                NoticePriority.LOW -> Color(0xFF2196F3)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = notice.priority.name.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = notice.subtitle.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title
            Text(
                text = notice.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Text(
                text = notice.content,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 24.sp
            )
            
            // Info Points
            if (notice.infoPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Key Points:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                notice.infoPoints.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = point,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date info
            Text(
                text = "Posted on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(notice.createdAt))}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
