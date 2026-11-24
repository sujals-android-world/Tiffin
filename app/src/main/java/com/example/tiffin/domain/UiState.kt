package com.example.tiffin.domain

data class KitchenListUiState(
    val isLoading: Boolean = false,
    val kitchens: List<Kitchen> = emptyList(),
    val error: String? = null
)