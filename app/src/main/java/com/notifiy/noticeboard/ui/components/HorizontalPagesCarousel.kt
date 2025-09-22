package com.notifiy.noticeboard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifiy.noticeboard.data.model.Page
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagesCarousel(
    pages: List<Page>, modifier: Modifier = Modifier, onPageClick: (Page) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Page indicator and navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = {
                    if (currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentPage - 1)
                        }
                    }
                }, enabled = currentPage > 0
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = if (currentPage > 0) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Page indicator
            Text(
                text = "${currentPage + 1} of ${pages.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Next button
            IconButton(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    }
                }, enabled = currentPage < pages.size - 1
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = if (currentPage < pages.size - 1) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal pager
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxWidth(), pageSpacing = 16.dp
        ) { pageIndex ->
            PageCarouselCard(
                page = pages[pageIndex],
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPageClick(pages[pageIndex]) })
        }

        // Dots indicator
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                    if (index < pages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PageCarouselCard(
    page: Page, modifier: Modifier = Modifier, onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle (instead of description)
            if (page.subtitle.isNotEmpty()) {
                Text(
                    text = page.subtitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Info Points (Key Points)
            if (page.infoPoints.isNotEmpty()) {
                Text(
                    text = "Key Points:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                page.infoPoints.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary
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

            // Additional Info
            if (page.additionalInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Additional Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = page.additionalInfo,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Posted on ${
                        java.text.SimpleDateFormat(
                            "MMM dd, yyyy", java.util.Locale.getDefault()
                        ).format(java.util.Date(page.createdAt))
                    }",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
