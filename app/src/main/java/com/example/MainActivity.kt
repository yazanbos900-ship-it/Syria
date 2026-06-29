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

import android.content.Intent
import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

  private val deepLinkFlow = MutableSharedFlow<String>(replay = 1)
  private var broadcastListener: com.google.firebase.firestore.ListenerRegistration? = null

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
    
    initializeFcmAndPermissions()

    val initialUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch(e: Exception) { null }
    if (initialUser != null) {
        startBroadcastNotificationsListener(initialUser.uid)
    }

    val initialDeepLink = intent.getStringExtra("deepLink")
    if (!initialDeepLink.isNullOrBlank()) {
        deepLinkFlow.tryEmit(initialDeepLink)
    }

    setContent {
      WasetPlusTheme {
        val navController = rememberNavController()

        val deepLinkRoute by deepLinkFlow.collectAsState(initial = null)
        LaunchedEffect(deepLinkRoute) {
            deepLinkRoute?.let { route ->
                if (route.isNotBlank()) {
                    try {
                        navController.navigate(route)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "DeepLink navigation failed for route $route", e)
                    }
                }
            }
        }
        
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

  private fun initializeFcmAndPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasPermission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    try {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null && token != null) {
                    com.example.core.utils.MyFirebaseMessagingService.updateTokenInFirestore(currentUser.uid, token)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("MainActivity", "FCM setup failed: ${e.localizedMessage}")
    }
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      val deepLink = intent.getStringExtra("deepLink")
      if (!deepLink.isNullOrBlank()) {
          deepLinkFlow.tryEmit(deepLink)
          Log.d("MainActivity", "onNewIntent received deepLink: $deepLink")
      }
  }

  override fun onDestroy() {
      super.onDestroy()
      broadcastListener?.remove()
  }

  private fun startBroadcastNotificationsListener(currentUserId: String) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val appStartTime = System.currentTimeMillis()
    val seenNotificationIds = mutableSetOf<String>()

    lifecycleScope.launch {
        try {
            val userDoc = db.collection("users").document(currentUserId).get().await()
            val isStoreOwner = userDoc.getBoolean("isStoreOwner") ?: false
            
            var storeCity = ""
            var storePlan = ""
            var storeId = ""
            
            if (isStoreOwner) {
                val storeSnap = db.collection("stores").whereEqualTo("ownerId", currentUserId).get().await()
                if (!storeSnap.isEmpty) {
                    val storeDoc = storeSnap.documents.first()
                    storeId = storeDoc.id
                    storeCity = storeDoc.getString("city") ?: ""
                    storePlan = storeDoc.getString("subscriptionTier") ?: "Starter"
                }
            }

            broadcastListener?.remove()
            broadcastListener = db.collection("broadcast_notifications")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("MainActivity", "Error listening to broadcasts", error)
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    for (docChange in snapshots.documentChanges) {
                        if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val doc = docChange.document
                            val id = doc.id
                            val sentAt = doc.getLong("sentAt") ?: 0L
                            
                            if (sentAt >= appStartTime && !seenNotificationIds.contains(id)) {
                                seenNotificationIds.add(id)
                                
                                val title = doc.getString("title") ?: ""
                                val body = doc.getString("body") ?: ""
                                val imageUrl = doc.getString("imageUrl")
                                val deepLink = doc.getString("deepLink")
                                val audience = doc.getString("targetAudience") ?: "all_users"
                                val sentBy = doc.getString("sentBy") ?: ""

                                if (sentBy == currentUserId) continue

                                val isMatch = when {
                                    audience == "all_users_and_stores" || audience == "all_users" -> true
                                    audience == "all_store_owners" && isStoreOwner -> true
                                    audience == "specific_user" && doc.getString("targetUserId") == currentUserId -> true
                                    audience == "specific_store" && doc.getString("targetStoreId") == storeId -> true
                                    audience.startsWith("subscription_plan:") && isStoreOwner -> {
                                        val plan = audience.substringAfter("subscription_plan:")
                                        storePlan.equals(plan, ignoreCase = true)
                                    }
                                    audience.startsWith("location:") -> {
                                        val loc = audience.substringAfter("location:")
                                        storeCity.equals(loc, ignoreCase = true)
                                    }
                                    else -> false
                                }

                                if (isMatch) {
                                    dispatchLocalHeadsUp(title, body, imageUrl, deepLink)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to setup broadcasts listener", e)
        }
    }
  }

  private fun dispatchLocalHeadsUp(title: String, body: String, imageUrl: String?, deepLink: String?) {
    val channelId = "wasetplus_broadcast_notifications"
    val channelName = "Broadcast Notifications"
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "General and administrative broadcast announcements"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (!deepLink.isNullOrBlank()) {
            putExtra("deepLink", deepLink)
        }
    }

    val pendingIntent = PendingIntent.getActivity(
        this,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    )

    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notificationBuilder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(pendingIntent)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))

    if (!imageUrl.isNullOrBlank()) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loader = coil.Coil.imageLoader(this@MainActivity)
                val request = coil.request.ImageRequest.Builder(this@MainActivity)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? coil.request.SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    notificationBuilder.setLargeIcon(bitmap)
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as Bitmap?)
                    )
                }
                notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load notification image: ${e.localizedMessage}")
                notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            }
        }
    } else {
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
  }
}

