package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.core.di.ServiceLocator
import com.example.features.marketplace.SharedCartState
import com.example.features.marketplace.SharedWishlistState
import com.example.navigation.NavigationGraph
import com.example.ui.theme.WasetPlusTheme

import android.content.Context
import com.example.core.utils.LanguageManager
import com.example.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val language = LanguageManager.getLanguage(newBase)
    super.attachBaseContext(LanguageManager.updateBaseContextLocale(newBase, language))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize ThemeManager to load user's theme mode selection
    ThemeManager.init(applicationContext)
    
    // Initialize Clean Architecture dependency nodes and dynamic Firebase instances
    ServiceLocator.init(applicationContext)
    
    SharedCartState.init(ServiceLocator.authRepository, ServiceLocator.cartRepository, lifecycleScope)
    SharedWishlistState.init(ServiceLocator.authRepository, ServiceLocator.wishlistRepository, lifecycleScope)
    com.example.features.marketplace.SharedFilterState.init(ServiceLocator.productRepository, lifecycleScope)
    
    lifecycleScope.launch {
        ServiceLocator.productRepository.seedCategories()
        com.example.data.repository.DatabaseSeeder.seedReviewsOnly()
    }

    enableEdgeToEdge()
    setContent {
      WasetPlusTheme {
        val navController = rememberNavController()
        
        val sharedPrefs = getSharedPreferences("waset_preferences", Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        val firebaseAuth = try {
            com.google.firebase.auth.FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
        val isLoggedIn = firebaseAuth?.currentUser != null
        val startRoute = when {
            isLoggedIn -> com.example.navigation.Screen.Home.route
            onboardingCompleted -> com.example.navigation.Screen.Authentication.route
            else -> com.example.navigation.Screen.Onboarding.route
        }

        // Live track current route for showing/hiding BottomNavigationBar
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showBottomBar = currentRoute in listOf(
            com.example.navigation.Screen.Home.route,
            com.example.navigation.Screen.Search.route,
            com.example.navigation.Screen.Cart.route,
            com.example.navigation.Screen.Profile.route,
            com.example.navigation.Screen.AddProduct.route
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    com.example.components.BottomNavigationBar(navController = navController)
                }
            },
            containerColor = com.example.ui.theme.BrandBackground
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavigationGraph(navController = navController, startDestination = startRoute)
            }
        }
      }
    }
  }
}

