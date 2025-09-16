package com.notifiy.noticeboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository()
    
    private val _subscribedBoards = MutableStateFlow<List<NoticeBoard>>(emptyList())
    val subscribedBoards: StateFlow<List<NoticeBoard>> = _subscribedBoards.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadSubscribedBoards(userId: String) {
        println("DEBUG: HomeViewModel.loadSubscribedBoards called with userId: $userId")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val boards = repository.getSubscribedBoards(userId)
                println("DEBUG: HomeViewModel got ${boards.size} subscribed boards")
                _subscribedBoards.value = boards
                _errorMessage.value = null
            } catch (e: Exception) {
                println("DEBUG: HomeViewModel error loading subscribed boards: ${e.message}")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun subscribeToBoard(userId: String, instituteCode: String, onResult: (Result<Boolean>) -> Unit) {
        println("DEBUG: HomeViewModel.subscribeToBoard called with userId: $userId, instituteCode: $instituteCode")
        viewModelScope.launch {
            try {
                val result = repository.subscribeToBoardByCode(userId, instituteCode)
                if (result.isSuccess) {
                    println("DEBUG: HomeViewModel.subscribeToBoard - Subscription successful, refreshing boards")
                    loadSubscribedBoards(userId) // Refresh the list
                } else {
                    println("DEBUG: HomeViewModel.subscribeToBoard - Subscription failed: ${result.exceptionOrNull()?.message}")
                }
                onResult(result)
            } catch (e: Exception) {
                println("DEBUG: HomeViewModel.subscribeToBoard - Exception: ${e.message}")
                onResult(Result.failure(e))
            }
        }
    }
    
    fun unsubscribeFromBoard(userId: String, instituteCode: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.unsubscribeFromBoardByCode(userId, instituteCode)
                if (result.isSuccess) {
                    loadSubscribedBoards(userId) // Refresh the list
                }
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    suspend fun getNoticeBoardById(boardId: String): NoticeBoard? {
        return try {
            println("DEBUG: HomeViewModel.getNoticeBoardById - Getting board: $boardId")
            repository.getNoticeBoardById(boardId)
        } catch (e: Exception) {
            println("DEBUG: HomeViewModel.getNoticeBoardById - Error: ${e.message}")
            null
        }
    }
    
    suspend fun getPagesByBoardCode(boardCode: Int): List<Page> {
        return try {
            println("DEBUG: HomeViewModel.getPagesByBoardCode - Getting pages for code: $boardCode")
            repository.getPagesByBoardCode(boardCode)
        } catch (e: Exception) {
            println("DEBUG: HomeViewModel.getPagesByBoardCode - Error: ${e.message}")
            emptyList()
        }
    }
}
