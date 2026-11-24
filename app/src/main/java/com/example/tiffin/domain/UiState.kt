package com.example.tiffin.domain

data class KitchenListUiState(
    val isLoading: Boolean = false,
    val kitchens: List<Kitchen> = emptyList(),
    val error: String? = null
)

data class KitchenDetailUiState(
    val isLoading: Boolean = true,
    val kitchen: Kitchen? = null,
    val isPremium: Boolean = false,
    val todayDay: String = "",
    val error: String? = null
)