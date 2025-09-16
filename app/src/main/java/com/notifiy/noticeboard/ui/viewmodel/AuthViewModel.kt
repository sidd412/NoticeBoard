package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    
    init {
        checkAuthState()
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
    
    fun skipLogin() {
        _authState.value = UiState(data = null, isLoading = false)
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}
