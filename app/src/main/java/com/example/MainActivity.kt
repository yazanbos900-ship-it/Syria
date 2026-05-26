package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.core.di.ServiceLocator
import com.example.features.marketplace.SharedCartState
import com.example.features.marketplace.SharedWishlistState
import com.example.navigation.NavigationGraph
import com.example.ui.theme.WasetPlusTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Clean Architecture dependency nodes and dynamic Firebase instances
    ServiceLocator.init(applicationContext)
    
    SharedCartState.init(ServiceLocator.authRepository, ServiceLocator.cartRepository, lifecycleScope)
    SharedWishlistState.init(ServiceLocator.authRepository, ServiceLocator.wishlistRepository, lifecycleScope)
    com.example.features.marketplace.SharedFilterState.init(ServiceLocator.productRepository, lifecycleScope)

    enableEdgeToEdge()
    setContent {
      WasetPlusTheme {
        val navController = rememberNavController()
        NavigationGraph(navController = navController)
      }
    }
  }
}

