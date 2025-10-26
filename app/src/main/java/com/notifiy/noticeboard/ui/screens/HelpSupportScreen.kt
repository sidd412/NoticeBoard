package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.popBackStack()
                    }) {
                    Icon(
                        Icons.Default.ArrowBack, contentDescription = "Back"
                    )
                }
                Text(
                    text = "Help & Support",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 30.dp)
                )
                Icon(
                    Icons.Outlined.QuestionMark,
                    contentDescription = "Back",
                    modifier = Modifier.size(0.dp)
                )
            }
        }

        // Help Overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Help",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "We're Here to Help",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Find answers to common questions or get in touch with our support team.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Frequently Asked Questions
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "FAQ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Frequently Asked Questions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val faqs: List<Triple<String, String, String>> = listOf(
                        Triple(
                            "How do I create a notice board?",
                            "Go to 'My Boards' tab and tap 'Create New Board'. Fill in your institute details including name, code, email, location, and WhatsApp number. Your board will be ready to use immediately.",
                            ""
                        ), Triple(
                            "How do I subscribe to a notice board?",
                            "You can subscribe by scanning the QR code, entering the board code manually, using the WhatsApp number, or email address. Look for the subscribe option on the board details page.",
                            ""
                        ), Triple(
                            "Why am I not receiving notifications?",
                            "Check your notification settings in 'Privacy & Notifications'. Make sure push notifications are enabled and check your device's notification settings. Also ensure you're subscribed to the notice board.",
                            ""
                        ), Triple(
                            "How do I change my theme?",
                            "Go to your Profile and tap on 'Theme Mode'. You can choose between System, Light, or Dark theme. The app will remember your preference.",
                            ""
                        ), Triple(
                            "Can I delete my account?",
                            "Yes, you can delete your account and all associated data by going to 'Privacy & Notifications' and selecting 'Delete My Account & Data'. This action is permanent and cannot be undone.",
                            ""
                        ), Triple(
                            "How do I share a notice?",
                            "Open any notice and look for the share button. You can share via social media, email, or other apps installed on your device. The shared content will include the notice details.",
                            ""
                        ), Triple(
                            "Is my data secure?",
                            "Yes, we use industry-standard encryption and security measures. Your data is never shared with third parties without your consent. We comply with GDPR and other privacy regulations.",
                            ""
                        ), Triple(
                            "How do I update my profile?",
                            "Go to your Profile tab and tap on your profile information. You can update your name and email address. Changes are saved automatically.",
                            ""
                        )
                    )

                    faqs.forEach { (question, answer, _) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = question,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = answer,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            if (question != faqs.last().first) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Contact & Support
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = "Support",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Contact & Support",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Support
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data =
                                        Uri.parse("mailto:siddharthaverma6213@gmail.com?subject=Support for NoteXP needed&body=Please describe your issue or question here...")
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        emailIntent, "Send Email"
                                    )
                                )
                            }, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Email Support",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "siddharthaverma6213@gmail.com",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Chat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                Toast.makeText(
                                    context,
                                    "Live chat support will come soon, not implemented yet",
                                    Toast.LENGTH_LONG
                                ).show()
                            }, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Chat Support",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available 24/7 in the app",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Support
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:+917080986116")
                                }
                                context.startActivity(phoneIntent)
                            }, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Phone Support",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "+91 7080986116",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Available for calls and WhatsApp",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Our support team typically responds within 48 hours. For urgent issues, please use live chat or phone support.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Quick Actions
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Support Button
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data =
                                    Uri.parse("mailto:siddharthaverma6213@gmail.com?subject=Support for NoteXP needed&body=Please describe your issue or question here...")
                            }
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Support Email",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Report Bug Button
                    OutlinedButton(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data =
                                    Uri.parse("mailto:siddharthaverma6213@gmail.com?subject=A bug is reported in NoteXP&body=Please describe the bug you encountered:\n\nSteps to reproduce:\n1. \n2. \n3. \n\nExpected behavior:\n\nActual behavior:\n\nDevice info:\n- Model: \n- OS Version: \n- App Version: ")
                            }
                            context.startActivity(Intent.createChooser(emailIntent, "Report Bug"))
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = "Bug Report",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report a Bug", fontSize = 16.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
