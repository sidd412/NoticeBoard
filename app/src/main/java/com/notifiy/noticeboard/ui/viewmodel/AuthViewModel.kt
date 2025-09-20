package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.notifiy.noticeboard.R
import com.notifiy.noticeboard.data.model.User
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val context: Context) : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository(context)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow(UiState<User?>())
    val authState: StateFlow<UiState<User?>> = _authState.asStateFlow()
    
    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()
    
    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()
    
    private val _profileUpdated = MutableStateFlow(false)
    val profileUpdated: StateFlow<Boolean> = _profileUpdated.asStateFlow()
    
    // Google Sign-In client
    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    init {
        checkAuthState()
    }
    
    // Debug function to log auth state changes
    private fun logAuthState(state: UiState<User?>) {
        println("accountdeletion: AuthViewModel - Auth state changed: isLoading=${state.isLoading}, hasData=${state.data != null}, error=${state.error}")
    }
    
    private fun checkAuthState() {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val user = repository.getCurrentUser()
                _authState.value = UiState(data = user, isLoading = false)
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun refreshAuthState() {
        checkAuthState()
    }
    
    fun signInWithEmail(email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = repository.getCurrentUser()
                _authState.value = UiState(data = user, isLoading = false)
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String, name: String) {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = name,
                        email = email,
                        phoneNumber = "",
                        profileImageUrl = "",
                        subscribedBoards = emptyList(),
                        subscribedCodes = emptyList(),
                        instituteCodes = emptyList(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    repository.createUser(user)
                    _authState.value = UiState(data = user, isLoading = false)
                } else {
                    _authState.value = UiState(isLoading = false, error = "Failed to create user")
                }
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = UiState(data = null, isLoading = false)
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun deleteAccount() {
        println("accountdeletion: Starting account deletion process")
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    println("accountdeletion: Current user found: ${currentUser.uid}")
                    
                    // Delete user data from Firestore first
                    println("accountdeletion: Deleting user data from Firestore")
                    val deleteResult = repository.deleteUser(currentUser.uid)
                    if (deleteResult.isSuccess) {
                        println("accountdeletion: User data deleted from Firestore successfully")
                    } else {
                        println("accountdeletion: Failed to delete user data from Firestore: ${deleteResult.exceptionOrNull()?.message}")
                    }
                    
                    // Delete the Firebase Auth account
                    println("accountdeletion: Deleting Firebase Auth account")
                    currentUser.delete().await()
                    println("accountdeletion: Firebase Auth account deleted successfully")
                    
                    // Clear all cache
                    println("accountdeletion: Clearing all cache")
                    val cacheResult = repository.clearAllCache()
                    if (cacheResult.isSuccess) {
                        println("accountdeletion: All cache cleared successfully")
                    } else {
                        println("accountdeletion: Failed to clear cache: ${cacheResult.exceptionOrNull()?.message}")
                    }
                    
                    // Clear the auth state and set success flag
                    println("accountdeletion: Setting success state")
                    _authState.value = UiState(data = null, isLoading = false)
                    logAuthState(_authState.value)
                    _accountDeleted.value = true
                    println("accountdeletion: Account deletion completed successfully")
                } else {
                    println("accountdeletion: No user logged in")
                    _authState.value = UiState(isLoading = false, error = "No user logged in")
                }
            } catch (e: Exception) {
                println("accountdeletion: Error during account deletion: ${e.message}")
                println("accountdeletion: Exception type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun skipLogin() {
        _authState.value = UiState(data = null, isLoading = false)
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun clearPasswordResetSent() {
        _passwordResetSent.value = false
    }
    
    fun clearProfileUpdated() {
        _profileUpdated.value = false
    }
    
    fun sendPasswordResetEmail(email: String) {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _passwordResetSent.value = true
                _authState.value = _authState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Check if user exists in Firestore
                    val existingUser = repository.getCurrentUser()
                    if (existingUser == null) {
                        // Create new user if doesn't exist
                        val user = User(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "User",
                            email = firebaseUser.email ?: "",
                            phoneNumber = "",
                            profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                            subscribedBoards = emptyList(),
                            subscribedCodes = emptyList(),
                            instituteCodes = emptyList(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.createUser(user)
                        _authState.value = UiState(data = user, isLoading = false)
                    } else {
                        _authState.value = UiState(data = existingUser, isLoading = false)
                    }
                } else {
                    _authState.value = UiState(isLoading = false, error = "Failed to sign in with Google")
                }
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
    
    fun updateUserProfile(name: String, email: String) {
        _authState.value = _authState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Update Firebase Auth profile
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    currentUser.updateProfile(profileUpdates).await()
                    
                    // Update email if changed
                    if (email != currentUser.email) {
                        currentUser.updateEmail(email).await()
                    }
                    
                    // Update user in Firestore
                    val existingUser = repository.getCurrentUser()
                    if (existingUser != null) {
                        val updatedUser = existingUser.copy(
                            name = name,
                            email = email,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateUser(updatedUser)
                        _authState.value = UiState(data = updatedUser, isLoading = false)
                        _profileUpdated.value = true
                    } else {
                        _authState.value = UiState(isLoading = false, error = "User not found")
                    }
                } else {
                    _authState.value = UiState(isLoading = false, error = "No user logged in")
                }
            } catch (e: Exception) {
                _authState.value = UiState(isLoading = false, error = e.message)
            }
        }
    }
}
