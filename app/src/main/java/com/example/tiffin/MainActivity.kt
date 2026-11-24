package com.example.tiffin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.tiffin.data.KitchenRepositoryImpl
import com.example.tiffin.data.SubscriptionPreferences
import com.example.tiffin.data.SubscriptionRepositoryImpl
import com.example.tiffin.domain.LogcatAnalytics
import com.example.tiffin.presentation.navigation.AppNavigation
import com.example.tiffin.presentation.ui.theme.TiffinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiffinTheme {
                val repository = remember { KitchenRepositoryImpl(applicationContext) }
                val subscriptionRepository = remember {
                    SubscriptionRepositoryImpl(SubscriptionPreferences(applicationContext))
                }
                val analytics = remember { LogcatAnalytics() }
                AppNavigation(
                    repository = repository,
                    subscriptionRepository = subscriptionRepository,
                    analytics = analytics,
                )
            }
        }
    }
}