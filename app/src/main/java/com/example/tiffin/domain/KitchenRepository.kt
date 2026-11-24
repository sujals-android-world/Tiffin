package com.example.tiffin.domain


interface KitchenRepository {
   suspend fun getKitchens() : List<Kitchen>
}