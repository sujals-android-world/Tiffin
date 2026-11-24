package com.example.tiffin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tiffin.domain.KitchenDetailUiState
import com.example.tiffin.domain.KitchenRepository
import com.example.tiffin.domain.SubscriptionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class KitchenDetailViewModel(
    private val kitchenId: String,
    private val kitchenRepository: KitchenRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val todayDayName: String = SimpleDateFormat("EEEE", Locale.ENGLISH)
        .format(Calendar.getInstance().time)

    val uiState: StateFlow<KitchenDetailUiState> = combine(
        flow { emit(kitchenRepository.getKitchens().find { it.id == kitchenId }) },
        subscriptionRepository.isPremium
    ) { kitchen, isPremium ->
        if (kitchen != null) {
            KitchenDetailUiState(
                isLoading = false,
                kitchen = kitchen,
                isPremium = isPremium,
                todayDay = todayDayName
            )
        } else {
            KitchenDetailUiState(
                isLoading = false,
                error = "Kitchen not found"
            )
        }
    }
        .catch { e ->
            emit(
                KitchenDetailUiState(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load kitchen details"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = KitchenDetailUiState(isLoading = true)
        )

    companion object {
        fun provideFactory(
            kitchenId: String,
            kitchenRepository: KitchenRepository,
            subscriptionRepository: SubscriptionRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                KitchenDetailViewModel(
                    kitchenId = kitchenId,
                    kitchenRepository = kitchenRepository,
                    subscriptionRepository = subscriptionRepository
                )
            }
        }
    }
}