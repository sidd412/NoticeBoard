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
                
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = board,
                    plans = plans,
                    selectedPlan = plans.firstOrNull(),
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
    
    fun subscribeToPlan(boardId: String, plan: Plan, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Set subscription to selected plan and expiry to 2026
                val calendar = Calendar.getInstance()
                calendar.set(2026, 11, 31) // December 31, 2026
                val expiryTime = calendar.timeInMillis
                
                val result = repository.updateNoticeBoardSubscription(
                    boardId = boardId,
                    subscriptionType = plan.planName.lowercase(),
                    subscriptionExpiry = expiryTime
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
    
    fun subscribeToFreePlan(boardId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Set subscription to "free" and expiry to 2026
                val calendar = Calendar.getInstance()
                calendar.set(2026, 11, 31) // December 31, 2026
                val expiryTime = calendar.timeInMillis
                
                val result = repository.updateNoticeBoardSubscription(
                    boardId = boardId,
                    subscriptionType = "free",
                    subscriptionExpiry = expiryTime
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

