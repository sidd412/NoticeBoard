package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.preferences.PreferencesManager
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.ThemeViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.ValidationUtils
import android.widget.Toast
import java.util.concurrent.TimeUnit

fun isSubscriptionActive(subscriptionExpiry: Long): Boolean {
    if (subscriptionExpiry <= 0) return true // 0 means unlimited/no expiry
    return System.currentTimeMillis() < subscriptionExpiry
}

fun getPlanValidityText(subscriptionExpiry: Long, subscriptionPeriod: String = "", planName: String = ""): String {
    // Handle unlimited/no-expiry for paid plans (expiry = 0 means unlimited)
    if (subscriptionExpiry <= 0 && planName.lowercase() != "free") {
        return "No Expiry"
    }
    
    val currentTime = System.currentTimeMillis()
    val timeDiff = subscriptionExpiry - currentTime
    if (timeDiff <= 0) return "Expired"

    val days = TimeUnit.MILLISECONDS.toDays(timeDiff)
    val months = days / 30
    val remainingDays = days % 30

    return when {
        planName.lowercase() == "free" -> {
            // For free plan, show specific format
            when {
                months > 0 && remainingDays > 0 -> "Expires in $months Month $remainingDays Days"
                months > 0 -> "Expires in $months Month"
                days > 0 -> "Expires in $days Days"
                else -> "Expired"
            }
        }
        subscriptionPeriod.lowercase() == "annual" -> "Expires in $days days"
        subscriptionPeriod.lowercase() == "monthly" -> "Expires in $days days"
        months > 0 && remainingDays > 0 -> "$months Month $remainingDays Days"
        months > 0 -> "$months Month"
        days > 0 -> "$days Days"
        else -> "Expired"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java),
    themeViewModel: ThemeViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val profileUpdated by authViewModel.profileUpdated.collectAsState()
    val currentUser = authState.data
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    
    // Reload user data when screen becomes active to get updated subscription info
    LaunchedEffect(Unit) {
        android.util.Log.d("sidxp", "ProfileScreen - Reloading user data to get updated subscription info")
        authViewModel.refreshAuthState()
    }
    
    // Also reload when the screen is recomposed (e.g., when returning from subscription screen)
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            android.util.Log.d("sidxp", "ProfileScreen - User data changed, refreshing to get latest subscription info")
            authViewModel.refreshAuthState()
        }
    }
    
    // Handle profile update success
    LaunchedEffect(profileUpdated) {
        if (profileUpdated) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            authViewModel.clearProfileUpdated()
        }
    }

    if (currentUser != null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Name
                        Column(modifier = Modifier) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentUser.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = "✎ edit",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(onClick = {
                                        showEditProfileDialog = true
                                    })
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // User Email
                            Text(
                                text = currentUser.email,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Account subscription Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Subscription Plan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentUser?.planName?.ifEmpty { "No Plan" } ?: "No Plan",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Text(
                                    text = if (currentUser?.planName?.isNotEmpty() == true) {
                                        getPlanValidityText(currentUser.subscriptionExpiry, currentUser.subscriptionPeriod, currentUser.planName)
                                    } else {
                                        "Not Subscribed"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            if (currentUser != null) {
                                val hasPlan = currentUser.planName.isNotEmpty() || currentUser.currentPlanId.isNotEmpty()
                                if (hasPlan) {
                                    val isActive = isSubscriptionActive(currentUser.subscriptionExpiry)
                                    Text(
                                        text = if (isActive) "✓ Active" else "Expired",
                                        fontSize = 14.sp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Not Subscribed",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not Subscribed",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Account & Settings Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Account & Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Privacy & Notifications
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate(Screen.PrivacySettings.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Privacy",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Privacy & Notifications",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Theme Mode Dropdown
                        ThemeModeDropdown(themeViewModel = themeViewModel)

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Your Orders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate(Screen.Orders.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Orders",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Your Orders",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Help & Support
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate(Screen.HelpSupport.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Help & Support",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // My Queries
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate(Screen.MyQueries.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "My Queries",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "My Queries",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate(Screen.About.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "About NoteXP",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Sign Out Button
                        if (currentUser != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { showSignOutDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Sign Out",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }


            // Empty space for bottom padding
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(onDismissRequest = { showSignOutDialog = false }, title = {
                Text(
                    text = "Sign Out", fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
            }, text = {
                Text(
                    text = "Are you sure you want to sign out? You will need to sign in again to access your account.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }, confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.MainContainer.route) { inclusive = true }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Yes, Sign Out", fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
            }, dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            })
        }
    } else {
        LoginScreen(
            navController, authViewModel
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onUpdateProfile = { name, email ->
                authViewModel.updateUserProfile(name, email)
                showEditProfileDialog = false
            },
            isLoading = authState.isLoading
        )
    }
}

@Composable
fun ThemeModeDropdown(themeViewModel: ThemeViewModel) {
    var expanded by remember { mutableStateOf(false) }

    val themeOptions = listOf(
        Triple("System", PreferencesManager.THEME_SYSTEM, Icons.Default.SettingsBrightness),
        Triple("Light", PreferencesManager.THEME_LIGHT, Icons.Default.LightMode),
        Triple("Dark", PreferencesManager.THEME_DARK, Icons.Default.DarkMode)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SettingsBrightness,
            contentDescription = "Theme",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Theme Mode",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Box {
            Row(modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = themeViewModel.getThemeModeString(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                themeOptions.forEach { (label, value, icon) ->
                    DropdownMenuItem(text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (themeViewModel.themeMode == value) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }, onClick = {
                        themeViewModel.setThemeMode(value)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentUser: com.notifiy.noticeboard.data.model.User?,
    onDismiss: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    isLoading: Boolean
) {
    val originalName = currentUser?.name ?: ""
    val originalEmail = currentUser?.email ?: ""

    var name by remember { mutableStateOf(originalName) }
    var email by remember { mutableStateOf(originalEmail) }
    var validationError by remember { mutableStateOf("") }

    // Check if any changes have been made
    val hasChanges = name != originalName || email != originalEmail

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Profile") }, text = {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = validationError.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                isError = validationError.isNotEmpty()
            )

            if (validationError.isNotEmpty()) {
                Text(
                    text = validationError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }, confirmButton = {
        Button(
            onClick = {
                val validation = ValidationUtils.validateProfileUpdateFields(name, email)
                if (!validation.isValid) {
                    validationError = validation.errorMessage
                    return@Button
                }
                onUpdateProfile(name, email)
            }, enabled = !isLoading && hasChanges
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), color = Color.White
                )
            } else {
                Text("Update")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}
