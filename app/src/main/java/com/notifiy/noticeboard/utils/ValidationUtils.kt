package com.notifiy.noticeboard.utils

object ValidationUtils {
    
    /**
     * Validates if the email format is correct and is a Gmail address
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        
        // More strict Gmail validation
        val gmailRegex = "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?@gmail\\.com$".toRegex()
        return gmailRegex.matches(email.lowercase())
    }
    
    /**
     * Validates if the email format is correct (generic email validation)
     */
    fun isValidEmailGeneric(email: String): Boolean {
        if (email.isBlank()) return false
        
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
    
    /**
     * Validates if the password meets minimum requirements
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Validates if the name is not empty
     */
    fun isValidName(name: String): Boolean {
        return name.trim().isNotEmpty()
    }
    
    /**
     * Validates all signup fields
     */
    fun validateSignupFields(name: String, email: String, password: String): ValidationResult {
        return when {
            !isValidName(name) -> ValidationResult(false, "Name cannot be empty")
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid Gmail address")
            !isValidPassword(password) -> ValidationResult(false, "Password must be at least 6 characters long")
            else -> ValidationResult(true, "")
        }
    }
    
    /**
     * Validates login fields
     */
    fun validateLoginFields(email: String, password: String): ValidationResult {
        return when {
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid Gmail address")
            password.isBlank() -> ValidationResult(false, "Password cannot be empty")
            else -> ValidationResult(true, "")
        }
    }
    
    /**
     * Validates profile update fields
     */
    fun validateProfileUpdateFields(name: String, email: String): ValidationResult {
        return when {
            !isValidName(name) -> ValidationResult(false, "Name cannot be empty")
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid Gmail address")
            else -> ValidationResult(true, "")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)
