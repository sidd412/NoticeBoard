package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoardDetailsUiState(
    val isLoading: Boolean = false,
    val data: NoticeBoard? = null,
    val error: String? = null
)

data class PagesUiState(
    val isLoading: Boolean = false,
    val data: List<Page>? = null,
    val error: String? = null
)

class BoardDetailsViewModel(
    private val context: Context,
    private val repository: FirebaseRepository = FirebaseRepository(context)
) : ViewModel() {
    
    private val _boardState = MutableStateFlow(BoardDetailsUiState())
    val boardState: StateFlow<BoardDetailsUiState> = _boardState.asStateFlow()
    
    private val _pagesState = MutableStateFlow(PagesUiState())
    val pagesState: StateFlow<PagesUiState> = _pagesState.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadBoardDetails(boardId: String) {
        viewModelScope.launch {
            try {
                println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Starting with boardId: $boardId")
                _boardState.value = _boardState.value.copy(isLoading = true, error = null)
                _pagesState.value = _pagesState.value.copy(isLoading = true, error = null)
                
                // Load board details
                val board = repository.getNoticeBoardById(boardId)
                println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Loaded board: $board")
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = board,
                    error = null
                )
                
                // Load pages for this board
                if (board != null) {
                    val boardCode = board.organizationCode
                    println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Board code: '$boardCode'")
                    
                    // Debug: Get all pages first to see what's in the database
                    val allPages = repository.getAllPages()
                    println("DEBUG: BoardDetailsViewModel.loadBoardDetails - All pages in database: ${allPages.size}")
                    
                    val pages = repository.getPagesByBoardCode(boardCode)
                    println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Loaded ${pages.size} pages for board code '$boardCode'")
                    _pagesState.value = _pagesState.value.copy(
                        isLoading = false,
                        data = pages,
                        error = null
                    )
                } else {
                    println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Board is null")
                    _pagesState.value = _pagesState.value.copy(
                        isLoading = false,
                        data = emptyList(),
                        error = null
                    )
                }
                
            } catch (e: Exception) {
                println("DEBUG: BoardDetailsViewModel.loadBoardDetails - Error: ${e.message}")
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _pagesState.value = _pagesState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _errorMessage.value = e.message
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun refreshData(boardId: String) {
        loadBoardDetails(boardId)
    }
    
    fun deletePage(pageId: String, boardId: String, onResult: (Boolean) -> Unit) {
        println("DEBUG: BoardDetailsViewModel.deletePage called for page: $pageId")
        _pagesState.value = _pagesState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val result = repository.deletePage(pageId)
                result.fold(
                    onSuccess = {
                        println("DEBUG: Page deleted successfully, reloading board details")
                        // Reload board details to reflect the deletion
                        loadBoardDetails(boardId)
                        _errorMessage.value = null
                        onResult(true)
                    },
                    onFailure = { exception ->
                        println("DEBUG: Failed to delete page: ${exception.message}")
                        _pagesState.value = _pagesState.value.copy(isLoading = false, error = exception.message)
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in deletePage: ${e.message}")
                _pagesState.value = _pagesState.value.copy(isLoading = false, error = e.message)
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    fun updateNoticeBoard(noticeBoard: NoticeBoard, onResult: (Boolean) -> Unit) {
        println("DEBUG: BoardDetailsViewModel.updateNoticeBoard called for board: ${noticeBoard.id}")
        _boardState.value = _boardState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val result = repository.updateNoticeBoard(noticeBoard)
                result.fold(
                    onSuccess = { updatedBoard ->
                        println("DEBUG: Board updated successfully")
                        _boardState.value = _boardState.value.copy(
                            isLoading = false,
                            data = updatedBoard,
                            error = null
                        )
                        _errorMessage.value = null
                        onResult(true)
                    },
                    onFailure = { exception ->
                        println("DEBUG: Failed to update board: ${exception.message}")
                        _boardState.value = _boardState.value.copy(isLoading = false, error = exception.message)
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in updateNoticeBoard: ${e.message}")
                _boardState.value = _boardState.value.copy(isLoading = false, error = e.message)
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
}