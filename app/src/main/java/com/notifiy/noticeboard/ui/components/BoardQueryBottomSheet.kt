package com.notifiy.noticeboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifiy.noticeboard.data.model.BoardQuery
import com.notifiy.noticeboard.data.model.User
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardQueryBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    currentUser: User?,
    boardOrgCode: String,
    boardOrgName: String,
    boardOrgEmail: String,
    boardOrgMobile: String,
    onQuerySubmitted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    val scope = rememberCoroutineScope()

    var question by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Query types for dropdown
    val queryTypes = listOf(
        "verification", "subscribers data"
    )

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
            onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.5f)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Board Query",
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

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Type Field (Required) - Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Query Type *") },
                            placeholder = { Text("Select query type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = type.isBlank(),
                            supportingText = if (type.isBlank()) {
                                {
                                    Text(
                                        "Query type is required",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null)
                        ExposedDropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false }) {
                            queryTypes.forEach { queryType ->
                                DropdownMenuItem(text = { Text(queryType) }, onClick = {
                                    type = queryType
                                    expanded = false
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question Field (Required)
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Your Query (Optional)") },
                        placeholder = { Text("Describe your query...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (type.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val boardQuery = BoardQuery(
                                            question = question,
                                            orgCode = boardOrgCode,
                                            orgEmail = boardOrgEmail,
                                            orgMobile = boardOrgMobile,
                                            orgName = boardOrgName,
                                            type = type
                                        )

                                        val result = repository.createBoardQuery(boardQuery)
                                        if (result.isSuccess) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Board query submitted successfully!",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            onQuerySubmitted()
                                            onDismiss()
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to submit query: ${result.exceptionOrNull()?.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Please fill all required fields correctly",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isLoading) "Submitting..." else "Send Query",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
