package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
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
    }

    enableEdgeToEdge()
    setContent {
      WasetPlusTheme {
        val navController = rememberNavController()
        NavigationGraph(navController = navController)
      }
    }
  }
}

