package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployeesScreen(
    employees: List<EmployeeEntity>,
    salaryPayments: List<SalaryPaymentEntity>,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onDisburseSalary: (SalaryPaymentEntity) -> Unit,
    onUpdateSalary: (SalaryPaymentEntity) -> Unit = {},
    onDeleteSalary: (SalaryPaymentEntity) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Employees, 1: Salary Payments
    var searchQuery by remember { mutableStateOf("") }

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showProcessSalaryDialog by remember { mutableStateOf(false) }

    var salaryToEdit by remember { mutableStateOf<SalaryPaymentEntity?>(null) }
    var salaryToDelete by remember { mutableStateOf<SalaryPaymentEntity?>(null) }

    val totalMonthlyPayroll = remember(employees) { employees.filter { it.status == "Active" }.sumOf { it.baseSalary } }
    val totalDisbursed = remember(salaryPayments) { salaryPayments.sumOf { it.netSalary } }

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
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) {
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
                    if (activeTab == 0) Icons.Default.PersonAdd else Icons.Default.Payments,
                    contentDescription = if (activeTab == 0) "Add Employee" else "Process Salary"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandBluePrimary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Staff Directory (${employees.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Salary Payroll (${salaryPayments.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Metric Header
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

                if (activeTab == 0) {
                    // Search
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
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(1f)
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
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { employeeToDelete = emp },
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
                } else {
                    // Tab 1: Salary Payments Log
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
                            modifier = Modifier.weight(1f)
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
                                                    modifier = Modifier.size(32.dp).testTag("edit_salary_btn")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Salary", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { salaryToDelete = sal },
                                                    modifier = Modifier.size(32.dp).testTag("delete_salary_btn")
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
            title = { Text("Delete Employee") },
            text = { Text("Are you sure you want to remove employee '${employeeToDelete!!.name}'?") },
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
}
