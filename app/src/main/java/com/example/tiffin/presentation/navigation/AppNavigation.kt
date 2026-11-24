package com.example.tiffin.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tiffin.domain.KitchenRepository
import com.example.tiffin.presentation.KitchenListViewModel
import com.example.tiffin.presentation.screen.KitchenDetailScreen
import com.example.tiffin.presentation.screen.KitchenListScreen
import com.example.tiffin.presentation.screen.PaywallScreen

@Composable
fun AppNavigation(
    repository: KitchenRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.KitchenList.route
    ) {
        composable(Routes.KitchenList.route) {
            val viewModel: KitchenListViewModel = viewModel(
                factory = KitchenListViewModel.provideFactory(repository)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            KitchenListScreen(
                state = state,
                onKitchenClick = { kitchenId ->
                    navController.navigate(Routes.KitchenDetail.createRoute(kitchenId))
                }
            )
        }

        composable(
            route = Routes.KitchenDetail.route,
            arguments = listOf(navArgument("kitchenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val kitchenId = backStackEntry.arguments?.getString("kitchenId").orEmpty()
            KitchenDetailScreen()
        }

        composable(Routes.Paywall.route) {
            PaywallScreen()
        }
    }
}