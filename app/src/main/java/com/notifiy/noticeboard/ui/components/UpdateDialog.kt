package com.notifiy.noticeboard.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notifiy.noticeboard.data.model.UpdateConfig

@Composable
fun UpdateDialog(
    updateConfig: UpdateConfig,
    isForceUpdate: Boolean,
    isSkipable: Boolean,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = if (isForceUpdate) { {} } else { onDismiss },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Update Icon
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = "NoteXP Update Available",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Title
                Text(
                    text = if (isForceUpdate) "Update Required" else "New Version Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = if (isForceUpdate) {
                        "A new version of NoteXP is available and must be installed to continue using the app."
                    } else {
                        "A new version of NoteXP is available with new features, improvements, and bug fixes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Version Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = "Version Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Version ${updateConfig.latestVersionName} (Build ${updateConfig.latestVersionCode})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isForceUpdate && isSkipable) {
                        // Skip Button (only for skipable updates)
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Skip for Now")
                        }
                    }
                    
                    // Update Button
                    Button(
                        onClick = {
                            onUpdate()
                            // Open Play Store with user-friendly error handling
                            try {
                                if (updateConfig.updateLink.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateConfig.updateLink))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } else {
                                    showError = true
                                    errorMessage = "Update link is not available. Please check your internet connection and try again."
                                }
                            } catch (e: Exception) {
                                // Fallback to browser if Play Store app is not available
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateConfig.updateLink))
                                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(browserIntent)
                                } catch (e2: Exception) {
                                    // Show user-friendly error message
                                    showError = true
                                    errorMessage = "Unable to open Play Store. Please manually search for 'NoteXP' in Google Play Store to update the app."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Update Now",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Error message display
                if (showError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { showError = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                if (isForceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This update is required to continue using NoteXP",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
