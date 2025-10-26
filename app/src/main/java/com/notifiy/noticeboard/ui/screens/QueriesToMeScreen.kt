package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.UserQuery
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueriesToMeScreen(
    navController: NavController,
    orgCode: String
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    val scope = rememberCoroutineScope()
    
    var createdQueries by remember { mutableStateOf<List<UserQuery>>(emptyList()) }
    var resolvedQueries by remember { mutableStateOf<List<UserQuery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Function to load queries
    fun loadQueries() {
        isLoading = true
        scope.launch {
            try {
                android.util.Log.d("QueriesToMeScreen", "Loading queries for orgCode: $orgCode")
                val allQueries = repository.getQueriesByOrganizationCode(orgCode)
                android.util.Log.d("QueriesToMeScreen", "Retrieved ${allQueries.size} total queries")
                
                createdQueries = allQueries.filter { it.status == "created" }
                resolvedQueries = allQueries.filter { it.status == "resolved" }
                
                android.util.Log.d("QueriesToMeScreen", "Created queries: ${createdQueries.size}, Resolved queries: ${resolvedQueries.size}")
            } catch (e: Exception) {
                android.util.Log.e("QueriesToMeScreen", "Error loading queries: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Load queries when screen loads
    LaunchedEffect(orgCode) {
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
                .padding(start = 16.dp, end = 16.dp, top = 35.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Queries to Me",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
//            }
            IconButton(onClick = { loadQueries() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
//        // Debug info (remove in production)
//        Text(
//            text = "Debug: Org Code = $orgCode",
//            fontSize = 12.sp,
//            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
//            modifier = Modifier.padding(horizontal = 16.dp)
//        )
        
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
                        Text("Created (${createdQueries.size})")
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
                        Text("Resolved (${resolvedQueries.size})")
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
            val queriesToShow = if (selectedTabIndex == 0) createdQueries else resolvedQueries
            
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
                            text = if (selectedTabIndex == 0) "Queries will appear here" else "Resolved queries will appear here",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 60.dp, top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(queriesToShow) { query ->
                        QueryToMeCard(
                            query = query,
                            isCreatedTab = selectedTabIndex == 0,
                            onResolve = { queryId, answer ->
                                scope.launch {
                                    try {
                                        val result = repository.updateUserQueryAnswer(queryId, answer)
                                        if (result.isSuccess) {
                                            android.widget.Toast.makeText(context, "Query resolved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            loadQueries() // Refresh the list
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to resolve query. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryToMeCard(
    query: UserQuery,
    isCreatedTab: Boolean,
    onResolve: (String, String) -> Unit
) {
    var answerText by remember { mutableStateOf("") }
    var showAnswerField by remember { mutableStateOf(false) }
    
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
            
            // Query raiser info
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
                        text = "From: ${query.raiserName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Email: ${query.raiserEmail}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (query.raiserMobile.isNotBlank()) {
                        Text(
                            text = "Mobile: ${query.raiserMobile}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (query.raiserLocation.isNotBlank()) {
                        Text(
                            text = "Location: ${query.raiserLocation}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Answer section (only for created tab)
            if (isCreatedTab) {
                if (!showAnswerField) {
                    Button(
                        onClick = { showAnswerField = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Answer Query")
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = { answerText = it },
                            label = { Text("Your Answer") },
                            placeholder = { Text("Write your answer here...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    showAnswerField = false
                                    answerText = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = { 
                                    if (answerText.isNotBlank()) {
                                        onResolve(query.id, answerText)
                                        showAnswerField = false
                                        answerText = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = answerText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resolve")
                            }
                        }
                    }
                }
            } else {
                // Show answer for resolved queries
                if (query.answer.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Your Answer:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
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
}
