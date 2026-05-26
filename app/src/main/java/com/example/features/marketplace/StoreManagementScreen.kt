package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.components.*
import com.example.core.di.ServiceLocator
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(
    onBack: () -> Unit,
    onEditProduct: (Product) -> Unit
) {
    val viewModel: StoreManagementViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StoreManagementViewModel(
                ServiceLocator.storeRepository,
                ServiceLocator.productRepository,
                ServiceLocator.authRepository
            ) as T
        }
    })

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStoreAndProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Store Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Product")
            }
        },
        containerColor = BrandBackground
    ) { innerPadding ->
        if (state.isLoading && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (state.error != null && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadStoreAndProducts() }) { Text("Retry") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Store Summary Header
                item {
                    state.store?.let { store ->
                        StoreHeaderCard(store)
                    }
                }

                item {
                    Text(
                        "Manage Products",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (state.products.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No products yet. Tap + to add one.", color = BrandTextMuted)
                        }
                    }
                } else {
                    items(state.products) { product ->
                        ProductManagementItem(
                            product = product,
                            onEdit = { onEditProduct(product) },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onAdd = { title, price, desc, category ->
                viewModel.addProduct(title, price, desc, emptyList(), category)
                showAddProductDialog = false
            }
        )
    }

    if (showSettingsDialog && state.store != null) {
        StoreSettingsDialog(
            store = state.store!!,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, desc, categoryId ->
                viewModel.updateStore(name, desc, categoryId, null)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun StoreHeaderCard(store: Store) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = store.logoUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape).background(BrandSoftGray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(store.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(store.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = BrandTextMuted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = BrandPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("${store.followersCount} Followers", fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductManagementItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(BrandSoftGray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$${product.price}", color = BrandPrimary, fontWeight = FontWeight.Bold)
                Text("Stock: ${product.stockCount}", fontSize = 12.sp, color = BrandTextMuted)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    // Filter out "All" category for product creation
    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
                BrandTextField(value = price, onValueChange = { price = it }, placeholder = "Price", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                BrandTextField(value = desc, onValueChange = { desc = it }, placeholder = "Description", singleLine = false, modifier = Modifier.height(100.dp))
                
                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCategory?.name ?: "Select Category", color = BrandTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = BrandTextPrimary)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && priceVal > 0 && selectedCategory != null) {
                        onAdd(title, priceVal, desc, selectedCategory!!.id)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StoreSettingsDialog(
    store: Store,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(store.name) }
    var desc by remember { mutableStateOf(store.description) }
    
    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == store.categoryId } ?: categories.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Store Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandTextField(value = name, onValueChange = { name = it }, placeholder = "Store Name")
                BrandTextField(value = desc, onValueChange = { desc = it }, placeholder = "Description", singleLine = false, modifier = Modifier.height(100.dp))
                
                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCategory?.name ?: "Select Category", color = BrandTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = BrandTextPrimary)
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && desc.isNotBlank() && selectedCategory != null) {
                        onSave(name, desc, selectedCategory!!.id)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
