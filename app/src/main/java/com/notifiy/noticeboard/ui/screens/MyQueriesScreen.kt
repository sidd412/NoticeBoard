package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.BoardQuery
import com.notifiy.noticeboard.data.model.UserQuery
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQueriesScreen(
    navController: NavController,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    val scope = rememberCoroutineScope()
    
    var createdUserQueries by remember { mutableStateOf<List<UserQuery>>(emptyList()) }
    var resolvedUserQueries by remember { mutableStateOf<List<UserQuery>>(emptyList()) }
    var createdBoardQueries by remember { mutableStateOf<List<BoardQuery>>(emptyList()) }
    var resolvedBoardQueries by remember { mutableStateOf<List<BoardQuery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isUserQueriesSelected by remember { mutableStateOf(true) } // true for User Queries, false for Board Queries
    
    // Function to load queries
    fun loadQueries() {
        if (currentUser != null) {
            isLoading = true
            scope.launch {
                try {
                    android.util.Log.d("MyQueriesScreen", "Loading queries for user: ${currentUser.id}")
                    
                    // Load User Queries
                    val allUserQueries = repository.getUserQueriesByUserId(currentUser.id)
                    android.util.Log.d("MyQueriesScreen", "Retrieved ${allUserQueries.size} total user queries")
                    
                    createdUserQueries = allUserQueries.filter { it.status == "created" }
                    resolvedUserQueries = allUserQueries.filter { it.status == "resolved" }
                    
                    // Load Board Queries
                    val allBoardQueries = repository.getBoardQueriesByUserId(currentUser.id)
                    android.util.Log.d("MyQueriesScreen", "Retrieved ${allBoardQueries.size} total board queries")
                    
                    createdBoardQueries = allBoardQueries.filter { it.status == "created" }
                    resolvedBoardQueries = allBoardQueries.filter { it.status == "resolved" }

                    android.util.Log.d("MyQueriesScreen", "User Queries - Created: ${createdUserQueries.size}, Resolved: ${resolvedUserQueries.size}")
                    android.util.Log.d("MyQueriesScreen", "Board Queries - Created: ${createdBoardQueries.size}, Resolved: ${resolvedBoardQueries.size}")
                } catch (e: Exception) {
                    android.util.Log.e("MyQueriesScreen", "Error loading queries: ${e.message}")
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    // Load queries when screen loads
    LaunchedEffect(currentUser) {
        loadQueries()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "My Queries",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(onClick = { loadQueries() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Toggle Slider for User Queries vs Board Queries
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // User Queries Button
                    Button(
                        onClick = { isUserQueriesSelected = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUserQueriesSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isUserQueriesSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isUserQueriesSelected) 4.dp else 0.dp
                        )
                    ) {
                        Text(
                            text = "User Queries",
                            fontSize = 14.sp,
                            fontWeight = if (isUserQueriesSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    // Board Queries Button
                    Button(
                        onClick = { isUserQueriesSelected = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isUserQueriesSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!isUserQueriesSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (!isUserQueriesSelected) 4.dp else 0.dp
                        )
                    ) {
                        Text(
                            text = "Board Queries",
                            fontSize = 14.sp,
                            fontWeight = if (!isUserQueriesSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        // Debug info (remove in production)
        if (currentUser != null) {
            Text(
                text = "Debug: User ID = ${currentUser.id}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Pending,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Created (${if (isUserQueriesSelected) createdUserQueries.size else createdBoardQueries.size})")
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Resolved (${if (isUserQueriesSelected) resolvedUserQueries.size else resolvedBoardQueries.size})")
                    }
                }
            )
        }
        
        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Show appropriate queries based on toggle selection
            val queriesToShow = if (isUserQueriesSelected) {
                if (selectedTabIndex == 0) createdUserQueries else resolvedUserQueries
            } else {
                if (selectedTabIndex == 0) createdBoardQueries else resolvedBoardQueries
            }
            
            if (queriesToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (selectedTabIndex == 0) Icons.Default.Pending else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (selectedTabIndex == 0) "No pending queries" else "No resolved queries",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (selectedTabIndex == 0) "Your queries will appear here" else "Resolved queries will appear here",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isUserQueriesSelected) {
                        items(queriesToShow as List<UserQuery>) { query ->
                            UserQueryCard(query = query)
                        }
                    } else {
                        items(queriesToShow as List<BoardQuery>) { query ->
                            BoardQueryCard(query = query)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserQueryCard(query: UserQuery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (query.status == "resolved") Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (query.status == "resolved") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Text(
                        text = query.status.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (query.status == "resolved") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(query.createdAt)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Question
            Text(
                text = query.question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Organization Code
            Text(
                text = "Organization: ${query.organisationCode}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // Answer (if resolved)
            if (query.status == "resolved" && query.answer.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Answer:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = query.answer,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardQueryCard(query: BoardQuery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Board Query",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (query.status == "resolved") Color(0xFF4CAF50) else Color(0xFFFF9800),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        text = query.status.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (query.status == "resolved") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
            
            // Type
            Text(
                text = "Type: ${query.type.replaceFirstChar { it.uppercase() }}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Question
            Text(
                text = query.question,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Organization details
            Text(
                text = "Organization: ${query.orgName} (${query.orgCode})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // Answer (if resolved)
            if (query.status == "resolved" && query.answer.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Answer:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = query.answer,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
