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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.SaleOrderEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgersScreen(
    customers: List<CustomerEntity>,
    sales: List<SaleOrderEntity>
) {
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var dropdownOpen by remember { mutableStateOf(false) }

    // Sort customers by name for selection
    val sortedCustomers = remember(customers) {
        customers.sortedBy { it.name }
    }

    // Automatically select the first customer if none is selected
    LaunchedEffect(sortedCustomers) {
        if (selectedCustomer == null && sortedCustomers.isNotEmpty()) {
            selectedCustomer = sortedCustomers.first()
        }
    }

    // Derive ledger entries chronologically
    val ledgerEntries = remember(selectedCustomer, sales) {
        if (selectedCustomer == null) return@remember emptyList<LedgerRow>()

        val entries = mutableListOf<LedgerRow>()

        // Find sales invoices for this customer
        val custSales = sales.filter { it.customerId == selectedCustomer!!.id }

        custSales.forEach { sale ->
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(sale.createdAt))
            // Add invoice purchase entry
            entries.add(
                LedgerRow(
                    date = formattedDate, // Format date
                    reference = "Invoice #${sale.invoiceNumber}",
                    debit = sale.grandTotal,  // Customer owes this amount
                    credit = 0.0,
                    type = "Purchase"
                )
            )

            // Add payment transaction entries for this invoice
            if (sale.paidAmount > 0.0) {
                entries.add(
                    LedgerRow(
                        date = formattedDate,
                        reference = "Receipt (Inv #${sale.invoiceNumber})",
                        debit = 0.0,
                        credit = sale.paidAmount, // Customer paid this amount
                        type = "Payment"
                    )
                )
            }
        }

        // Sort chronologically. If dates match, put Purchases before Payments
        val sortedList = entries.sortedWith(compareBy<LedgerRow> { it.date }.thenBy { if (it.type == "Purchase") 0 else 1 })

        // Calculate running balance
        var currentBalance = 0.0
        val finalRows = sortedList.map { row ->
            currentBalance += (row.debit - row.credit)
            row.copy(runningBalance = currentBalance)
        }

        finalRows
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Customer selector Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = dropdownOpen,
                onExpandedChange = { dropdownOpen = !dropdownOpen }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Select Customer...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Customer Profile") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("ledger_customer_dropdown")
                )

                ExposedDropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false }
                ) {
                    if (sortedCustomers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No customers available") },
                            onClick = { dropdownOpen = false }
                        )
                    } else {
                        sortedCustomers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text(cust.name) },
                                onClick = {
                                    selectedCustomer = cust
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedCustomer == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Please register a customer profile to view general account ledgers.", color = AppTextSecondary)
            }
        } else {
            // Customer Summary Header
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(selectedCustomer!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Phone: ${selectedCustomer!!.phone}${if (selectedCustomer!!.address.isNotBlank()) " • ${selectedCustomer!!.address}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = AppOutlineLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Billing (Debit)", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                            Text(
                                text = String.format("Rs. %,.2f", selectedCustomer!!.totalPurchases),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Paid (Credit)", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                            Text(
                                text = String.format("Rs. %,.2f", selectedCustomer!!.totalPaid),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Outstanding Balance", style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                            Text(
                                text = String.format("Rs. %,.2f", selectedCustomer!!.outstandingBalance),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCustomer!!.outstandingBalance > 0) DangerRed else SuccessGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Chronological Account Statement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandNavySecondary)

            Spacer(modifier = Modifier.height(8.dp))

            // Ledger Entries List / Table format
            if (ledgerEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No billing or payment history found for this customer.", color = AppTextSecondary)
                }
            } else {
                // Table header
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date & Ref", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Debit (+)", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Credit (-)", modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Balance", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(ledgerEntries) { entry ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(entry.date, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(entry.reference, style = MaterialTheme.typography.labelSmall, color = AppTextSecondary)
                                }

                                Text(
                                    text = if (entry.debit > 0) String.format("Rs. %,.0f", entry.debit) else "—",
                                    modifier = Modifier.weight(1.1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (entry.debit > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (entry.credit > 0) String.format("Rs. %,.0f", entry.credit) else "—",
                                    modifier = Modifier.weight(1.1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (entry.credit > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = String.format("Rs. %,.0f", entry.runningBalance),
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.runningBalance > 0) DangerRed else SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LedgerRow(
    val date: String,
    val reference: String,
    val debit: Double,
    val credit: Double,
    val type: String,
    val runningBalance: Double = 0.0
)
