package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Plan
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val data: com.notifiy.noticeboard.data.model.User? = null,
    val plans: List<Plan> = emptyList(),
    val selectedPlan: Plan? = null,
    val error: String? = null
)

class SubscriptionViewModel(
    private val context: Context,
    private val repository: FirebaseRepository = FirebaseRepository(context)
) : ViewModel() {
    
    private val _boardState = MutableStateFlow(SubscriptionUiState())
    val boardState: StateFlow<SubscriptionUiState> = _boardState.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadUserDetails() {
        viewModelScope.launch {
            try {
                _boardState.value = _boardState.value.copy(isLoading = true, error = null)
                
                val currentUser = repository.getCurrentUser()
                val plans = repository.getAllPlans()
                
                // Find current user's plan and preselect it
                val currentPlan = currentUser?.let { user ->
                    plans.find { plan ->
                        plan.planName.equals(user.planName, ignoreCase = true) ||
                        plan.planId.contains(user.currentPlanId) ||
                        plan.id == user.currentPlanId
                    }
                }
                
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = currentUser,
                    plans = plans,
                    selectedPlan = currentPlan ?: plans.firstOrNull(),
                    error = null
                )
                
            } catch (e: Exception) {
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _errorMessage.value = e.message
            }
        }
    }
    
    fun selectPlan(plan: Plan) {
        _boardState.value = _boardState.value.copy(selectedPlan = plan)
    }
    
    fun subscribeToPlan(plan: Plan, isMonthly: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("sidxp", "SubscriptionViewModel - Starting subscription process for plan: ${plan.planName}, isMonthly: $isMonthly")
                
                // Get current user ID first
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    android.util.Log.d("sidxp", "SubscriptionViewModel - No authenticated user found")
                    _errorMessage.value = "User not authenticated"
                    onResult(false)
                    return@launch
                }
                
                android.util.Log.d("sidxp", "SubscriptionViewModel - Current user ID: $userId")
                
                // Get the plan ID for this subscription
                val planId = if (isMonthly && plan.planId.isNotEmpty()) {
                    plan.planId[0] // First element is monthly plan ID
                } else if (!isMonthly && plan.planId.size > 1) {
                    plan.planId[1] // Second element is annual plan ID
                } else {
                    plan.id // Fallback to plan document ID
                }
                
                android.util.Log.d("sidxp", "SubscriptionViewModel - Updating user subscription with planId: '$planId', planName: '${plan.planName}'")
                
                // Calculate proper expiry date based on subscription period
                val calendar = Calendar.getInstance()
                if (isMonthly) {
                    calendar.add(Calendar.MONTH, 1) // Add 1 month
                } else {
                    calendar.add(Calendar.YEAR, 1) // Add 1 year
                }
                val expiryTime = calendar.timeInMillis
                
                val result = repository.updateUserSubscription(
                    userId = userId,
                    subscriptionExpiry = expiryTime,
                    currentPlanId = planId,
                    subscriptionPeriod = if (isMonthly) "monthly" else "annual",
                    planName = plan.planName
                )
                
                result.fold(
                    onSuccess = {
                        android.util.Log.d("sidxp", "SubscriptionViewModel - User subscription updated successfully")
                        _errorMessage.value = null
                        // Reload user details to reflect the updated subscription
                        loadUserDetails()
                        onResult(true)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("sidxp", "SubscriptionViewModel - updateUserSubscription failed: ${exception.message}")
                        exception.printStackTrace()
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("sidxp", "SubscriptionViewModel - Exception in subscribeToPlan: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    
    private suspend fun getCurrentUserId(): String {
        val user = repository.getCurrentUser()
        println("DEBUG: SubscriptionViewModel - getCurrentUserId: ${user?.id}")
        return user?.id ?: ""
    }
    
    fun subscribeToFreePlan(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Get current user ID first
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    _errorMessage.value = "User not authenticated"
                    onResult(false)
                    return@launch
                }
                
                // Set subscription to "free" with no expiry (unlimited)
                val expiryTime = 0L // 0 means unlimited/no expiry
                
                val result = repository.updateUserSubscription(
                    userId = userId,
                    subscriptionExpiry = expiryTime,
                    currentPlanId = "",
                    subscriptionPeriod = "",
                    planName = "Free"
                )
                
                result.fold(
                    onSuccess = {
                        _errorMessage.value = null
                        // Reload user details to reflect the updated subscription
                        loadUserDetails()
                        onResult(true)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

