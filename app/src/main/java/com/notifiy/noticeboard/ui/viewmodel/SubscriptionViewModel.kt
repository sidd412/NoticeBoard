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
    val data: NoticeBoard? = null,
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
    
    fun loadBoardDetails(boardId: String) {
        viewModelScope.launch {
            try {
                _boardState.value = _boardState.value.copy(isLoading = true, error = null)
                
                val board = repository.getNoticeBoardById(boardId)
                val plans = repository.getAllPlans()
                val currentUser = repository.getCurrentUser()
                
                // Find current board's plan and preselect it
                val currentPlan = board?.currentPlanId?.let { planId ->
                    plans.find { plan ->
                        plan.planId.contains(planId) || plan.id == planId
                    }
                }
                
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = board,
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
    
    fun subscribeToPlan(boardId: String, plan: Plan, isMonthly: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                println("DEBUG: SubscriptionViewModel - Starting subscription process for plan: ${plan.planName}, isMonthly: $isMonthly")
                
                // Get current user ID first
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    println("DEBUG: SubscriptionViewModel - No authenticated user found")
                    _errorMessage.value = "User not authenticated"
                    onResult(false)
                    return@launch
                }
                
                println("DEBUG: SubscriptionViewModel - Current user ID: $userId")
                
                // Check if this is a new board that needs to be created first
                var actualBoardId = boardId
                if (boardId.startsWith("new_")) {
                    println("DEBUG: SubscriptionViewModel - Board ID starts with 'new_', creating board first")
                    val boardCode = boardId.substringAfter("new_")
                    
                    // Get the plan ID for this subscription
                    val planId = if (isMonthly && plan.planId.isNotEmpty()) {
                        plan.planId[0] // First element is monthly plan ID
                    } else if (!isMonthly && plan.planId.size > 1) {
                        plan.planId[1] // Second element is annual plan ID
                    } else {
                        plan.id // Fallback to plan document ID
                    }
                    
                    println("DEBUG: SubscriptionViewModel - Creating board with planId: '$planId', planName: '${plan.planName}'")
                    
                    // Calculate proper expiry date based on subscription period
                    val calendar = Calendar.getInstance()
                    if (isMonthly) {
                        calendar.add(Calendar.MONTH, 1) // Add 1 month
                    } else {
                        calendar.add(Calendar.YEAR, 1) // Add 1 year
                    }
                    val expiryTime = calendar.timeInMillis
                    
                    // Create a new board with the subscription
                    val newBoard = com.notifiy.noticeboard.data.model.NoticeBoard(
                        id = java.util.UUID.randomUUID().toString(),
                        organizationName = "New Board",
                        organizationCode = boardCode,
                        organizationEmail = "",
                        organizationLocation = "",
                        organizationWhatsapp = "",
                        createdBy = userId,
                        subscriptionPeriod = if (isMonthly) "monthly" else "annual",
                        subscriptionExpiry = expiryTime,
                        currentPlanId = planId,
                        planName = plan.planName
                    )
                    
                    val createResult = repository.createNoticeBoard(newBoard)
                    createResult.fold(
                        onSuccess = { createdBoard ->
                            println("DEBUG: SubscriptionViewModel - Board created successfully: ${createdBoard.id}")
                            actualBoardId = createdBoard.id
                            
                            // Add institute code to user
                            repository.addInstituteCodeToUser(userId, createdBoard.organizationCode)
                            
                            // Board plan is already set, just complete
                            _errorMessage.value = null
                            onResult(true)
                        },
                        onFailure = { exception ->
                            println("DEBUG: SubscriptionViewModel - Board creation failed: ${exception.message}")
                            _errorMessage.value = "Failed to create board: ${exception.message}"
                            onResult(false)
                        }
                    )
                } else {
                    // Existing board - update subscription
                    // Calculate proper expiry date based on subscription period
                    val calendar = Calendar.getInstance()
                    if (isMonthly) {
                        calendar.add(Calendar.MONTH, 1) // Add 1 month
                    } else {
                        calendar.add(Calendar.YEAR, 1) // Add 1 year
                    }
                    val expiryTime = calendar.timeInMillis
                    
                    // Get the plan ID for this subscription
                    val planId = if (isMonthly && plan.planId.isNotEmpty()) {
                        plan.planId[0] // First element is monthly plan ID
                    } else if (!isMonthly && plan.planId.size > 1) {
                        plan.planId[1] // Second element is annual plan ID
                    } else {
                        plan.id // Fallback to plan document ID
                    }
                    
                    println("DEBUG: SubscriptionViewModel - Updating board subscription for boardId: $boardId with planId: '$planId', planName: '${plan.planName}'")
                    val result = repository.updateNoticeBoardSubscription(
                        boardId = boardId,
                        subscriptionExpiry = expiryTime,
                        currentPlanId = planId,
                        subscriptionPeriod = if (isMonthly) "monthly" else "annual",
                        planName = plan.planName
                    )
                    
                    result.fold(
                        onSuccess = {
                            println("DEBUG: SubscriptionViewModel - Board subscription updated successfully")
                            _errorMessage.value = null
                            onResult(true)
                        },
                        onFailure = { exception ->
                            println("DEBUG: SubscriptionViewModel - updateNoticeBoardSubscription failed: ${exception.message}")
                            exception.printStackTrace()
                            _errorMessage.value = exception.message
                            onResult(false)
                        }
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: SubscriptionViewModel - Exception in subscribeToPlan: ${e.message}")
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
    
    fun subscribeToFreePlan(boardId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Set subscription to "free" with no expiry (unlimited)
                val expiryTime = 0L // 0 means unlimited/no expiry
                
                val result = repository.updateNoticeBoardSubscription(
                    boardId = boardId,
                    subscriptionExpiry = expiryTime,
                    currentPlanId = "",
                    subscriptionPeriod = "",
                    planName = "Free"
                )
                
                result.fold(
                    onSuccess = {
                        _errorMessage.value = null
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

