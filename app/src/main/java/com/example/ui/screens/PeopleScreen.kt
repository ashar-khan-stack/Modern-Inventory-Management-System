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
import com.example.data.model.EmployeeEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.data.model.SaleOrderEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    customers: List<CustomerEntity>,
    employees: List<EmployeeEntity>,
    salaryPayments: List<SalaryPaymentEntity>,
    sales: List<SaleOrderEntity>,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onSettlePayment: (customerId: Long, amount: Double) -> Unit,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onDisburseSalary: (SalaryPaymentEntity) -> Unit,
    onUpdateSalary: (SalaryPaymentEntity) -> Unit = {},
    onDeleteSalary: (SalaryPaymentEntity) -> Unit = {}
) {
    var mainTab by remember { mutableIntStateOf(0) } // 0: Customers, 1: Employees
    var employeeSubTab by remember { mutableIntStateOf(0) } // 0: Staff Directory, 1: Salary Payroll

    var searchQuery by remember { mutableStateOf("") }
    var filterBalanceOnly by remember { mutableStateOf(false) }

    // Dialog state
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToSettle by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerCannotDeleteMessage by remember { mutableStateOf<String?>(null) }

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeCannotDeleteMessage by remember { mutableStateOf<String?>(null) }

    var showProcessSalaryDialog by remember { mutableStateOf(false) }
    var salaryToEdit by remember { mutableStateOf<SalaryPaymentEntity?>(null) }
    var salaryToDelete by remember { mutableStateOf<SalaryPaymentEntity?>(null) }

    // Metrics calculations
    val totalReceivable = remember(customers) { customers.sumOf { it.outstandingBalance } }
    val totalPurchases = remember(customers) { customers.sumOf { it.totalPurchases } }

    val totalMonthlyPayroll = remember(employees) { employees.filter { it.status == "Active" }.sumOf { it.baseSalary } }
    val totalDisbursed = remember(salaryPayments) { salaryPayments.sumOf { it.netSalary } }

    // Filtering lists
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

    val filteredEmployees = remember(employees, searchQuery) {
        employees.filter { emp ->
            searchQuery.isBlank() ||
                    emp.name.contains(searchQuery, ignoreCase = true) ||
                    emp.position.contains(searchQuery, ignoreCase = true) ||
                    emp.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (mainTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (employeeSubTab == 0) {
                            employeeToEdit = null
                            showAddEmployeeDialog = true
                        } else {
                            showProcessSalaryDialog = true
                        }
                    },
                    containerColor = BrandBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_employee_fab")
                ) {
                    Icon(
                        if (employeeSubTab == 0) Icons.Default.PersonAdd else Icons.Default.Payments,
                        contentDescription = if (employeeSubTab == 0) "Add Employee" else "Process Salary"
                    )
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
            // Main Top Level Tabs: Customers vs Employees
            TabRow(
                selectedTabIndex = mainTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandBluePrimary
            ) {
                Tab(
                    selected = mainTab == 0,
                    onClick = {
                        mainTab = 0
                        searchQuery = ""
                    },
                    text = { Text("Customers (${customers.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("customers_tab")
                )
                Tab(
                    selected = mainTab == 1,
                    onClick = {
                        mainTab = 1
                        searchQuery = ""
                    },
                    text = { Text("Employees (${employees.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("employees_tab")
                )
            }

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                if (mainTab == 0) {
                    // ================= CUSTOMERS SECTION =================
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
                        modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
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
                            },
                            modifier = Modifier.testTag("balance_only_filter_chip")
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
                            modifier = Modifier.fillMaxWidth().weight(1f)
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                                        modifier = Modifier.height(32.dp).testTag("customer_settle_btn_${cust.id}")
                                                    ) {
                                                        Text("Receive Payment", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        customerToEdit = cust
                                                        showAddCustomerDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("customer_edit_btn_${cust.id}")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        // Safe-Integrity Check: If Customer has associated SaleOrders, block deletion
                                                        val hasSales = sales.any { it.customerId == cust.id }
                                                        if (hasSales) {
                                                            customerCannotDeleteMessage = "Customer '${cust.name}' has existing historical sales records and cannot be deleted to preserve financial history."
                                                        } else {
                                                            customerToDelete = cust
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("customer_delete_btn_${cust.id}")
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

                } else {
                    // ================= EMPLOYEES SECTION =================
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryMetricCard(
                            title = "Monthly Base Payroll",
                            value = formatCurrency(totalMonthlyPayroll),
                            subtitle = "${employees.count { it.status == "Active" }} active employees",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = BrandBluePrimary,
                            containerColor = BrandBluePrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            title = "Disbursed YTD",
                            value = formatCurrency(totalDisbursed),
                            subtitle = "${salaryPayments.size} payout vouchers",
                            icon = Icons.Default.CheckCircle,
                            accentColor = SuccessGreen,
                            containerColor = SuccessGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Inner Tabs: Staff Directory vs Salary Payroll
                    TabRow(
                        selectedTabIndex = employeeSubTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = BrandNavySecondary,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = employeeSubTab == 0,
                            onClick = { employeeSubTab = 0 },
                            text = { Text("Staff Directory", style = MaterialTheme.typography.titleSmall) }
                        )
                        Tab(
                            selected = employeeSubTab == 1,
                            onClick = { employeeSubTab = 1 },
                            text = { Text("Salary Payroll Logs", style = MaterialTheme.typography.titleSmall) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (employeeSubTab == 0) {
                        // Employee Directory Subtab
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search staff name, position, phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("employee_search_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredEmployees.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No employee records found.", color = AppTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                items(filteredEmployees, key = { it.id }) { emp ->
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
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(CircleShape)
                                                            .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            emp.name.take(1).uppercase(),
                                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(emp.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                        Text("${emp.position} • Joined ${emp.joiningDate}", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                                                    }
                                                }

                                                StatusBadge(status = emp.status)
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = AppOutlineLight)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Base Monthly Salary", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                                    Text(formatCurrency(emp.baseSalary), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BrandNavySecondary))
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            employeeToEdit = emp
                                                            showAddEmployeeDialog = true
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("employee_edit_btn_${emp.id}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            // Safe-Integrity Check: If employee has associated SalaryPayments, block deletion
                                                            val hasSalaryRecords = salaryPayments.any { it.employeeId == emp.id }
                                                            if (hasSalaryRecords) {
                                                                employeeCannotDeleteMessage = "This employee has existing salary records and cannot be deleted without affecting payroll history."
                                                            } else {
                                                                employeeToDelete = emp
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("employee_delete_btn_${emp.id}")
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
                    } else {
                        // Salary Disbursements Log Subtab
                        if (salaryPayments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No salary disbursements logged yet.", color = AppTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                items(salaryPayments, key = { it.id }) { sal ->
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(sal.paymentDate))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(sal.employeeName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    StatusBadge(status = sal.paymentStatus)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Period: ${sal.monthYear} • Disbursed $dateStr via ${sal.paymentMethod}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary)
                                                )
                                                Text(
                                                    "Base: ${formatCurrency(sal.baseSalary)}${if (sal.bonus > 0) " + Bonus: ${formatCurrency(sal.bonus)}" else ""}${if (sal.deductions > 0) " - Deduct: ${formatCurrency(sal.deductions)}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 11.sp)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    formatCurrency(sal.netSalary),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                                                )
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            salaryToEdit = sal
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("edit_salary_btn_${sal.id}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Salary", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { salaryToDelete = sal },
                                                        modifier = Modifier.size(32.dp).testTag("delete_salary_btn_${sal.id}")
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Salary", tint = DangerRed, modifier = Modifier.size(18.dp))
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
            }
        }
    }

    // ================= ALL DIALOGS & POPUPS =================

    if (showAddCustomerDialog) {
        CustomerFormDialog(
            initialCustomer = customerToEdit,
            onDismiss = { showAddCustomerDialog = false },
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
            title = { Text("Delete Customer?") },
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

    // Customer Deletion Blocked Warn Dialog
    if (customerCannotDeleteMessage != null) {
        AlertDialog(
            onDismissRequest = { customerCannotDeleteMessage = null },
            title = { Text("Cannot Delete Customer") },
            text = { Text(customerCannotDeleteMessage!!) },
            confirmButton = {
                Button(onClick = { customerCannotDeleteMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showAddEmployeeDialog) {
        EmployeeFormDialog(
            initialEmployee = employeeToEdit,
            onDismiss = { showAddEmployeeDialog = false },
            onSave = onSaveEmployee
        )
    }

    if (showProcessSalaryDialog || salaryToEdit != null) {
        ProcessSalaryDialog(
            employees = employees,
            initialSalary = salaryToEdit,
            onDismiss = {
                showProcessSalaryDialog = false
                salaryToEdit = null
            },
            onSave = { record ->
                if (salaryToEdit != null) {
                    onUpdateSalary(record)
                } else {
                    onDisburseSalary(record)
                }
                showProcessSalaryDialog = false
                salaryToEdit = null
            }
        )
    }

    if (salaryToDelete != null) {
        AlertDialog(
            onDismissRequest = { salaryToDelete = null },
            title = { Text("Delete Salary Payment") },
            text = { Text("Are you sure you want to delete the salary record for '${salaryToDelete!!.employeeName}' (${salaryToDelete!!.monthYear})?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSalary(salaryToDelete!!)
                        salaryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { salaryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee?") },
            text = { Text("Are you sure you want to delete this employee?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEmployee(employeeToDelete!!)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Employee Deletion Blocked Warn Dialog
    if (employeeCannotDeleteMessage != null) {
        AlertDialog(
            onDismissRequest = { employeeCannotDeleteMessage = null },
            title = { Text("Cannot Delete Employee") },
            text = { Text(employeeCannotDeleteMessage!!) },
            confirmButton = {
                Button(onClick = { employeeCannotDeleteMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
