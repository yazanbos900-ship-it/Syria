package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.di.ServiceLocator
import com.example.domain.model.Store
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary

import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    onBack: () -> Unit
) {
    var store by remember { mutableStateOf<Store?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(storeId) {
        store = ServiceLocator.storeRepository.getStoreById(storeId)
        Log.d("STORE_DEBUG", "load storeId=$storeId exists=${store != null}")
        isLoading = false
    }

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = store?.name ?: "تفاصيل المتجر", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 100.dp))
            } else if (store != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Store Logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(BrandSoftGray)
                ) {
                    AsyncImage(
                        model = store?.logoUrl,
                        contentDescription = store?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Store Name
                Text(
                    text = store?.name ?: "Unnamed Store",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    textAlign = TextAlign.Center
                )
                
                // Followers / Status
                Text(
                    text = "متابعين: ${store?.followersCount ?: 0}",
                    fontSize = 14.sp,
                    color = BrandTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Description
                Text(
                    text = store?.description ?: "لا يوجد وصف",
                    fontSize = 16.sp,
                    color = BrandTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            } else {
                Text(
                    text = "لم يتم العثور على المتجر",
                    color = BrandTextMuted,
                    modifier = Modifier.padding(top = 100.dp)
                )
            }
        }
    }
}
