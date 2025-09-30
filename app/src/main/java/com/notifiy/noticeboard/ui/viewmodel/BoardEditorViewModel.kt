package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BoardEditorViewModel(private val context: Context) : ViewModel() {
    
    val repository: FirebaseRepository = FirebaseRepository(context)
    
    private val _authState = MutableStateFlow(UiState<com.notifiy.noticeboard.data.model.User?>())
    val authState: StateFlow<UiState<com.notifiy.noticeboard.data.model.User?>> = _authState.asStateFlow()
    
    private val _currentNotice = MutableStateFlow<Notice?>(null)
    val currentNotice: StateFlow<Notice?> = _currentNotice.asStateFlow()
    
    private val _currentPage = MutableStateFlow<Page?>(null)
    val currentPage: StateFlow<Page?> = _currentPage.asStateFlow()
    
    private val _currentBoard = MutableStateFlow<NoticeBoard?>(null)
    val currentBoard: StateFlow<NoticeBoard?> = _currentBoard.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    var currentBoardId: String = ""
    
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
    
    fun loadNotice(noticeId: String) {
        viewModelScope.launch {
            try {
                // Load notice details
                val notices = repository.getNoticesByBoardId(noticeId)
                // For now, we'll assume we're editing the first notice or create a new one
                // In a real app, you'd pass the specific notice ID
                _currentNotice.value = notices.firstOrNull()
                currentBoardId = noticeId
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun loadPage(pageId: String) {
        viewModelScope.launch {
            try {
                if (pageId == "new") {
                    _currentPage.value = null
                } else {
                    val page = repository.getPageById(pageId)
                    _currentPage.value = page
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun loadBoard(boardId: String) {
        viewModelScope.launch {
            try {
                val board = repository.getNoticeBoardById(boardId)
                _currentBoard.value = board
                currentBoardId = boardId
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun publishNotice(notice: Notice, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.createNotice(notice)
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
    
    fun updateNotice(notice: Notice, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.updateNotice(notice)
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
    
    fun savePage(page: Page, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                println("DEBUG: BoardEditorViewModel.savePage - Starting to save page: $page")
                val result = if (page.id.isEmpty() || _currentPage.value == null) {
                    println("DEBUG: BoardEditorViewModel.savePage - Creating new page")
                    repository.createPage(page)
                } else {
                    println("DEBUG: BoardEditorViewModel.savePage - Updating existing page")
                    repository.updatePage(page)
                }
                result.fold(
                    onSuccess = { savedPage ->
                        println("DEBUG: BoardEditorViewModel.savePage - Success: $savedPage")
                        _errorMessage.value = null
                        
                        // Increment notification count for subscribed users
                        val boardId = getBoardIdByCode(savedPage.code)
                        if (boardId != null) {
                            repository.incrementNotificationCount(boardId, savedPage.code)
                            
                            // Send local notification to subscribers
                            val title = "New Notice Update"
                            val body = "A new notice has been added to ${getNoticeBoardById(boardId)?.organizationName ?: "a notice board"} you're subscribed to"
                            repository.sendLocalNotificationToSubscribers(boardId, savedPage.code, title, body)
                        }
                        
                        onResult(true)
                    },
                    onFailure = { exception ->
                        println("DEBUG: BoardEditorViewModel.savePage - Failure: ${exception.message}")
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: BoardEditorViewModel.savePage - Exception: ${e.message}")
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    private suspend fun getBoardIdByCode(code: String): String? {
        return try {
            repository.getNoticeBoardByCode(code)?.id
        } catch (e: Exception) {
            println("DEBUG: BoardEditorViewModel.getBoardIdByCode - Error: ${e.message}")
            null
        }
    }
    
    private suspend fun getNoticeBoardById(boardId: String): NoticeBoard? {
        return try {
            repository.getNoticeBoardById(boardId)
        } catch (e: Exception) {
            println("DEBUG: BoardEditorViewModel.getNoticeBoardById - Error: ${e.message}")
            null
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun checkPageLimit(boardCode: String, onResult: (Boolean, Int) -> Unit) {
        android.util.Log.d("sidxp", "checkPageLimit - Starting with boardCode: '$boardCode'")
        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser == null) {
                    android.util.Log.d("sidxp", "checkPageLimit - No current user found")
                    onResult(false, 0)
                    return@launch
                }
                
                // Get board's current plan
                val board = repository.getNoticeBoardByCode(boardCode)
                if (board == null) {
                    android.util.Log.d("sidxp", "checkPageLimit - Board not found for code: '$boardCode'")
                    onResult(false, 0)
                    return@launch
                }
                
                android.util.Log.d("sidxp", "checkPageLimit - Found board: ${board.organizationName}")
                
                val plans = repository.getAllPlans()
                android.util.Log.d("sidxp", "checkPageLimit - Board currentPlanId: '${board.currentPlanId}'")
                android.util.Log.d("sidxp", "checkPageLimit - Board planName: '${board.planName}'")
                android.util.Log.d("sidxp", "checkPageLimit - Available plans: ${plans.map { "${it.planName} (${it.pages} pages)" }}")
                
                val boardPlan = if (board.currentPlanId.isNotEmpty()) {
                    // Find plan by currentPlanId
                    val foundPlan = plans.find { plan ->
                        plan.planId.contains(board.currentPlanId) || plan.id == board.currentPlanId
                    }
                    android.util.Log.d("sidxp", "checkPageLimit - Found plan by currentPlanId: ${foundPlan?.planName} with ${foundPlan?.pages} pages")
                    foundPlan
                } else if (board.planName.isNotEmpty()) {
                    // Find plan by planName (case-insensitive)
                    android.util.Log.d("sidxp", "checkPageLimit - Searching for plan with name: '${board.planName}'")
                    val foundPlan = plans.find { plan ->
                        val matches = plan.planName.equals(board.planName, ignoreCase = true)
                        android.util.Log.d("sidxp", "checkPageLimit - Comparing '${plan.planName}' with '${board.planName}': $matches")
                        matches
                    }
                    android.util.Log.d("sidxp", "checkPageLimit - Found plan by planName: ${foundPlan?.planName} with ${foundPlan?.pages} pages")
                    foundPlan
                } else {
                    // Default to free plan if no plan is set
                    val foundPlan = plans.find { it.planName.equals("free", ignoreCase = true) }
                    android.util.Log.d("sidxp", "checkPageLimit - Found default free plan: ${foundPlan?.planName} with ${foundPlan?.pages} pages")
                    foundPlan
                }
                
                android.util.Log.d("sidxp", "checkPageLimit - Final board plan: ${boardPlan?.planName} with ${boardPlan?.pages} pages")
                val planPages = boardPlan?.pages ?: 0
                android.util.Log.d("sidxp", "checkPageLimit - Plan pages limit: $planPages")
                
                // Get current page count for this board
                val existingPages = repository.getPagesByBoardCode(boardCode.toString())
                val currentPageCount = existingPages.size
                
                android.util.Log.d("sidxp", "checkPageLimit - Current page count: $currentPageCount")
                android.util.Log.d("sidxp", "checkPageLimit - Plan pages limit: $planPages")
                android.util.Log.d("sidxp", "checkPageLimit - Can create: ${currentPageCount < planPages}")
                
                val canCreate = currentPageCount < planPages
                onResult(canCreate, planPages)
                
            } catch (e: Exception) {
                android.util.Log.e("sidxp", "Error checking page limit: ${e.message}")
                onResult(false, 0)
            }
        }
    }
    
    fun deletePage(pageId: String, onResult: (Boolean) -> Unit) {
        println("DEBUG: BoardEditorViewModel.deletePage called for page: $pageId")
        viewModelScope.launch {
            try {
                val result = repository.deletePage(pageId)
                result.fold(
                    onSuccess = {
                        println("DEBUG: Page deleted successfully")
                        _errorMessage.value = null
                        onResult(true)
                    },
                    onFailure = { exception ->
                        println("DEBUG: Failed to delete page: ${exception.message}")
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in deletePage: ${e.message}")
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
}
