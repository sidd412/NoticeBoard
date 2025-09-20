package com.notifiy.noticeboard.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(context) as T
            }
            modelClass.isAssignableFrom(YourBoardsViewModel::class.java) -> {
                YourBoardsViewModel(context) as T
            }
            modelClass.isAssignableFrom(BoardDetailsViewModel::class.java) -> {
                BoardDetailsViewModel(context) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(context) as T
            }
            modelClass.isAssignableFrom(BoardEditorViewModel::class.java) -> {
                BoardEditorViewModel(context) as T
            }
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) -> {
                SubscriptionViewModel(context) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(context) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@Composable
fun <T : ViewModel> cachedViewModel(
    modelClass: Class<T>,
    context: Context = LocalContext.current
): T {
    val factory = remember { ViewModelFactory(context) }
    return viewModel(
        modelClass = modelClass,
        factory = factory
    )
}
