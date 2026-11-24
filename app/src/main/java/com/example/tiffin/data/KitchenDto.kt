package com.example.tiffin.data

import com.example.tiffin.domain.Kitchen
import com.google.gson.annotations.SerializedName
import java.util.UUID


data class KitchenDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("cuisine") val cuisine: String? = null,
    @SerializedName("pricePerTiffin") val pricePerTiffin: Int? = null,
    @SerializedName("isVeg") val isVeg: Boolean? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("deliverySlots") val deliverySlots: List<String>? = null,
    @SerializedName("weeklyMenu") val weeklyMenu: Map<String, String>? = null
) {
    fun toDomain(): Kitchen = Kitchen(
        id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        name = name ?: "Unknown Kitchen",
        cuisine = cuisine ?: "Home Style",
        pricePerTiffin = pricePerTiffin ?: 0,
        isVeg = isVeg ?: true,
        rating = rating ?: 0.0,
        deliverySlots = deliverySlots.orEmpty(),
        weeklyMenu = weeklyMenu.orEmpty()
    )
}