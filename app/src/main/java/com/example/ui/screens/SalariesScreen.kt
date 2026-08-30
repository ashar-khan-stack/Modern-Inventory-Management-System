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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.ui.components.ProcessSalaryDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalariesScreen(
    employees: List<EmployeeEntity>,
    salaryPayments: List<SalaryPaymentEntity>,
    onDisburseSalary: (SalaryPaymentEntity) -> Unit,
    onUpdateSalary: (SalaryPaymentEntity) -> Unit,
    onDeleteSalary: (SalaryPaymentEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMonthFilter by remember { mutableStateOf("All Months") }
    var showProcessDialog by remember { mutableStateOf(false) }
    var salaryToEdit by remember { mutableStateOf<SalaryPaymentEntity?>(null) }
    var salaryToDelete by remember { mutableStateOf<SalaryPaymentEntity?>(null) }
    var selectedSalaryForVoucher by remember { mutableStateOf<SalaryPaymentEntity?>(null) }

    val distinctMonths = remember(salaryPayments) {
        listOf("All Months") + salaryPayments.map { it.monthYear }.distinct().sortedDescending()
    }

    val filteredPayments = remember(salaryPayments, searchQuery, selectedMonthFilter) {
        salaryPayments.filter { payment ->
            val matchesSearch = searchQuery.isBlank() ||
                    payment.employeeName.contains(searchQuery.trim(), ignoreCase = true) ||
                    payment.paymentMethod.contains(searchQuery.trim(), ignoreCase = true) ||
                    payment.monthYear.contains(searchQuery.trim(), ignoreCase = true)
            val matchesMonth = selectedMonthFilter == "All Months" || payment.monthYear == selectedMonthFilter
            matchesSearch && matchesMonth
        }
    }

    val totalDisbursed = remember(filteredPayments) { filteredPayments.sumOf { it.netSalary } }
    val totalBonuses = remember(filteredPayments) { filteredPayments.sumOf { it.bonus } }
    val totalDeductions = remember(filteredPayments) { filteredPayments.sumOf { it.deductions } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showProcessDialog = true },
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.testTag("process_salary_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Payments, contentDescription = "Process Salary", modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay Salary", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Metrics Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SalaryMetricCard(
                    title = "Total Disbursed",
                    value = "Rs. ${"%,.0f".format(totalDisbursed)}",
                    color = BrandBluePrimary,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                SalaryMetricCard(
                    title = "Total Bonuses",
                    value = "Rs. ${"%,.0f".format(totalBonuses)}",
                    color = SuccessGreen,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                SalaryMetricCard(
                    title = "Deductions",
                    value = "Rs. ${"%,.0f".format(totalDeductions)}",
                    color = DangerRed,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar & Month Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by employee, method...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("salary_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Month Filter Chips
            if (distinctMonths.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = distinctMonths.indexOf(selectedMonthFilter).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    distinctMonths.forEach { month ->
                        val isSelected = selectedMonthFilter == month
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMonthFilter = month },
                            label = { Text(month, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBluePrimaryContainer,
                                selectedLabelColor = BrandBluePrimary
                            )
                        )
                    }
                }
            }

            // Payments List
            if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedMonthFilter != "All Months") "No salary disbursements match the filter" else "No salary records processed yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (salaryPayments.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showProcessDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Process First Salary")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        SalaryPaymentCard(
                            payment = payment,
                            onEdit = { salaryToEdit = payment },
                            onDelete = { salaryToDelete = payment },
                            onClick = { selectedSalaryForVoucher = payment }
                        )
                    }
                }
            }
        }
    }

    // Process Salary Dialog
    if (showProcessDialog || salaryToEdit != null) {
        ProcessSalaryDialog(
            employees = employees,
            initialSalary = salaryToEdit,
            onDismiss = {
                showProcessDialog = false
                salaryToEdit = null
            },
            onSave = { payment ->
                if (salaryToEdit != null) {
                    onUpdateSalary(payment)
                } else {
                    onDisburseSalary(payment)
                }
                showProcessDialog = false
                salaryToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (salaryToDelete != null) {
        AlertDialog(
            onDismissRequest = { salaryToDelete = null },
            title = { Text("Delete Salary Voucher?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the salary record for ${salaryToDelete!!.employeeName} (${salaryToDelete!!.monthYear})?") },
            confirmButton = {
                Button(
                    onClick = {
                        salaryToDelete?.let { onDeleteSalary(it) }
                        salaryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { salaryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Salary Voucher Detail Dialog
    if (selectedSalaryForVoucher != null) {
        val p = selectedSalaryForVoucher!!
        val dateFormatted = remember(p.paymentDate) {
            try {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(p.paymentDate))
            } catch (e: Exception) {
                p.paymentDate.toString()
            }
        }
        AlertDialog(
            onDismissRequest = { selectedSalaryForVoucher = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(p.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Month: ${p.monthYear}", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Payment Date: $dateFormatted", style = MaterialTheme.typography.bodyMedium)
                    Text("Method: ${p.paymentMethod}", style = MaterialTheme.typography.bodyMedium)
                    if (p.notes.isNotBlank()) {
                        Text("Notes: ${p.notes}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Salary:", fontWeight = FontWeight.SemiBold)
                        Text("Rs. ${"%,.2f".format(p.baseSalary)}", fontWeight = FontWeight.SemiBold)
                    }
                    if (p.bonus > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bonus / Allowance:", fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                            Text("+ Rs. ${"%,.2f".format(p.bonus)}", fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        }
                    }
                    if (p.deductions > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Deductions / Advances:", fontWeight = FontWeight.SemiBold, color = DangerRed)
                            Text("- Rs. ${"%,.2f".format(p.deductions)}", fontWeight = FontWeight.SemiBold, color = DangerRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Salary Disbursed:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Rs. ${"%,.2f".format(p.netSalary)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandBluePrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSalaryForVoucher = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SalaryMetricCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.bodySmall, color = AppTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SalaryPaymentCard(
    payment: SalaryPaymentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormatted = remember(payment.paymentDate) {
        try {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(payment.paymentDate))
        } catch (e: Exception) {
            payment.paymentDate.toString()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("salary_payment_card_${payment.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandBluePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.employeeName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Period: ${payment.monthYear} • ${payment.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextSecondary
                )
                Text(
                    text = "Paid on: $dateFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rs. ${"%,.0f".format(payment.netSalary)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandBluePrimary
                )
                if (payment.bonus > 0 || payment.deductions > 0) {
                    Text(
                        text = "Base: Rs. ${"%,.0f".format(payment.baseSalary)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
