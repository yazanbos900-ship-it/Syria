package com.example.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Authentication : Screen("auth")
    object Home : Screen("home")
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object StoreDetail : Screen("store_detail/{storeId}") {
        fun createRoute(storeId: String) = "store_detail/$storeId"
    }
    object CategoryProducts : Screen("category_products/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String) = "category_products/$categoryId/$categoryName"
    }
    object Cart : Screen("cart")
    object Search : Screen("search")
    object Checkout : Screen("checkout")
    object Orders : Screen("orders")
    object Profile : Screen("profile")
    object Wishlist : Screen("wishlist")
    object CreateStore : Screen("create_store")
    object StoreManagement : Screen("store_management")
}
