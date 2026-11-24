package com.example.tiffin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tiffin.domain.Kitchen
import com.example.tiffin.domain.KitchenListUiState
import com.example.tiffin.domain.KitchenRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn



class KitchenListViewModel(
    private val repository: KitchenRepository
) : ViewModel() {

    val uiState: StateFlow<KitchenListUiState> = flow {
        emit(repository.getKitchens())
    }
        .map { kitchens ->
            KitchenListUiState(kitchens = kitchens, isLoading = false)
        }
        .onStart {
            emit(KitchenListUiState(isLoading = true))
        }
        .catch { e ->
            emit(
                KitchenListUiState(
                    error = e.localizedMessage ?: "Failed to load kitchens",
                    isLoading = false
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KitchenListUiState(isLoading = true)
        )

    companion object {
        fun provideFactory(repository: KitchenRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    KitchenListViewModel(repository)
                }
            }
    }
}