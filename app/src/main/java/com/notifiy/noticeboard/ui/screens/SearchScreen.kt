package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.notifiy.noticeboard.utils.LocationManager
import com.notifiy.noticeboard.utils.hasLocationPermission
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.HomeViewModel
import com.notifiy.noticeboard.ui.viewmodel.SearchViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = cachedViewModel(SearchViewModel::class.java),
    homeViewModel: HomeViewModel = cachedViewModel(HomeViewModel::class.java),
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val errorMessage by searchViewModel.errorMessage.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.data
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        // Only show toast for permission denial, success will trigger location fetch
        if (!allGranted) {
            Toast.makeText(context, "Location permission denied. You can still search manually.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val locationManager = remember { LocationManager(context) }
    
    // Handle location fetching when permission is granted
    LaunchedEffect(isLoadingLocation) {
        if (isLoadingLocation && context.hasLocationPermission()) {
            android.util.Log.d("SearchScreen", "Starting location-based search")
            try {
                val locationData = locationManager.getCurrentLocation()
                android.util.Log.d("SearchScreen", "Location data: $locationData")
                locationData?.let { data ->
                    // Extract city name from the full address for more precise search
                    val cityName = data.city
                    android.util.Log.d("SearchScreen", "Searching for city: $cityName")
                    searchQuery = cityName
                    searchViewModel.searchNoticeBoards(cityName)
                    // Search results will show automatically, no need for toast here
                } ?: run {
                    android.util.Log.d("SearchScreen", "No location data returned")
                    Toast.makeText(context, "Could not detect location. Please search manually.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchScreen", "Error getting location", e)
                Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingLocation = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Notice Boards") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar with Location Button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    searchViewModel.searchNoticeBoards(it)
                },
                label = { Text("Search by city / code / name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            android.util.Log.d("SearchScreen", "Location button clicked in search")
                            
                            if (context.hasLocationPermission()) {
                                android.util.Log.d("SearchScreen", "Permission granted, getting location for search")
                                isLoadingLocation = true
                            } else {
                                android.util.Log.d("SearchScreen", "Location permission needed for search")
                                // Request permission using the launcher
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isLoadingLocation
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Search by Current Location",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Results
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search Notice Boards",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter city name, organization code, or organization name to find notice boards\nOr click the location icon to search boards near you",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Results Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try searching with different keywords",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                 LazyColumn(
                     verticalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     items(searchResults) { board ->
                         val authState by authViewModel.authState.collectAsState()
                         val currentUser = authState.data
                         val isSubscribed = currentUser?.subscribedCodes?.contains(board.organizationCode) ?: false
                         
                         SearchResultCard(
                             board = board,
                             onClick = {
                                 navController.navigate(Screen.NoticeViewer.createRoute(board.id))
                             },
                             onSubscribe = {
                                 currentUser?.let { user ->
                                     homeViewModel.subscribeToBoard(
                                         userId = user.id,
                                         instituteCode = board.organizationCode
                                     ) { result ->
                                         if (result.isSuccess) {
                                             // Refresh auth state to update UI
                                             authViewModel.refreshAuthState()
                                             Toast.makeText(
                                                 context,
                                                 "Successfully subscribed to ${board.organizationName}",
                                                 Toast.LENGTH_SHORT
                                             ).show()
                                         } else {
                                             Toast.makeText(
                                                 context,
                                                 "Failed to subscribe: ${result.exceptionOrNull()?.message}",
                                                 Toast.LENGTH_SHORT
                                             ).show()
                                         }
                                     }
                                 } ?: run {
                                     Toast.makeText(
                                         context,
                                         "Please log in to subscribe",
                                         Toast.LENGTH_SHORT
                                     ).show()
                                 }
                             },
                             onUnsubscribe = {
                                 currentUser?.let { user ->
                                     homeViewModel.unsubscribeFromBoard(
                                         userId = user.id,
                                         instituteCode = board.organizationCode
                                     ) { result ->
                                         if (result.isSuccess) {
                                             // Refresh auth state to update UI
                                             authViewModel.refreshAuthState()
                                             Toast.makeText(
                                                 context,
                                                 "Successfully unsubscribed from ${board.organizationName}",
                                                 Toast.LENGTH_SHORT
                                             ).show()
                                         } else {
                                             Toast.makeText(
                                                 context,
                                                 "Failed to unsubscribe: ${result.exceptionOrNull()?.message}",
                                                 Toast.LENGTH_SHORT
                                             ).show()
                                         }
                                     }
                                 } ?: run {
                                     Toast.makeText(
                                         context,
                                         "Please log in to unsubscribe",
                                         Toast.LENGTH_SHORT
                                     ).show()
                                 }
                             },
                             isSubscribed = isSubscribed
                         )
                     }
                 }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    board: NoticeBoard,
    onClick: () -> Unit,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    isSubscribed: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Board Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = board.organizationName.take(2).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Board Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
                Text(
                    text = board.organizationName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = board.organizationLocation,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Subscribe/Unsubscribe Button
            Button(
                onClick = if (isSubscribed) onUnsubscribe else onSubscribe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubscribed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isSubscribed) "Unsubscribe" else "Subscribe",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
