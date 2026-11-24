package com.example.tiffin.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tiffin_prefs")

class SubscriptionPreferences(private val context: Context) {

    private val isSubscribedKey = booleanPreferencesKey("is_subscribed")
    private val launchCountKey = intPreferencesKey("launch_count")

    val isSubscribed: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isSubscribedKey] ?: false
    }

    val launchCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[launchCountKey] ?: 0
    }

    suspend fun incrementLaunchCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[launchCountKey] ?: 0
            prefs[launchCountKey] = current + 1
        }
    }

    suspend fun setSubscribed(subscribed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[isSubscribedKey] = subscribed
        }
    }
}