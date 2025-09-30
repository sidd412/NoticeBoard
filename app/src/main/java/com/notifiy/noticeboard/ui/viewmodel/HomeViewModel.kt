package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
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

class HomeViewModel(private val context: Context) : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository(context)
    
    private val _subscribedBoards = MutableStateFlow<List<NoticeBoard>>(emptyList())
    val subscribedBoards: StateFlow<List<NoticeBoard>> = _subscribedBoards.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()
    
    private val _boardNotifications = MutableStateFlow<Map<String, Int>>(emptyMap())
    val boardNotifications: StateFlow<Map<String, Int>> = _boardNotifications.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadNotificationCount(userId: String) {
        println("DEBUG: HomeViewModel.loadNotificationCount called with userId: $userId")
        viewModelScope.launch {
            try {
                val notifications = repository.getUserNotifications(userId)
                println("DEBUG: HomeViewModel.loadNotificationCount - Got ${notifications.size} notifications")
                
                val totalCount = notifications.sumOf { it.unreadCount }
                val boardCounts = notifications.associate { it.boardId to it.unreadCount }
                
                _notificationCount.value = totalCount
                _boardNotifications.value = boardCounts
                
                println("DEBUG: HomeViewModel.loadNotificationCount - Total count: $totalCount, Board counts: $boardCounts")
            } catch (e: Exception) {
                println("DEBUG: HomeViewModel.loadNotificationCount - Error: ${e.message}")
                _notificationCount.value = 0
                _boardNotifications.value = emptyMap()
            }
        }
    }
    
    fun markNotificationAsRead(userId: String, boardId: String) {
        println("DEBUG: HomeViewModel.markNotificationAsRead called with userId: $userId, boardId: $boardId")
        viewModelScope.launch {
            try {
                val result = repository.markNotificationAsRead(userId, boardId)
                if (result.isSuccess) {
                    println("DEBUG: HomeViewModel.markNotificationAsRead - Success, refreshing count")
                    loadNotificationCount(userId) // Refresh the count
                } else {
                    println("DEBUG: HomeViewModel.markNotificationAsRead - Failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("DEBUG: HomeViewModel.markNotificationAsRead - Exception: ${e.message}")
            }
        }
    }
    
    fun loadSubscribedBoards(userId: String) {
        println("DEBUG: HomeViewModel.loadSubscribedBoards called with userId: $userId")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val boards = repository.getSubscribedBoards(userId)
                println("DEBUG: HomeViewModel got ${boards.size} subscribed boards")
                _subscribedBoards.value = boards
                _errorMessage.value = null
                
                // Also load notification count
                loadNotificationCount(userId)
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
    
    suspend fun isUserSubscribedToBoard(userId: String, instituteCode: String): Boolean {
        return try {
            val user = repository.getCurrentUser()
            user?.subscribedCodes?.contains(instituteCode) ?: false
        } catch (e: Exception) {
            println("DEBUG: isUserSubscribedToBoard - Error: ${e.message}")
            false
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
    
    fun forceRefreshSubscribedBoards(userId: String) {
        println("DEBUG: HomeViewModel.forceRefreshSubscribedBoards called")
        // Clear any cached data first
        repository.clearSubscribedBoardsCache(userId)
        // Force reload
        loadSubscribedBoards(userId)
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
    
    suspend fun getPagesByBoardCode(boardCode: String): List<Page> {
        return try {
            println("DEBUG: HomeViewModel.getPagesByBoardCode - Getting pages for code: '$boardCode'")
            repository.getPagesByBoardCode(boardCode)
        } catch (e: Exception) {
            println("DEBUG: HomeViewModel.getPagesByBoardCode - Error: ${e.message}")
            emptyList()
        }
    }
}
