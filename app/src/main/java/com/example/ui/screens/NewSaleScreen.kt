package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerEntity
import com.example.data.model.SaleOrderEntity
import com.example.data.model.SaleOrderItem
import com.example.ui.components.CustomerFormDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    customers: List<CustomerEntity>,
    onProcessSale: (
        customer: CustomerEntity,
        items: List<SaleOrderItem>,
        paidAmount: Double,
        paymentMethod: String,
        discountAmount: Double,
        taxRatePercent: Double,
        notes: String,
        onSuccess: (SaleOrderEntity) -> Unit
    ) -> Unit,
    onViewInvoice: (SaleOrderEntity) -> Unit,
    onAddCustomer: (CustomerEntity) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCreateCustomerDialog by remember { mutableStateOf(false) }

    var cartItems by remember { mutableStateOf(listOf<SaleOrderItem>()) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    var discountAmountText by remember { mutableStateOf("") }
    var taxRateText by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }
    var isFullPaymentSelected by remember { mutableStateOf(true) }

    val subtotal = remember(cartItems) { cartItems.sumOf { it.subtotal } }
    val discount = discountAmountText.toDoubleOrNull() ?: 0.0
    val taxRate = taxRateText.toDoubleOrNull() ?: 0.0
    val taxAmount = ((subtotal - discount).coerceAtLeast(0.0)) * (taxRate / 100.0)
    val grandTotal = (subtotal - discount + taxAmount).coerceAtLeast(0.0)

    val paidAmount = if (isFullPaymentSelected) grandTotal else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val balanceDue = (grandTotal - paidAmount).coerceAtLeast(0.0)

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val paymentMethods = listOf("Cash", "Bank Transfer", "Credit Card", "Cheque", "Credit / Due")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {
            // Master-Detail 2-Pane layout on Tablets / Wide screens
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left Column: Items Cart & Product Selector
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    CartHeaderSection(
                        cartItemCount = cartItems.sumOf { it.quantity },
                        onClearCart = { cartItems = emptyList() },
                        onOpenProductPicker = { showAddItemDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cartItems.isEmpty()) {
                        EmptyCartPlaceholder(onAddProducts = { showAddItemDialog = true })
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cartItems, key = { it.productName }) { item ->
                                CartItemCard(
                                    item = item,
                                    onIncrease = {
                                        cartItems = cartItems.map {
                                            if (it.productName == item.productName) {
                                                val newQ = it.quantity + 1
                                                it.copy(quantity = newQ, subtotal = newQ * it.unitPrice)
                                            } else it
                                        }
                                    },
                                    onDecrease = {
                                        if (item.quantity > 1) {
                                            cartItems = cartItems.map {
                                                if (it.productName == item.productName) {
                                                    val newQ = it.quantity - 1
                                                    it.copy(quantity = newQ, subtotal = newQ * it.unitPrice)
                                                } else it
                                            }
                                        } else {
                                            cartItems = cartItems.filter { it.productName != item.productName }
                                        }
                                    },
                                    onRemove = {
                                        cartItems = cartItems.filter { it.productName != item.productName }
                                    }
                                )
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Right Column: Customer & Checkout Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CheckoutControls(
                        selectedCustomer = selectedCustomer,
                        onOpenCustomerPicker = { showCustomerPicker = true },
                        onClearCustomer = { selectedCustomer = null },
                        paymentMethods = paymentMethods,
                        selectedMethod = paymentMethod,
                        onSelectMethod = { paymentMethod = it },
                        discountText = discountAmountText,
                        onDiscountChange = { discountAmountText = it },
                        taxRateText = taxRateText,
                        onTaxRateChange = { taxRateText = it },
                        isFullPayment = isFullPaymentSelected,
                        onToggleFullPayment = {
                            isFullPaymentSelected = it
                            if (it) paidAmountText = ""
                        },
                        paidText = paidAmountText,
                        onPaidChange = {
                            paidAmountText = it
                            isFullPaymentSelected = false
                        },
                        notes = notes,
                        onNotesChange = { notes = it },
                        subtotal = subtotal,
                        discount = discount,
                        taxAmount = taxAmount,
                        grandTotal = grandTotal,
                        paidAmount = paidAmount,
                        balanceDue = balanceDue,
                        errorMessage = errorMessage,
                        isProcessing = isProcessing,
                        onCompleteSale = {
                            if (selectedCustomer == null) {
                                errorMessage = "Please select or add a customer"
                                return@CheckoutControls
                            }
                            if (cartItems.isEmpty()) {
                                errorMessage = "Please add at least one product to the sale"
                                return@CheckoutControls
                            }
                            if (paidAmount < 0) {
                                errorMessage = "Paid amount cannot be negative"
                                return@CheckoutControls
                            }

                            errorMessage = null
                            isProcessing = true
                            onProcessSale(
                                selectedCustomer!!,
                                cartItems,
                                paidAmount,
                                paymentMethod,
                                discount,
                                taxRate,
                                notes
                            ) { createdSale ->
                                isProcessing = false
                                onViewInvoice(createdSale)
                            }
                        }
                    )
                }
            }
        } else {
            // Portrait / Compact Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Customer Selector Banner
                CustomerSelectBanner(
                    selectedCustomer = selectedCustomer,
                    onOpenPicker = { showCustomerPicker = true },
                    onClear = { selectedCustomer = null }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cart Header
                CartHeaderSection(
                    cartItemCount = cartItems.sumOf { it.quantity },
                    onClearCart = { cartItems = emptyList() },
                    onOpenProductPicker = { showAddItemDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (cartItems.isEmpty()) {
                    EmptyCartPlaceholder(onAddProducts = { showAddItemDialog = true })
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cartItems.forEach { item ->
                            CartItemCard(
                                item = item,
                                onIncrease = {
                                    cartItems = cartItems.map {
                                        if (it.productName == item.productName) {
                                            val newQ = it.quantity + 1
                                            it.copy(quantity = newQ, subtotal = newQ * it.unitPrice)
                                        } else it
                                    }
                                },
                                onDecrease = {
                                    if (item.quantity > 1) {
                                        cartItems = cartItems.map {
                                            if (it.productName == item.productName) {
                                                val newQ = it.quantity - 1
                                                it.copy(quantity = newQ, subtotal = newQ * it.unitPrice)
                                            } else it
                                        }
                                    } else {
                                        cartItems = cartItems.filter { it.productName != item.productName }
                                    }
                                },
                                onRemove = {
                                    cartItems = cartItems.filter { it.productName != item.productName }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Checkout Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CheckoutControls(
                            selectedCustomer = selectedCustomer,
                            onOpenCustomerPicker = { showCustomerPicker = true },
                            onClearCustomer = { selectedCustomer = null },
                            paymentMethods = paymentMethods,
                            selectedMethod = paymentMethod,
                            onSelectMethod = { paymentMethod = it },
                            discountText = discountAmountText,
                            onDiscountChange = { discountAmountText = it },
                            taxRateText = taxRateText,
                            onTaxRateChange = { taxRateText = it },
                            isFullPayment = isFullPaymentSelected,
                            onToggleFullPayment = {
                                isFullPaymentSelected = it
                                if (it) paidAmountText = ""
                            },
                            paidText = paidAmountText,
                            onPaidChange = {
                                paidAmountText = it
                                isFullPaymentSelected = false
                            },
                            notes = notes,
                            onNotesChange = { notes = it },
                            subtotal = subtotal,
                            discount = discount,
                            taxAmount = taxAmount,
                            grandTotal = grandTotal,
                            paidAmount = paidAmount,
                            balanceDue = balanceDue,
                            errorMessage = errorMessage,
                            isProcessing = isProcessing,
                            showCustomerSection = false,
                            onCompleteSale = {
                                if (selectedCustomer == null) {
                                    errorMessage = "Please select or add a customer"
                                    return@CheckoutControls
                                }
                                if (cartItems.isEmpty()) {
                                    errorMessage = "Please add at least one product to the sale"
                                    return@CheckoutControls
                                }
                                if (paidAmount < 0) {
                                    errorMessage = "Paid amount cannot be negative"
                                    return@CheckoutControls
                                }

                                errorMessage = null
                                isProcessing = true
                                onProcessSale(
                                    selectedCustomer!!,
                                    cartItems,
                                    paidAmount,
                                    paymentMethod,
                                    discount,
                                    taxRate,
                                    notes
                                ) { createdSale ->
                                    isProcessing = false
                                    onViewInvoice(createdSale)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Product Picker Dialog
    if (showAddItemDialog) {
        AddSaleItemDialog(
            onDismiss = { showAddItemDialog = false },
            onProductSelected = { newItem ->
                // Since productId is 0L, use UUID or index for identity if needed, 
                // but since we allow custom items, we just add it to the list.
                // We'll give it a fake productId just so LazyColumn key works, or just don't rely on productId.
                cartItems = cartItems + newItem
                showAddItemDialog = false
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onCustomerSelected = {
                selectedCustomer = it
                showCustomerPicker = false
            },
            onAddNewCustomer = {
                showCustomerPicker = false
                showCreateCustomerDialog = true
            }
        )
    }

    // Quick Add Customer Dialog
    if (showCreateCustomerDialog) {
        CustomerFormDialog(
            initialCustomer = null,
            onDismiss = { showCreateCustomerDialog = false },
            onSave = { newCust ->
                onAddCustomer(newCust)
                selectedCustomer = newCust
                showCreateCustomerDialog = false
            }
        )
    }
}

@Composable
private fun CustomerSelectBanner(
    selectedCustomer: CustomerEntity?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPicker),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedCustomer != null) BrandBluePrimaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BrandBluePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (selectedCustomer != null) {
                    Text(
                        text = selectedCustomer.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandBluePrimary
                    )
                    Text(
                        text = selectedCustomer.phone.ifBlank { "Client Account" },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                } else {
                    Text(
                        text = "Select or Add Customer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap to search or create new client profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                }
            }

            if (selectedCustomer != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Customer", tint = DangerRed)
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTextSecondary)
            }
        }
    }
}

@Composable
private fun CartHeaderSection(
    cartItemCount: Int,
    onClearCart: () -> Unit,
    onOpenProductPicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Order Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (cartItemCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = BrandBluePrimary,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "$cartItemCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cartItemCount > 0) {
                TextButton(onClick = onClearCart) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = DangerRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = DangerRed)
                }
            }
            Button(
                onClick = onOpenProductPicker,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item")
            }
        }
    }
}

@Composable
private fun EmptyCartPlaceholder(onAddProducts: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AddShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No items added to sale yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Select items from inventory to build your invoice",
                style = MaterialTheme.typography.bodySmall,
                color = AppTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddProducts,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Custom Item")
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: SaleOrderItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imageUrl.isNotBlank() && java.io.File(item.imageUrl).exists()) {
                coil.compose.AsyncImage(
                    model = java.io.File(item.imageUrl),
                    contentDescription = item.productName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (item.sku.isNotBlank()) {
                    Text("SKU: ${item.sku}", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                }
                Text("Unit: Rs. ${"%,.2f".format(item.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = BrandBluePrimary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            "${item.quantity}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Rs. ${"%,.2f".format(item.subtotal)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutControls(
    selectedCustomer: CustomerEntity?,
    onOpenCustomerPicker: () -> Unit,
    onClearCustomer: () -> Unit,
    paymentMethods: List<String>,
    selectedMethod: String,
    onSelectMethod: (String) -> Unit,
    discountText: String,
    onDiscountChange: (String) -> Unit,
    taxRateText: String,
    onTaxRateChange: (String) -> Unit,
    isFullPayment: Boolean,
    onToggleFullPayment: (Boolean) -> Unit,
    paidText: String,
    onPaidChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    subtotal: Double,
    discount: Double,
    taxAmount: Double,
    grandTotal: Double,
    paidAmount: Double,
    balanceDue: Double,
    errorMessage: String?,
    isProcessing: Boolean,
    showCustomerSection: Boolean = true,
    onCompleteSale: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showCustomerSection) {
            Text("Client Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            CustomerSelectBanner(
                selectedCustomer = selectedCustomer,
                onOpenPicker = onOpenCustomerPicker,
                onClear = onClearCustomer
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Payment & Billing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Payment Method Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paymentMethods.take(3).forEach { method ->
                val isSelected = selectedMethod == method
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMethod(method) },
                    label = { Text(method, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimaryContainer,
                        selectedLabelColor = BrandBluePrimary
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paymentMethods.drop(3).forEach { method ->
                val isSelected = selectedMethod == method
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMethod(method) },
                    label = { Text(method, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimaryContainer,
                        selectedLabelColor = BrandBluePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Discount and Tax Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = discountText,
                onValueChange = onDiscountChange,
                label = { Text("Discount (Rs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = taxRateText,
                onValueChange = onTaxRateChange,
                label = { Text("Tax Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Paid Amount Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Full Payment Received", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = isFullPayment,
                onCheckedChange = onToggleFullPayment,
                colors = SwitchDefaults.colors(checkedThumbColor = BrandBluePrimary)
            )
        }

        if (!isFullPayment) {
            OutlinedTextField(
                value = paidText,
                onValueChange = onPaidChange,
                label = { Text("Partial Amount Paid (Rs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes / Invoice Terms (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Calculations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SummaryRow(label = "Subtotal", value = "Rs. ${"%,.2f".format(subtotal)}")
                if (discount > 0) {
                    SummaryRow(label = "Discount", value = "- Rs. ${"%,.2f".format(discount)}", valueColor = DangerRed)
                }
                if (taxAmount > 0) {
                    SummaryRow(label = "Tax", value = "+ Rs. ${"%,.2f".format(taxAmount)}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryRow(
                    label = "Grand Total",
                    value = "Rs. ${"%,.2f".format(grandTotal)}",
                    isBold = true,
                    fontSize = 18.sp,
                    valueColor = BrandBluePrimary
                )
                SummaryRow(
                    label = "Amount Paid",
                    value = "Rs. ${"%,.2f".format(paidAmount)}",
                    valueColor = SuccessGreen
                )
                if (balanceDue > 0) {
                    SummaryRow(
                        label = "Balance Due",
                        value = "Rs. ${"%,.2f".format(balanceDue)}",
                        isBold = true,
                        valueColor = DangerRed
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = DangerRed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = DangerRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCompleteSale,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_sale_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing Sale...")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete & View Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else AppTextSecondary
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
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
}


@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onCustomerSelected: (CustomerEntity) -> Unit,
    onAddNewCustomer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.phone.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.city.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone, city...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAddNewCustomer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Customer Profile")
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No customers found", color = AppTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id }) { cust ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCustomerSelected(cust) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(BrandBluePrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBluePrimary,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(cust.phone.ifBlank { cust.city }, style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                                    }

                                    if (cust.outstandingBalance > 0) {
                                        Text(
                                            "Due: Rs. ${"%,.0f".format(cust.outstandingBalance)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DangerRed,
                                            fontWeight = FontWeight.Bold
                                        )
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
