package com.notifiy.noticeboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notifiy.noticeboard.data.model.User
import com.notifiy.noticeboard.data.model.UserQuery
import com.notifiy.noticeboard.data.repository.FirebaseRepository
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
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                QueryFormContent(
                    currentUser = currentUser,
                    organisationCode = organisationCode,
                    onDismiss = onDismiss,
                    onQuerySubmitted = onQuerySubmitted
                )
            }
        }
    }
}

@Composable
private fun QueryFormContent(
    currentUser: User?,
    organisationCode: String,
    onDismiss: () -> Unit,
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            
            // Location Field (Optional)
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
                placeholder = { Text("City, State") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        ) {
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
}
