package com.notifiy.noticeboard.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.notifiy.noticeboard.navigation.Screen
import com.notifiy.noticeboard.ui.viewmodel.AuthViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel
import com.notifiy.noticeboard.utils.ShowErrorSnackbar
import com.notifiy.noticeboard.utils.ValidationUtils
import com.notifiy.noticeboard.utils.getErrorMessage

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = cachedViewModel(AuthViewModel::class.java)
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    
    val authState by authViewModel.authState.collectAsState()
    val passwordResetSent by authViewModel.passwordResetSent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                10 -> validationError = "Google Sign-In configuration error. Please contact support."
                12501 -> {
                    // User cancelled - don't show error message
                    // This is normal user behavior
                }
                7 -> validationError = "Network error. Please check your internet connection."
                8 -> validationError = "Internal error. Please try again later."
                else -> validationError = "Google Sign-In failed. Please try again."
            }
        }
    }
    
    // Handle successful authentication
    LaunchedEffect(authState.data, authState.isLoading) {
        if (!authState.isLoading && authState.data != null) {
            navController.navigate(Screen.MainContainer.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    // Show error messages
    ShowErrorSnackbar(
        error = authState.error?.let { getErrorMessage(Exception(it)) },
        snackbarHostState = snackbarHostState,
        onErrorShown = { authViewModel.clearError() }
    )
    
    // Handle validation errors
    LaunchedEffect(validationError) {
        if (validationError.isNotEmpty()) {
            snackbarHostState.showSnackbar(validationError)
            validationError = ""
        }
    }
    
    // Handle password reset success
    LaunchedEffect(passwordResetSent) {
        if (passwordResetSent) {
            snackbarHostState.showSnackbar("Password reset email sent! Check your inbox and spam folder.")
            showPasswordResetDialog = false
            authViewModel.clearPasswordResetSent()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NB",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isSignUp) "Sign up to get started" else "Sign in to continue",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Name field (only for sign up)
            if (isSignUp) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mobile Number field (only for sign up)
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { 
                        // Limit to exactly 10 digits and only allow numbers
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.length <= 10) {
                            mobileNumber = filtered
                        }
                    },
                    label = { Text("Mobile Number (10 digits)") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text("9876543210") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign in/up button
            Button(
                onClick = {
                    val validation = if (isSignUp) {
                        ValidationUtils.validateSignupFields(name, email, password, mobileNumber)
                    } else {
                        ValidationUtils.validateLoginFields(email, password)
                    }
                    
                    if (!validation.isValid) {
                        validationError = validation.errorMessage
                        return@Button
                    }
                    
                    if (isSignUp) {
                        authViewModel.signUpWithEmail(email, password, name, mobileNumber)
                    } else {
                        authViewModel.signInWithEmail(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle sign up/sign in
            TextButton(
                onClick = { 
                    isSignUp = !isSignUp
                    authViewModel.clearError()
                    // Clear form fields when switching
                    if (isSignUp) {
                        name = ""
                        mobileNumber = ""
                    }
                    email = ""
                    password = ""
                    validationError = ""
                }
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Google Sign-In button
            OutlinedButton(
                onClick = {
                    val signInIntent = authViewModel.googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading
            ) {
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password reset button (only for sign in)
            if (!isSignUp) {
                TextButton(
                    onClick = { showPasswordResetDialog = true }
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Password Reset Dialog
    if (showPasswordResetDialog) {
        PasswordResetDialog(
            onDismiss = { showPasswordResetDialog = false },
            onSendReset = { email ->
                authViewModel.sendPasswordResetEmail(email)
            },
            isLoading = authState.isLoading
        )
    }
}

@Composable
fun PasswordResetDialog(
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit,
    isLoading: Boolean
) {
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("Enter your email address and we'll send you a link to reset your password.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Tip: Check your spam/junk folder if you don't receive the email within a few minutes.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!ValidationUtils.isValidEmailGeneric(email)) {
                        validationError = "Please enter a valid email address"
                        return@Button
                    }
                    onSendReset(email)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("Send Reset Link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
