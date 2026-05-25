package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.features.auth.AuthScreen
import com.example.features.marketplace.CartScreen
import com.example.features.marketplace.MarketplaceScreen
import com.example.features.marketplace.ProductDetailScreen
import com.example.features.marketplace.SearchScreen
import com.example.features.marketplace.WishlistScreen
import com.example.features.marketplace.CreateStoreScreen
import com.example.features.onboarding.OnboardingScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Authentication.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Authentication.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Authentication.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            MarketplaceScreen(
                onProductSelected = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onStoreSelected = { storeId ->
                    navController.navigate(Screen.StoreDetail.createRoute(storeId))
                },
                onSignOut = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCartSelected = {
                    navController.navigate(Screen.Cart.route)
                },
                onSearchSelected = {
                    navController.navigate(Screen.Search.route)
                },
                onWishlistSelected = {
                    navController.navigate(Screen.Wishlist.route)
                },
                onCreateStoreSelected = {
                    navController.navigate(Screen.CreateStore.route)
                }
            )
        }

        composable(route = Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onProductSelected = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }

        composable(route = Screen.Cart.route) {
            CartScreen(
                onNavigateBack = { navController.popBackStack() },
                onCheckoutSuccess = {
                    // Navigate back or show success sequence
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Wishlist.route) {
            WishlistScreen(
                onNavigateBack = { navController.popBackStack() },
                onProductSelected = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onGoToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            ProductDetailScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StoreDetail.route,
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            com.example.features.marketplace.StoreDetailScreen(
                storeId = storeId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.CreateStore.route) {
            CreateStoreScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
