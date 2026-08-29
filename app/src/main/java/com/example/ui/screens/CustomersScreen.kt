package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    customers: List<CustomerEntity>,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onSettlePayment: (customerId: Long, amount: Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterBalanceOnly by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToSettle by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    val totalReceivable = remember(customers) { customers.sumOf { it.outstandingBalance } }
    val totalPurchases = remember(customers) { customers.sumOf { it.totalPurchases } }

    val filteredCustomers = remember(customers, searchQuery, filterBalanceOnly) {
        customers.filter { c ->
            val matchQuery = searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery, ignoreCase = true) ||
                    c.email.contains(searchQuery, ignoreCase = true) ||
                    c.city.contains(searchQuery, ignoreCase = true)

            val matchBalance = !filterBalanceOnly || c.outstandingBalance > 0.0
            matchQuery && matchBalance
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Metric Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetricCard(
                    title = "Total Customers",
                    value = "${customers.size}",
                    subtitle = "Lifetime: ${formatCurrency(totalPurchases)}",
                    icon = Icons.Default.People,
                    accentColor = BrandBluePrimary,
                    containerColor = BrandBluePrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Customer Receivables",
                    value = formatCurrency(totalReceivable),
                    subtitle = "Unpaid customer balance",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = if (totalReceivable > 0) DangerRed else SuccessGreen,
                    containerColor = if (totalReceivable > 0) DangerRedContainer else SuccessGreenContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer name, phone, city...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterBalanceOnly,
                    onClick = { filterBalanceOnly = !filterBalanceOnly },
                    label = { Text("Has Outstanding Balance") },
                    leadingIcon = {
                        if (filterBalanceOnly) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                Text("${filteredCustomers.size} Customers", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customer records match search", color = AppTextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCustomers, key = { it.id }) { cust ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                cust.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(cust.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                "${cust.phone}${if (cust.email.isNotBlank()) " • ${cust.email}" else ""}${if (cust.address.isNotBlank() || cust.city.isNotBlank()) " • ${listOf(cust.address, cust.city).filter { it.isNotBlank() }.joinToString(", ")}" else ""}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (cust.outstandingBalance > 0.0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = DangerRedContainer
                                            ) {
                                                Text(
                                                    "Due: ${formatCurrency(cust.outstandingBalance)}",
                                                    color = DangerRed,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SuccessGreenContainer
                                            ) {
                                                Text(
                                                    "Settled",
                                                    color = Color(0xFF065F46),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = AppOutlineLight)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text("Total Sales", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(cust.totalPurchases), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Column {
                                            Text("Total Paid", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(cust.totalPaid), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                                        }
                                        Column {
                                            Text("Balance", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(cust.outstandingBalance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (cust.outstandingBalance > 0) DangerRed else SuccessGreen))
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (cust.outstandingBalance > 0.0) {
                                            FilledTonalButton(
                                                onClick = { customerToSettle = cust },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Receive Payment", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                customerToEdit = cust
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { customerToDelete = cust },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
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

    if (showAddDialog) {
        CustomerFormDialog(
            initialCustomer = customerToEdit,
            onDismiss = { showAddDialog = false },
            onSave = onSaveCustomer
        )
    }

    if (customerToSettle != null) {
        SettlePaymentDialog(
            title = "Receive Customer Payment",
            partyName = customerToSettle!!.name,
            outstandingAmount = customerToSettle!!.outstandingBalance,
            onDismiss = { customerToSettle = null },
            onConfirm = { amount ->
                onSettlePayment(customerToSettle!!.id, amount)
                customerToSettle = null
            }
        )
    }

    if (customerToDelete != null) {
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer") },
            text = { Text("Are you sure you want to delete customer '${customerToDelete!!.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(customerToDelete!!)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
