package com.example.tiffin.data

import com.example.tiffin.domain.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class SubscriptionRepositoryImpl(
    private val preferences: UserPreferences
) : SubscriptionRepository {

    override val isPremium: Flow<Boolean> = preferences.isSubscribed
    override val isFreeTrialEligible: Flow<Boolean> = preferences.isFreeTrialEligible
    override val launchCount: Flow<Int> = preferences.launchCount

    override suspend fun incrementLaunchCount() {
        preferences.incrementLaunchCount()
    }

    override suspend fun activateSubscription() {
        preferences.activateSubscription()
    }
}