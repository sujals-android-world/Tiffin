package com.example.tiffin.domain

import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    val isPremium: Flow<Boolean>
    val isFreeTrialEligible: Flow<Boolean>
    val launchCount: Flow<Int>
    suspend fun incrementLaunchCount()
    suspend fun activateSubscription()
}