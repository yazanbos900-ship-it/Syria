package com.example.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.features.marketplace.SharedCartState
import com.example.navigation.Screen
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val titleEn: String,
    val titleAr: String,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val coroutineScope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val cartItemsCount = remember {
        derivedStateOf {
            SharedCartState.cartItems.sumOf { it.quantity }
        }
    }

    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            titleEn = "Home",
            titleAr = "الرئيسية",
            testTag = "nav_home"
        ),
        BottomNavItem(
            route = Screen.Search.route,
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search,
            titleEn = "Search",
            titleAr = "البحث",
            testTag = "nav_search"
        ),
        BottomNavItem(
            route = "sell_trigger", // Custom trigger
            selectedIcon = Icons.Filled.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircle,
            titleEn = "Sell",
            titleAr = "بيع",
            testTag = "nav_sell"
        ),
        BottomNavItem(
            route = Screen.Cart.route,
            selectedIcon = Icons.Filled.ShoppingCart,
            unselectedIcon = Icons.Outlined.ShoppingCart,
            titleEn = "Cart",
            titleAr = "السلة",
            testTag = "nav_cart"
        ),
        BottomNavItem(
            route = Screen.Profile.route,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            titleEn = "Profile",
            titleAr = "حسابي",
            testTag = "nav_profile"
        )
    )

    NavigationBar(
        containerColor = BrandSurface,
        contentColor = BrandTextPrimary,
        modifier = modifier.testTag("bottom_navigation_bar")
    ) {
        items.forEach { item ->
            val isSelected = when (item.route) {
                "sell_trigger" -> currentRoute == Screen.AddProduct.route || currentRoute == Screen.CreateStore.route
                else -> currentRoute == item.route
            }

            val labelText = if (isArabic) item.titleAr else item.titleEn

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (item.route == "sell_trigger") {
                        coroutineScope.launch {
                            val user = ServiceLocator.authRepository.getCurrentUserSession()
                            if (user != null) {
                                val ownStore = ServiceLocator.storeRepository.getStoreByOwnerId(user.id)
                                if (ownStore != null) {
                                    navController.navigate(Screen.AddProduct.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate(Screen.CreateStore.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else {
                                // Fallback to auth if no user logged in
                                navController.navigate(Screen.Authentication.route)
                            }
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item.route == Screen.Cart.route && cartItemsCount.value > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFF1DB954),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = cartItemsCount.value.toString(),
                                        modifier = Modifier.testTag("cart_badge_count")
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = labelText
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = labelText
                        )
                    }
                },
                label = {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1DB954), // Active indicator color specified: #1DB954
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF1DB954),
                    unselectedIconColor = BrandTextMuted,
                    unselectedTextColor = BrandTextMuted
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
