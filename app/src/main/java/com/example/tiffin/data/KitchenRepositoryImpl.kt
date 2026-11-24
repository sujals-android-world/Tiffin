package com.example.tiffin.data

import android.content.Context
import com.example.tiffin.domain.Kitchen
import com.example.tiffin.domain.KitchenRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class KitchenRepositoryImpl(
    private val context: Context,
    private val gson: Gson = Gson()
) : KitchenRepository {

    override suspend fun getKitchens(): List<Kitchen> = withContext(Dispatchers.IO) {
        val jsonText = context.assets.open("kitchens.json").bufferedReader().use { it.readText() }
        gson.fromJson(jsonText, Array<KitchenDto>::class.java).map { it.toDomain() }
    }
}