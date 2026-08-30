import re

with open("app/src/main/java/com/example/ui/screens/NewSaleScreen.kt", "r") as f:
    content = f.read()

# Remove ProductEntity import
content = re.sub(r"import com\.example\.data\.model\.ProductEntity\n", "", content)

# Remove products parameter
content = re.sub(r"\s*products: List<ProductEntity>,", "", content)

# Replace "Browse Inventory" with "Add Custom Item"
content = content.replace('"Browse Inventory"', '"Add Custom Item"')
content = content.replace('ProductSearchPickerDialog', 'AddSaleItemDialog')
content = content.replace('onAddProducts = { showProductPicker = true }', 'onAddProducts = { showProductPicker = true }')
content = content.replace('showProductPicker', 'showAddItemDialog')

# Re-write the AddSaleItemDialog to be a custom entry form instead of a list picker
old_dialog_pattern = r"@Composable\s+private fun AddSaleItemDialog.*?\}\s*\}"
new_dialog = """@Composable
private fun AddSaleItemDialog(
    onDismiss: () -> Unit,
    onProductSelected: (SaleOrderItem) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var sellingCostText by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var imageUrl by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val savedPath = com.example.ui.viewmodel.InventoryViewModel.saveImageUriToAppStorage(context, uri)
            imageUrl = savedPath
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Sale Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Image Upload
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = java.io.File(imageUrl),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = AppTextSecondary)
                            Text("Upload Image", color = AppTextSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sellingCostText,
                    onValueChange = { sellingCostText = it },
                    label = { Text("Selling Cost *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Quantity:", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (quantity > 1) quantity-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text("$quantity", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { quantity++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Button(
                    onClick = {
                        val cost = sellingCostText.toDoubleOrNull() ?: 0.0
                        if (productName.isNotBlank() && cost > 0.0) {
                            val item = SaleOrderItem(
                                description = productName,
                                productName = productName,
                                sku = sku,
                                unitPrice = cost,
                                quantity = quantity,
                                subtotal = cost * quantity,
                                imageUrl = imageUrl
                            )
                            onProductSelected(item)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = productName.isNotBlank() && (sellingCostText.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Add Item")
                }
            }
        }
    }
}"""

content = re.sub(old_dialog_pattern, new_dialog, content, flags=re.DOTALL)

# Update the call to AddSaleItemDialog in NewSaleScreen
old_call = """    if (showAddItemDialog) {
        AddSaleItemDialog(
            products = products,
            onDismiss = { showAddItemDialog = false },
            onProductSelected = { prod, qty ->
                val existing = cartItems.find { it.productId == prod.id }
                if (existing != null) {
                    cartItems = cartItems.map {
                        if (it.productId == prod.id) {
                            val newQ = it.quantity + qty
                            it.copy(quantity = newQ, subtotal = newQ * it.unitPrice)
                        } else it
                    }
                } else {
                    cartItems = cartItems + SaleOrderItem(
                        description = prod.name,
                        productName = prod.name,
                        productId = prod.id,
                        sku = prod.sku,
                        unitPrice = prod.sellingPrice,
                        quantity = qty,
                        subtotal = prod.sellingPrice * qty,
                        imageUrl = prod.imageUrl
                    )
                }
                showAddItemDialog = false
            }
        )
    }"""
new_call = """    if (showAddItemDialog) {
        AddSaleItemDialog(
            onDismiss = { showAddItemDialog = false },
            onProductSelected = { newItem ->
                // Since productId is 0L, use UUID or index for identity if needed, 
                // but since we allow custom items, we just add it to the list.
                // We'll give it a fake productId just so LazyColumn key works, or just don't rely on productId.
                val itemWithFakeId = newItem.copy(productId = System.currentTimeMillis() + (0..1000).random())
                cartItems = cartItems + itemWithFakeId
                showAddItemDialog = false
            }
        )
    }"""
content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/ui/screens/NewSaleScreen.kt", "w") as f:
    f.write(content)
