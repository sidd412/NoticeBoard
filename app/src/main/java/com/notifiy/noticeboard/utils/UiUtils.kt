package com.notifiy.noticeboard.utils

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@Composable
fun ShowErrorSnackbar(
    error: String?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit = {}
) {
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            onErrorShown()
        }
    }
}

fun getErrorMessage(exception: Throwable): String {
    return when {
        exception.message?.contains("email-already-in-use") == true -> 
            "This email is already registered. Please try signing in instead."
        exception.message?.contains("weak-password") == true -> 
            "Password is too weak. Please choose a stronger password."
        exception.message?.contains("invalid-email") == true -> 
            "Please enter a valid email address."
        exception.message?.contains("user-not-found") == true -> 
            "No account found with this email. Please sign up first."
        exception.message?.contains("wrong-password") == true -> 
            "Incorrect password. Please try again."
        exception.message?.contains("network-request-failed") == true -> 
            "Network error. Please check your internet connection."
        exception.message?.contains("too-many-requests") == true -> 
            "Too many attempts. Please try again later."
        else -> exception.message ?: "An unexpected error occurred. Please try again."
    }
}

/**
 * Validates if a phone number is exactly 10 digits
 * @param phoneNumber The phone number to validate
 * @return true if the phone number is exactly 10 digits, false otherwise
 */
fun isValidPhoneNumber(phoneNumber: String): Boolean {
    return phoneNumber.matches(Regex("^\\d{10}$"))
}


