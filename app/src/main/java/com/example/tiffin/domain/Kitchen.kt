package com.example.tiffin.domain

data class Kitchen(
    val id: String,
    val name: String,
    val cuisine: String,
    val pricePerTiffin: Int,
    val isVeg: Boolean,
    val rating: Double,
    val deliverySlots: List<String>,
    val weeklyMenu: Map<String, String>
)