package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SearchViewModel(private val context: Context) : ViewModel() {
    
    private val repository: FirebaseRepository = FirebaseRepository(context)
    
    private val _searchResults = MutableStateFlow<List<NoticeBoard>>(emptyList())
    val searchResults: StateFlow<List<NoticeBoard>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var searchJob: Job? = null
    private val DEBOUNCE_DELAY = 300L // 300ms delay
    
    fun searchNoticeBoards(query: String) {
        // Cancel previous search job
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            _errorMessage.value = null
            return
        }
        
        // Show loading immediately for better UX
        _isLoading.value = true
        _errorMessage.value = null
        
        searchJob = viewModelScope.launch {
            // Debounce the actual search
            delay(DEBOUNCE_DELAY)
            
            // Check if job was cancelled during delay
            if (!isActive) return@launch
            
            try {
                val results = repository.searchNoticeBoards(query)
                _searchResults.value = results
                println("DEBUG: SearchViewModel.searchNoticeBoards - Found ${results.size} results for query: $query")
            } catch (e: Exception) {
                println("DEBUG: SearchViewModel.searchNoticeBoards - Error: ${e.message}")
                _errorMessage.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
