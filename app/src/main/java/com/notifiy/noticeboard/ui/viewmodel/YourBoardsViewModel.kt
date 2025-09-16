package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class YourBoardsViewModel(private val context: Context) : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository(context)
    
    private val _userBoards = MutableStateFlow(UiState<List<NoticeBoard>>())
    val userBoards: StateFlow<UiState<List<NoticeBoard>>> = _userBoards.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadUserBoards(userId: String) {
        println("DEBUG: YourBoardsViewModel.loadUserBoards called with userId: $userId")
        _userBoards.value = _userBoards.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                // Directly get user boards without additional Firebase calls
                val boards = repository.getUserNoticeBoards(userId)
                println("DEBUG: YourBoardsViewModel got ${boards.size} boards")
                _userBoards.value = UiState(data = boards, isLoading = false)
                _errorMessage.value = null
            } catch (e: Exception) {
                println("DEBUG: YourBoardsViewModel error: ${e.message}")
                _userBoards.value = UiState(isLoading = false, error = e.message)
                _errorMessage.value = e.message
            }
        }
    }
    
    fun createNoticeBoard(noticeBoard: NoticeBoard, onResult: (Boolean) -> Unit) {
        println("DEBUG: YourBoardsViewModel.createNoticeBoard called")
        _userBoards.value = _userBoards.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val result = repository.createNoticeBoard(noticeBoard)
                result.fold(
                    onSuccess = { createdBoard ->
                        println("DEBUG: Board created successfully: ${createdBoard.id}")
                        // Add the institute code to the user's profile
                        val addCodeResult = repository.addInstituteCodeToUser(noticeBoard.createdBy, createdBoard.organizationCode)
                        addCodeResult.fold(
                            onSuccess = {
                                println("DEBUG: Institute code added successfully, reloading boards")
                                // Reload user boards to show the new board
                                loadUserBoards(noticeBoard.createdBy)
                                _errorMessage.value = null
                                onResult(true)
                            },
                            onFailure = { exception ->
                                println("DEBUG: Failed to add institute code: ${exception.message}")
                                _errorMessage.value = "Board created but failed to update user profile: ${exception.message}"
                                onResult(false)
                            }
                        )
                    },
                    onFailure = { exception ->
                        println("DEBUG: Failed to create board: ${exception.message}")
                        _userBoards.value = UiState(isLoading = false, error = exception.message)
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in createNoticeBoard: ${e.message}")
                _userBoards.value = UiState(isLoading = false, error = e.message)
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    fun updateNoticeBoard(noticeBoard: NoticeBoard, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.updateNoticeBoard(noticeBoard)
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
    
    override fun onCleared() {
        super.onCleared()
        println("DEBUG: YourBoardsViewModel - onCleared called")
        // Clean up any ongoing operations
        _userBoards.value = UiState()
        _errorMessage.value = null
    }
}
