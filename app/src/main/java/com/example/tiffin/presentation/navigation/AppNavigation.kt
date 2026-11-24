package com.example.tiffin.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tiffin.domain.Analytics
import com.example.tiffin.domain.KitchenRepository
import com.example.tiffin.domain.SubscriptionRepository
import com.example.tiffin.presentation.KitchenDetailViewModel
import com.example.tiffin.presentation.KitchenListViewModel
import com.example.tiffin.presentation.screen.KitchenDetailScreen
import com.example.tiffin.presentation.screen.KitchenListScreen
import com.example.tiffin.presentation.screen.PaywallScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    repository: KitchenRepository,
    subscriptionRepository: SubscriptionRepository,
    analytics: Analytics,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        subscriptionRepository.incrementLaunchCount()
        val count = subscriptionRepository.launchCount.first()
        val isPaid = subscriptionRepository.isPremium.first()
        if (count == 3 && !isPaid) {
            analytics.track("paywall_shown", mapOf("source" to "third_launch"))
            navController.navigate(Routes.Paywall.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.KitchenList.route
    ) {
        composable(Routes.KitchenList.route) {
            val viewModel: KitchenListViewModel = viewModel(
                factory = KitchenListViewModel.provideFactory(repository)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                analytics.track("kitchen_list_viewed")
            }

            KitchenListScreen(
                state = state,
                onKitchenClick = { kitchenId ->
                    analytics.track("kitchen_detail_opened", mapOf("kitchen_id" to kitchenId))
                    navController.navigate(Routes.KitchenDetail.createRoute(kitchenId))
                }
            )
        }

        composable(
            route = Routes.KitchenDetail.route,
            arguments = listOf(navArgument("kitchenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val kitchenId = backStackEntry.arguments?.getString("kitchenId").orEmpty()
            val viewModel: KitchenDetailViewModel = viewModel(
                factory = KitchenDetailViewModel.provideFactory(
                    kitchenId = kitchenId,
                    kitchenRepository = repository,
                    subscriptionRepository = subscriptionRepository
                )
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            KitchenDetailScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onSubscribeClick = {
                    analytics.track("paywall_shown", mapOf("source" to "detail_subscribe_button"))
                    navController.navigate(Routes.Paywall.route)
                }
            )
        }

        composable(Routes.Paywall.route) {
            val isTrialEligible by subscriptionRepository.isFreeTrialEligible
                .collectAsStateWithLifecycle(initialValue = true)

            PaywallScreen(
                isFreeTrialEligible = isTrialEligible,
                onClose = { navController.popBackStack() },
                onConfirmPurchase = {
                    scope.launch {
                        subscriptionRepository.activateSubscription()
                        analytics.track(
                            "subscription_purchased",
                            mapOf("price" to if (isTrialEligible) "1" else "249")
                        )
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}