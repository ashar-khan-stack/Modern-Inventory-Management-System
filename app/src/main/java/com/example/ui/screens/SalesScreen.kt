package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.OrderJsonParser
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.util.ValidationUtils
import com.example.ui.viewmodel.InventoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private fun formatCurrency(amount: Double): String {
    return String.format("Rs. %,.2f", amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    onNavigateToNewSale: () -> Unit,
    customers: List<CustomerEntity>,
    pastSales: List<SaleOrderEntity>,
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
    onAddCustomer: (CustomerEntity) -> Unit,
    onDeleteSale: (SaleOrderEntity) -> Unit = {},
    onUpdateSalePayment: (sale: SaleOrderEntity, additionalAmount: Double, onSuccess: (SaleOrderEntity) -> Unit) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomerFilter by remember { mutableStateOf<CustomerEntity?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var showNewSaleDialog by remember { mutableStateOf(false) }
    var saleToDelete by remember { mutableStateOf<SaleOrderEntity?>(null) }
    var saleToEditPayment by remember { mutableStateOf<SaleOrderEntity?>(null) }

    val matchingCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            customers.filter { customer ->
                customer.name.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    // Summary Metrics
    val totalRevenue = remember(pastSales) { pastSales.sumOf { it.grandTotal } }
    val totalPaid = remember(pastSales) { pastSales.sumOf { it.paidAmount } }
    val totalDue = remember(pastSales) { pastSales.sumOf { it.remainingBalance } }

    val filteredSales = remember(pastSales, searchQuery, selectedCustomerFilter, selectedStatusFilter) {
        pastSales.filter { sale ->
            val matchesCustomer = when {
                selectedCustomerFilter != null -> {
                    sale.customerId == selectedCustomerFilter!!.id ||
                            sale.customerName.contains(selectedCustomerFilter!!.name, ignoreCase = true)
                }
                searchQuery.isNotBlank() -> {
                    sale.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
                            sale.invoiceNumber.contains(searchQuery.trim(), ignoreCase = true)
                }
                else -> true
            }

            val matchesStatus = when (selectedStatusFilter) {
                "All" -> true
                "Paid" -> sale.paymentStatus.equals("Paid", ignoreCase = true)
                "Partial" -> sale.paymentStatus.equals("Partial", ignoreCase = true)
                "Unpaid" -> sale.paymentStatus.equals("Unpaid", ignoreCase = true)
                else -> true
            }

            matchesCustomer && matchesStatus
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Metrics Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Sales", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatCurrency(totalRevenue),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                            )
                            Text("${pastSales.size} Invoices", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary, fontSize = 10.sp))
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Collected (Paid)", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatCurrency(totalPaid),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                            )
                            Text("Cash & Digital", style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontSize = 10.sp))
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Outstanding Due", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatCurrency(totalDue),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDue > 0) DangerRed else AppTextSecondary
                                )
                            )
                            Text("Receivables", style = MaterialTheme.typography.labelSmall.copy(color = if (totalDue > 0) DangerRed else AppTextSecondary, fontSize = 10.sp))
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                selectedCustomerFilter = null
                                showSuggestions = it.isNotBlank()
                            },
                            placeholder = {
                                Text(
                                    "Search by customer or invoice",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = AppTextSecondary, fontSize = 13.sp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotEmpty() || selectedCustomerFilter != null) BrandBluePrimary else AppTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty() || selectedCustomerFilter != null) {
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            selectedCustomerFilter = null
                                            showSuggestions = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = AppTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = AppOutlineLight,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = AppSurfaceVariantLight.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sales_search_input")
                        )

                        AnimatedVisibility(
                            visible = showSuggestions && searchQuery.isNotBlank()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AppOutlineLight),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    if (matchingCustomers.isNotEmpty()) {
                                        matchingCustomers.take(5).forEach { customer ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        searchQuery = customer.name
                                                        selectedCustomerFilter = customer
                                                        showSuggestions = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandBluePrimary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = customer.name.take(1).uppercase(Locale.US),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandBluePrimary,
                                                            fontSize = 13.sp
                                                        )
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = customer.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filters = listOf("All", "Paid", "Partial", "Unpaid")
                            filters.forEach { filter ->
                                FilterChip(
                                    selected = selectedStatusFilter == filter,
                                    onClick = { selectedStatusFilter = filter },
                                    label = { Text(filter, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sales Orders & Invoices (${filteredSales.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
            }

            if (filteredSales.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = if (pastSales.isEmpty()) "No sales recorded yet. Tap '+' to create your first sale." else "No sales matching your filter criteria.",
                        actionLabel = "New Sale",
                        onAction = { onNavigateToNewSale() }
                    )
                }
            } else {
                items(filteredSales, key = { it.id }) { sale ->
                    val itemsCount = remember(sale.itemsJson) {
                        OrderJsonParser.jsonToSaleItems(sale.itemsJson).sumOf { it.quantity }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_card_${sale.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandBluePrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            sale.invoiceNumber,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandNavySecondary)
                                        )
                                        Text(
                                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(sale.createdAt)),
                                            style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                }
                                StatusBadge(status = sale.paymentStatus)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppOutlineLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            sale.customerName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Payment: ${sale.paymentMethod} • $itemsCount items",
                                        style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 11.sp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            "Paid: ${formatCurrency(sale.paidAmount)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        )
                                        if (sale.remainingBalance > 0.0) {
                                            Text(
                                                "Balance: ${formatCurrency(sale.remainingBalance)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Total",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary, fontSize = 10.sp)
                                    )
                                    Text(
                                        formatCurrency(sale.grandTotal),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = AppOutlineLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { saleToEditPayment = sale },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBluePrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("edit_sale_${sale.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = { saleToDelete = sale },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("delete_sale_${sale.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }

                                FilledTonalButton(
                                    onClick = { onViewInvoice(sale) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = BrandBluePrimaryContainer, contentColor = BrandBluePrimary),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(34.dp)
                                        .testTag("view_invoice_${sale.id}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Invoice", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNavigateToNewSale() },
            containerColor = BrandBluePrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_sale_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Sale", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Sale", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = {
                Text(
                    "Delete Sale?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this sale?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saleToDelete?.let { onDeleteSale(it) }
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_sale_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { saleToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_sale_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (saleToEditPayment != null) {
        val targetSale = saleToEditPayment!!
        EditPaymentSettlementDialog(
            sale = targetSale,
            onDismiss = { saleToEditPayment = null },
            onSavePayment = { additionalAmount ->
                onUpdateSalePayment(targetSale, additionalAmount) { _ ->
                    saleToEditPayment = null
                }
            }
        )
    }
}

/**
 * Payment Settlement Dialog for updating payment on an existing sale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPaymentSettlementDialog(
    sale: SaleOrderEntity,
    onDismiss: () -> Unit,
    onSavePayment: (additionalAmount: Double) -> Unit
) {
    var isFullPaymentMode by remember { mutableStateOf(false) }
    var additionalAmountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val remainingBalance = sale.remainingBalance
    val alreadyPaid = sale.paidAmount
    val grandTotal = sale.grandTotal

    val additionalAmount = additionalAmountText.toDoubleOrNull() ?: 0.0
    val newPaidAmount = alreadyPaid + additionalAmount
    val newBalance = (grandTotal - newPaidAmount).coerceAtLeast(0.0)
    val newPaymentStatus = when {
        newBalance <= 0.0 -> "Paid"
        newPaidAmount > 0.0 -> "Partial"
        else -> "Unpaid"
    }

    val isAmountTooHigh = additionalAmount > remainingBalance + 0.01

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Edit Payment Settlement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sale Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Invoice:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(sale.invoiceNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandNavySecondary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(sale.customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = AppOutlineLight)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(formatCurrency(grandTotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Already Paid:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(formatCurrency(alreadyPaid), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Balance:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(
                                formatCurrency(remainingBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance > 0) DangerRed else SuccessGreen
                            )
                        }
                    }
                }

                // Quick Option Chips
                Text("Payment Option", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isFullPaymentMode,
                        onClick = {
                            isFullPaymentMode = true
                            additionalAmountText = if (remainingBalance % 1.0 == 0.0) remainingBalance.toLong().toString() else remainingBalance.toString()
                            errorMessage = null
                        },
                        label = { Text("Full Payment") },
                        leadingIcon = if (isFullPaymentMode) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("full_payment_chip")
                    )

                    FilterChip(
                        selected = !isFullPaymentMode,
                        onClick = {
                            isFullPaymentMode = false
                            additionalAmountText = ""
                            errorMessage = null
                        },
                        label = { Text("Partial Payment") },
                        leadingIcon = if (!isFullPaymentMode) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("partial_payment_chip")
                    )
                }

                // Additional Amount Input Field
                OutlinedTextField(
                    value = additionalAmountText,
                    onValueChange = { input ->
                        additionalAmountText = input.filter { it.isDigit() || it == '.' }
                        val num = additionalAmountText.toDoubleOrNull() ?: 0.0
                        if (num > remainingBalance + 0.01) {
                            errorMessage = "Payment cannot be greater than the remaining balance."
                        } else {
                            errorMessage = null
                        }
                        isFullPaymentMode = (num >= remainingBalance - 0.01 && remainingBalance > 0)
                    },
                    label = { Text("Additional Amount Paid (Rs.) *") },
                    placeholder = { Text("Enter additional payment amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null || isAmountTooHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("additional_payment_input")
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Updated Live Preview
                if (additionalAmount > 0 && !isAmountTooHigh) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandBluePrimaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Updated Summary Preview:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BrandNavySecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New Paid Amount:", style = MaterialTheme.typography.bodySmall)
                                Text(formatCurrency(newPaidAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("New Balance:", style = MaterialTheme.typography.bodySmall)
                                Text(formatCurrency(newBalance), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (newBalance > 0) DangerRed else SuccessGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("New Status:", style = MaterialTheme.typography.bodySmall)
                                StatusBadge(status = newPaymentStatus)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = additionalAmountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        errorMessage = "Please enter a valid payment amount."
                        return@Button
                    }
                    if (amount > remainingBalance + 0.01) {
                        errorMessage = "Payment cannot be greater than the remaining balance."
                        return@Button
                    }
                    onSavePayment(amount)
                },
                enabled = additionalAmount > 0 && !isAmountTooHigh,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_payment_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
