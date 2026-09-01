package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.SaleOrderEntity

@Composable
fun ClientPositionReportScreen(
    customers: List<CustomerEntity>,
    sales: List<SaleOrderEntity>,
    onSettlePayment: (customerId: Long, amount: Double) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val customerPositions = remember(customers, sales, searchQuery, selectedStatusFilter) {
        customers.map { customer ->
            val custSales = sales.filter { it.customerId == customer.id }
            val totalInvoicedSales = custSales.sumOf { it.grandTotal }
            val totalPaidSales = custSales.sumOf { it.paidAmount }
            val totalOutstandingSales = custSales.sumOf { it.remainingBalance }

            val debit = totalInvoicedSales + customer.openingBalance
            val credit = totalPaidSales
            val netCurrentBalance = (debit - credit).coerceAtLeast(0.0)

            val status = when {
                netCurrentBalance <= 0.001 -> "Fully Paid"
                totalPaidSales > 0.0 -> "Partially Paid"
                else -> "Outstanding"
            }

            ClientPositionData(
                customer = customer,
                totalSales = totalInvoicedSales,
                totalPaid = totalPaidSales,
                totalOutstanding = totalOutstandingSales,
                debit = debit,
                credit = credit,
                currentBalance = netCurrentBalance,
                paymentStatus = status
            )
        }.filter { pos ->
            val matchesSearch = searchQuery.isBlank() ||
                    pos.customer.name.contains(searchQuery, ignoreCase = true) ||
                    pos.customer.phone.contains(searchQuery, ignoreCase = true) ||
                    pos.customer.city.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == "All" ||
                    pos.paymentStatus.equals(selectedStatusFilter, ignoreCase = true)
            matchesSearch && matchesStatus
        }
    }

    val totalPortfolioDebit = customerPositions.sumOf { it.debit }
    val totalPortfolioCredit = customerPositions.sumOf { it.credit }
    val totalPortfolioOutstanding = customerPositions.sumOf { it.currentBalance }

    var settlingCustomer by remember { mutableStateOf<ClientPositionData?>(null) }
    var settlementAmountStr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CLIENT POSITION FINANCIAL SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Total Outstanding: Rs. ${"%,.2f".format(totalPortfolioOutstanding)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Debit (Invoiced + Opening)", style = MaterialTheme.typography.labelSmall)
                            Text("Rs. ${"%,.2f".format(totalPortfolioDebit)}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Credit (Collected)", style = MaterialTheme.typography.labelSmall)
                            Text("Rs. ${"%,.2f".format(totalPortfolioCredit)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search client name, phone, or city") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Outstanding", "Partially Paid", "Fully Paid").forEach { filter ->
                    FilterChip(
                        selected = selectedStatusFilter == filter,
                        onClick = { selectedStatusFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        item {
            Text(
                "Client Statements (${customerPositions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (customerPositions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No client position records match your filter.")
                    }
                }
            }
        } else {
            items(customerPositions, key = { it.customer.id }) { pos ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pos.customer.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("${pos.customer.phone} ${if (pos.customer.city.isNotBlank()) "• ${pos.customer.city}" else ""}", style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (pos.paymentStatus) {
                                    "Fully Paid" -> Color(0xFFE8F5E9)
                                    "Partially Paid" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFFFEBEE)
                                }
                            ) {
                                Text(
                                    pos.paymentStatus,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (pos.paymentStatus) {
                                        "Fully Paid" -> Color(0xFF2E7D32)
                                        "Partially Paid" -> Color(0xFFE65100)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Opening Balance: Rs. ${"%,.2f".format(pos.customer.openingBalance)}", style = MaterialTheme.typography.bodySmall)
                                Text("Total Invoiced: Rs. ${"%,.2f".format(pos.totalSales)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Paid (Credit): Rs. ${"%,.2f".format(pos.credit)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                Text("Debit Total: Rs. ${"%,.2f".format(pos.debit)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Net Current Balance / Payable:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "Rs. ${"%,.2f".format(pos.currentBalance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (pos.currentBalance > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                            }

                            if (pos.currentBalance > 0) {
                                Button(
                                    onClick = {
                                        settlingCustomer = pos
                                        settlementAmountStr = pos.currentBalance.toString()
                                    },
                                    modifier = Modifier.testTag("settle_client_payment_button")
                                ) {
                                    Text("Receive Payment", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (settlingCustomer != null) {
        val cust = settlingCustomer!!
        AlertDialog(
            onDismissRequest = { settlingCustomer = null },
            title = { Text("Record Payment for ${cust.customer.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outstanding Balance: Rs. ${"%,.2f".format(cust.currentBalance)}")
                    OutlinedTextField(
                        value = settlementAmountStr,
                        onValueChange = { settlementAmountStr = it },
                        label = { Text("Amount Received (Rs.)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settlementAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onSettlePayment(cust.customer.id, amt)
                            settlingCustomer = null
                        }
                    }
                ) {
                    Text("Confirm Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { settlingCustomer = null }) { Text("Cancel") }
            }
        )
    }
}

private data class ClientPositionData(
    val customer: CustomerEntity,
    val totalSales: Double,
    val totalPaid: Double,
    val totalOutstanding: Double,
    val debit: Double,
    val credit: Double,
    val currentBalance: Double,
    val paymentStatus: String
)
