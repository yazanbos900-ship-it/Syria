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
import com.example.features.marketplace.ProfileScreen
import com.example.features.marketplace.AddProductScreen
import com.example.features.onboarding.OnboardingScreen

import com.example.features.marketplace.StoreManagementScreen
import com.example.features.marketplace.OrdersScreen
import com.example.features.marketplace.CheckoutScreen
import com.example.features.admin.AdminDashboardScreen
import com.example.features.chat.ChatListScreen
import com.example.features.chat.ChatScreen

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
                    navController.navigate(Screen.Authentication.route) {
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
                },
                onManageStoreSelected = { _ ->
                    navController.navigate(Screen.StoreManagement.route)
                },
                onAdminSelected = {
                    navController.navigate(Screen.AdminDashboard.route)
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
                    navController.navigate(Screen.Checkout.route)
                }
            )
        }

        composable(route = Screen.Checkout.route) {
            CheckoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route) {
                        popUpTo(Screen.Home.route)
                    }
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
                onBack = { navController.popBackStack() },
                onContactSeller = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId))
                },
                onSellerProfileClick = { sellerType, id ->
                    if (sellerType == "STORE") {
                        navController.navigate(Screen.StoreDetail.createRoute(id))
                    } else {
                        navController.navigate(Screen.SellerProfile.createRoute(id))
                    }
                }
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
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onManageStore = {
                    navController.navigate(Screen.StoreManagement.route)
                }
            )
        }
        
        composable(
            route = Screen.SellerProfile.route,
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            com.example.features.marketplace.SellerProfileScreen(
                sellerId = sellerId,
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }

        composable(route = Screen.CreateStore.route) {
            CreateStoreScreen(
                onNavigateHome = {
                    navController.navigate(Screen.AddProduct.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWishlist = { navController.navigate(Screen.Wishlist.route) },
                onNavigateToCreateStore = { navController.navigate(Screen.CreateStore.route) },
                onNavigateToStoreManagement = { navController.navigate(Screen.StoreManagement.route) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                onNavigateToOrders = { navController.navigate(Screen.Orders.route) },
                onNavigateToSellerProfile = { sellerId ->
                    navController.navigate(Screen.SellerProfile.createRoute(sellerId))
                },
                onSignOut = {
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Orders.route) {
            OrdersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.AddProduct.route) {
            AddProductScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateStore = {
                    navController.navigate(Screen.CreateStore.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(route = Screen.PostDirectAd.route) {
            com.example.features.marketplace.PostDirectAdScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.StoreManagement.route) {
            StoreManagementScreen(
                onBack = { navController.popBackStack() },
                onEditProduct = { product ->
                    // For now, we can just log or show a toast as real editing requires a complex dialog/screen
                    // But I'll implement a basic add/edit logic in the screen itself
                }
            )
        }

        composable(route = Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId))
                }
            )
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
