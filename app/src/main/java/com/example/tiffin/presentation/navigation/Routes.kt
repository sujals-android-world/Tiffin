package com.example.tiffin.presentation.navigation


sealed class Routes(val route: String) {
    data object KitchenList : Routes("kitchen_list")
    data object KitchenDetail : Routes("kitchen_detail/{kitchenId}") {
        fun createRoute(kitchenId: String) = "kitchen_detail/$kitchenId"
    }
    data object Paywall : Routes("paywall")
}