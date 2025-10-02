package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.Plan
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubscriptionUiStateSimple(
    val isLoading: Boolean = false,
    val data: com.notifiy.noticeboard.data.model.User? = null,
    val plans: List<Plan> = emptyList(),
    val selectedPlan: Plan? = null
)

class SubscriptionViewModelSimple(
    private val context: Context,
    private val repository: FirebaseRepository = FirebaseRepository(context)
) : ViewModel() {
    
    private val _boardState = MutableStateFlow(SubscriptionUiStateSimple())
    val boardState: StateFlow<SubscriptionUiStateSimple> = _boardState.asStateFlow()
    
    fun loadUserDetails() {
        viewModelScope.launch {
            try {
                _boardState.value = _boardState.value.copy(isLoading = true)
                
                val currentUser = repository.getCurrentUser()
                val plans = repository.getAllPlans()
                
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = currentUser,
                    plans = plans,
                    selectedPlan = plans.firstOrNull()
                )
                
            } catch (e: Exception) {
                _boardState.value = _boardState.value.copy(isLoading = false)
            }
        }
    }
    
    fun selectPlan(plan: Plan) {
        _boardState.value = _boardState.value.copy(selectedPlan = plan)
    }
    
    fun subscribeToPlan(context: android.app.Activity, plan: Plan, isMonthly: Boolean, callback: (Boolean) -> Unit) {
        callback(true)
    }
}
