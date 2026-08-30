package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ProductEntity
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.InventoryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    products: List<ProductEntity>,
    onSaveProduct: (ProductEntity) -> Unit = {},
    onDeleteProduct: (ProductEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var filterLowStockOnly by remember { mutableStateOf(false) }

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToViewDetails by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, filterLowStockOnly) {
        products.filter { prod ->
            val matchQuery = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchLowStock = !filterLowStockOnly || prod.currentStock <= prod.minStock

            matchQuery && matchCategory && matchLowStock
        }
    }

    val totalUniqueProducts = products.size
    val totalStock = products.sumOf { it.currentStock }
    val lowStockCount = products.count { it.currentStock <= it.minStock }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddProductDialog = true
                },
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Metric Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandBluePrimaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Items", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalUniqueProducts Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Stock On Hand", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalStock Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }

                if (lowStockCount > 0) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRedContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Low Stock Alert", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$lowStockCount Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DangerRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product, SKU, category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_search_input")
                )

                Button(
                    onClick = {
                        productToEdit = null
                        showAddProductDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("add_product_header_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category & Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterLowStockOnly,
                    onClick = { filterLowStockOnly = !filterLowStockOnly },
                    label = { Text("Low Stock Alert") },
                    leadingIcon = {
                        if (filterLowStockOnly) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DangerRedContainer,
                        selectedLabelColor = DangerRed
                    ),
                    modifier = Modifier.testTag("low_stock_filter_chip")
                )

                Text(
                    text = "${filteredProducts.size} Items",
                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = AppTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No Products Registered" else "No matching products found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Tap '+ Add' to register your inventory items." else "Try modifying your search filter.",
                            style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary)
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    productToEdit = null
                                    showAddProductDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Product")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val isLowStock = prod.currentStock <= prod.minStock
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { productToViewDetails = prod }
                                .testTag("product_card_${prod.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Image thumbnail with fallback
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BrandBluePrimary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageValid = prod.imageUrl.isNotBlank() && (
                                            prod.imageUrl.startsWith("http") ||
                                            prod.imageUrl.startsWith("content:") ||
                                            File(prod.imageUrl).exists()
                                    )
                                    if (imageValid) {
                                        AsyncImage(
                                            model = prod.imageUrl,
                                            contentDescription = prod.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "SKU: ${prod.sku.ifBlank { "N/A" }} • ${prod.category.ifBlank { "General" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isLowStock) DangerRedContainer else SuccessGreenContainer
                                        ) {
                                            Text(
                                                text = "Stock: ${prod.currentStock} Units",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isLowStock) DangerRed else Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (prod.purchasePrice > 0.0) {
                                            Text(
                                                text = "Cost: ${formatCurrency(prod.purchasePrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppTextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(prod.sellingPrice),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        IconButton(
                                            onClick = {
                                                productToEdit = prod
                                                showAddProductDialog = true
                                            },
                                            modifier = Modifier.size(32.dp).testTag("product_edit_btn_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { productToDelete = prod },
                                            modifier = Modifier.size(32.dp).testTag("product_delete_btn_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= PRODUCT DIALOGS =================

    if (showAddProductDialog) {
        ProductFormDialog(
            initialProduct = productToEdit,
            onDismiss = { showAddProductDialog = false },
            onSave = { savedProd ->
                onSaveProduct(savedProd)
                showAddProductDialog = false
            }
        )
    }

    if (productToViewDetails != null) {
        ProductDetailsDialog(
            product = productToViewDetails!!,
            onDismiss = { productToViewDetails = null },
            onEdit = {
                productToEdit = productToViewDetails
                productToViewDetails = null
                showAddProductDialog = true
            },
            onDelete = {
                productToDelete = productToViewDetails
                productToViewDetails = null
            }
        )
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to delete '${productToDelete!!.name}'? This item will be removed from inventory.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(productToDelete!!)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_delete_product_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Full Add/Edit Product Dialog with Image Upload, Replace, and Remove
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
    var costPriceText by remember { mutableStateOf(if (initialProduct != null && initialProduct.purchasePrice > 0) initialProduct.purchasePrice.toString() else "") }
    var sellingPriceText by remember { mutableStateOf(if (initialProduct != null && initialProduct.sellingPrice > 0) initialProduct.sellingPrice.toString() else "") }
    var currentStockText by remember { mutableStateOf(if (initialProduct != null) initialProduct.currentStock.toString() else "0") }
    var minStockText by remember { mutableStateOf(if (initialProduct != null) initialProduct.minStock.toString() else "5") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = InventoryViewModel.saveImageUriToAppStorage(context, uri, imageUrl)
            imageUrl = savedPath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "Add New Product" else "Edit Product",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error != null) {
                    Surface(
                        color = DangerRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error!!,
                            color = DangerRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Image Upload Area
                Text("Product Image", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandBluePrimary.copy(alpha = 0.08f))
                            .border(1.dp, AppOutlineLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageValid = imageUrl.isNotBlank() && (
                                imageUrl.startsWith("http") ||
                                imageUrl.startsWith("content:") ||
                                File(imageUrl).exists()
                        )
                        if (imageValid) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Product Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(32.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("upload_product_image_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (imageUrl.isNotBlank()) "Replace" else "Upload")
                            }

                            if (imageUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { imageUrl = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed),
                                    modifier = Modifier.testTag("remove_product_image_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }
                        Text("PNG, JPG supported (saved permanently)", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                    }
                }

                HorizontalDivider(color = AppOutlineLight)

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Wireless Mouse") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                // SKU & Category
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Code") },
                        placeholder = { Text("Auto / Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_sku_input")
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("Electronics, etc.") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_category_input")
                    )
                }

                // Prices
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Purchase / Cost Price (Rs.)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_cost_input")
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it; error = null },
                        label = { Text("Selling Price * (Rs.)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_price_input")
                    )
                }

                // Stock & Min Stock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStockText,
                        onValueChange = { currentStockText = it },
                        label = { Text("Stock Qty *") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_stock_input")
                    )

                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it },
                        label = { Text("Min Alert") },
                        placeholder = { Text("5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("product_min_stock_input")
                    )
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Product specifications or notes...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("product_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Product name is required."
                        return@Button
                    }
                    val sellingPrice = sellingPriceText.toDoubleOrNull()
                    if (sellingPrice == null || sellingPrice <= 0.0) {
                        error = "Please enter a valid selling price greater than 0."
                        return@Button
                    }
                    val stock = currentStockText.toIntOrNull() ?: 0
                    if (stock < 0) {
                        error = "Stock cannot be negative."
                        return@Button
                    }
                    val minStock = minStockText.toIntOrNull() ?: 5
                    if (minStock < 0) {
                        error = "Min stock cannot be negative."
                        return@Button
                    }
                    val purchasePrice = costPriceText.toDoubleOrNull() ?: 0.0
                    val finalSku = if (sku.isBlank()) "PRD-${System.currentTimeMillis() % 100000}" else sku.trim()

                    val product = ProductEntity(
                        id = initialProduct?.id ?: 0L,
                        name = name.trim(),
                        sku = finalSku,
                        category = category.trim().ifBlank { "General" },
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        currentStock = stock,
                        minStock = minStock,
                        imageUrl = imageUrl.trim(),
                        description = description.trim(),
                        createdAt = initialProduct?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(product)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (initialProduct == null) "Save Product" else "Update Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Product Details View Modal
 */
@Composable
fun ProductDetailsDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.currentStock <= product.minStock

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Product Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isLowStock) DangerRedContainer else SuccessGreenContainer
                ) {
                    Text(
                        text = if (isLowStock) "LOW STOCK" else "IN STOCK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isLowStock) DangerRed else Color(0xFF065F46),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Image banner if available
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBluePrimary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageValid = product.imageUrl.isNotBlank() && (
                            product.imageUrl.startsWith("http") ||
                            product.imageUrl.startsWith("content:") ||
                            File(product.imageUrl).exists()
                    )
                    if (imageValid) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(56.dp))
                    }
                }

                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                if (product.description.isNotBlank()) {
                    Text(product.description, style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                }

                HorizontalDivider(color = AppOutlineLight)

                // Key specifications table
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SKU Code:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text(product.sku.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text(product.category.ifBlank { "General" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Stock:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text("${product.currentStock} Units", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isLowStock) DangerRed else SuccessGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min Stock Threshold:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text("${product.minStock} Units", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Purchase Price:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text(formatCurrency(product.purchasePrice), style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Selling Price:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Text(formatCurrency(product.sellingPrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
