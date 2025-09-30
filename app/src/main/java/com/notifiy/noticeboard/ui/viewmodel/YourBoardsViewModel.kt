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
    
    fun createNoticeBoard(noticeBoard: NoticeBoard, onResult: (Boolean, NoticeBoard?) -> Unit) {
        android.util.Log.d("sidxp", "YourBoardsViewModel.createNoticeBoard called")
        _userBoards.value = _userBoards.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                // Check board limit first
                val boardLimitResult = repository.checkBoardLimit(noticeBoard.createdBy)
                boardLimitResult.fold(
                    onSuccess = { (canCreate, boardLimit) ->
                        if (!canCreate) {
                            android.util.Log.d("sidxp", "YourBoardsViewModel.createNoticeBoard - Board limit reached: $boardLimit")
                            _userBoards.value = _userBoards.value.copy(isLoading = false, error = null)
                            // Don't set error message here, let the UI show the popup
                            onResult(false, null)
                            return@launch
                        }
                        
                        android.util.Log.d("sidxp", "YourBoardsViewModel.createNoticeBoard - Board limit check passed: $boardLimit")
                        
                        // Proceed with board creation
                        val result = repository.createNoticeBoard(noticeBoard)
                        result.fold(
                            onSuccess = { createdBoard ->
                                android.util.Log.d("sidxp", "Board created successfully: ${createdBoard.id} with code: ${createdBoard.organizationCode}")
                                // Add the institute code to the user's profile
                                val addCodeResult = repository.addInstituteCodeToUser(noticeBoard.createdBy, createdBoard.organizationCode)
                                addCodeResult.fold(
                                    onSuccess = {
                                        android.util.Log.d("sidxp", "Institute code added successfully, reloading boards")
                                        // Reload user boards to show the new board
                                        loadUserBoards(noticeBoard.createdBy)
                                        _errorMessage.value = null
                                        onResult(true, createdBoard)
                                    },
                                    onFailure = { exception ->
                                        android.util.Log.e("sidxp", "Failed to add institute code: ${exception.message}")
                                        _errorMessage.value = "Board created but failed to update user profile: ${exception.message}"
                                        onResult(false, null)
                                    }
                                )
                            },
                            onFailure = { exception ->
                                android.util.Log.e("sidxp", "Failed to create board: ${exception.message}")
                                _userBoards.value = UiState(isLoading = false, error = exception.message)
                                _errorMessage.value = exception.message
                                onResult(false, null)
                            }
                        )
                    },
                    onFailure = { exception ->
                        android.util.Log.e("sidxp", "Failed to check board limit: ${exception.message}")
                        _userBoards.value = UiState(isLoading = false, error = exception.message)
                        _errorMessage.value = exception.message
                        onResult(false, null)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("sidxp", "Exception in createNoticeBoard: ${e.message}")
                _userBoards.value = UiState(isLoading = false, error = e.message)
                _errorMessage.value = e.message
                onResult(false, null)
            }
        }
    }
    
    fun checkBoardLimit(userId: String, onResult: (Boolean, Int) -> Unit) {
        android.util.Log.d("board limit", "YourBoardsViewModel.checkBoardLimit - Called for userId: $userId")
        viewModelScope.launch {
            try {
                val boardLimitResult = repository.checkBoardLimit(userId)
                boardLimitResult.fold(
                    onSuccess = { (canCreate, boardLimit) ->
                        android.util.Log.d("board limit", "YourBoardsViewModel.checkBoardLimit - Success: canCreate=$canCreate, limit=$boardLimit")
                        onResult(canCreate, boardLimit)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("board limit", "YourBoardsViewModel.checkBoardLimit - Failed: ${exception.message}")
                        onResult(false, 0)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("board limit", "YourBoardsViewModel.checkBoardLimit - Exception: ${e.message}")
                onResult(false, 0)
            }
        }
    }
    
    fun reloadUserBoards() {
        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser != null) {
                    android.util.Log.d("sidxp", "YourBoardsViewModel.reloadUserBoards - Reloading boards for user: ${currentUser.name}")
                    loadUserBoards(currentUser.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("sidxp", "YourBoardsViewModel.reloadUserBoards - Error: ${e.message}")
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
    
    fun deleteNoticeBoard(boardId: String, userId: String, onResult: (Boolean) -> Unit) {
        println("DEBUG: YourBoardsViewModel.deleteNoticeBoard called for board: $boardId")
        _userBoards.value = _userBoards.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val result = repository.deleteNoticeBoard(boardId)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("sidxp", "YourBoardsViewModel.deleteNoticeBoard - Board deleted successfully, reloading data")
                        // Reload user boards to reflect the deletion
                        loadUserBoards(userId)
                        _errorMessage.value = null
                        onResult(true)
                    },
                    onFailure = { exception ->
                        println("DEBUG: Failed to delete board: ${exception.message}")
                        _userBoards.value = UiState(isLoading = false, error = exception.message)
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in deleteNoticeBoard: ${e.message}")
                _userBoards.value = UiState(isLoading = false, error = e.message)
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
