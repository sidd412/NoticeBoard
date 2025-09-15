package com.notifiy.noticeboard.ui.viewmodel

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

class BoardEditorViewModel : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository()
    
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
    
    fun clearError() {
        _errorMessage.value = null
    }
}
