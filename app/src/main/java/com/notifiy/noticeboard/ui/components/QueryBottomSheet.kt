package com.notifiy.noticeboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifiy.noticeboard.data.model.User
import com.notifiy.noticeboard.data.model.UserQuery
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.LocationManager
import com.notifiy.noticeboard.utils.hasLocationPermission
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    currentUser: User?,
    organisationCode: String,
    onQuerySubmitted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    val scope = rememberCoroutineScope()
    
    var question by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var mobile by remember { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var location by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    
    val locationManager = remember { LocationManager(context) }
    
    // Function to detect current location using the same LocationManager as board creation
    val handleGetCurrentLocation = {
        android.util.Log.d("QueryBottomSheet", "Current location button clicked")
        
        if (context.hasLocationPermission()) {
            android.util.Log.d("QueryBottomSheet", "Permission already granted, getting location")
            isDetectingLocation = true
        } else {
            android.util.Log.d("QueryBottomSheet", "No permission, showing manual entry message")
            android.widget.Toast.makeText(context, "Location permission not granted. Please enter location manually.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Handle location fetching when permission is granted
    LaunchedEffect(isDetectingLocation) {
        if (isDetectingLocation && context.hasLocationPermission()) {
            android.util.Log.d("QueryBottomSheet", "Starting location fetch")
            try {
                val locationData = locationManager.getCurrentLocation()
                android.util.Log.d("QueryBottomSheet", "Location data: $locationData")
                locationData?.let { data ->
                    location = data.fullAddress
                    android.util.Log.d("QueryBottomSheet", "Location filled: ${data.fullAddress}")
                } ?: run {
                    android.util.Log.d("QueryBottomSheet", "No location data returned")
                    android.widget.Toast.makeText(context, "Could not detect location. Please enter manually.", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("QueryBottomSheet", "Error getting location", e)
                android.widget.Toast.makeText(context, "Error getting location: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isDetectingLocation = false
            }
        }
    }
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Expand the sheet to full height when it opens
    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.expand()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(.9f)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raise a Query",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Question Field (Required)
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Your Question *") },
                        placeholder = { Text("Describe your query or issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        isError = question.isBlank(),
                        supportingText = if (question.isBlank()) {
                            { Text("Question is required", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    
                    // Name Field (Required if not available from user)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = name.isBlank(),
                        supportingText = if (name.isBlank()) {
                            { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    
                    // Email Field (Required if not available from user)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                        supportingText = when {
                            email.isBlank() -> { { Text("Email is required", color = MaterialTheme.colorScheme.error) } }
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { 
                                { Text("Please enter a valid email", color = MaterialTheme.colorScheme.error) } 
                            }
                            else -> null
                        }
                    )
                    
                    // Mobile Field (Required if not available from user)
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = mobile.isBlank(),
                        supportingText = if (mobile.isBlank()) {
                            { Text("Mobile number is required", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                    
                    // Location Field (Optional) with Location Detection Button
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Optional)") },
                        placeholder = { Text("City, State") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location Icon")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = handleGetCurrentLocation,
                                enabled = !isDetectingLocation
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Get Current Location",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    // Submit Button
                    Button(
                        onClick = {
                            if (question.isNotBlank() && name.isNotBlank() && email.isNotBlank() &&
                                mobile.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                                isLoading = true
                                scope.launch {
                                    try {
                                        val userQuery = UserQuery(
                                            question = question,
                                            organisationCode = organisationCode,
                                            raiserEmail = email,
                                            raiserLocation = location,
                                            raiserMobile = mobile,
                                            raiserName = name,
                                            status = "created"
                                        )

                                        val result = repository.createUserQuery(userQuery)
                                        if (result.isSuccess) {
                                            android.widget.Toast.makeText(context, "Query raised successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            onQuerySubmitted()
                                            onDismiss()
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to submit query. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && question.isNotBlank() && name.isNotBlank() &&
                                email.isNotBlank() && mobile.isNotBlank() &&
                                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    )
                    {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Query", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                

                
                // Add bottom padding for better UX
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
