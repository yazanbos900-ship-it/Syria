package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
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

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val language = LanguageManager.getLanguage(newBase)
    super.attachBaseContext(LanguageManager.updateBaseContextLocale(newBase, language))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
        val sharedPrefs = getSharedPreferences("waset_crash", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("last_crash", ex.stackTraceToString()).commit()
        defaultHandler?.uncaughtException(thread, ex)
    }
    
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

        val crashPrefs = getSharedPreferences("waset_crash", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)

        if (lastCrash != null) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                androidx.compose.material3.Text("CRASH DETECTED", color = androidx.compose.ui.graphics.Color.Red, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                androidx.compose.material3.Text(lastCrash, color = androidx.compose.ui.graphics.Color.White)
                androidx.compose.material3.Button(onClick = { crashPrefs.edit().remove("last_crash").apply() }) {
                    androidx.compose.material3.Text("Clear Crash & Reload")
                }
            }
        } else {
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
}

