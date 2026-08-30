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
import com.example.ui.components.EmployeeFormDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeInfoScreen(
    employees: List<EmployeeEntity>,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var selectedEmployeeForDetails by remember { mutableStateOf<EmployeeEntity?>(null) }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isBlank()) employees
        else employees.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.position.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.phone.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.email.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val activeCount = remember(employees) { employees.count { it.status == "Active" } }
    val totalPayrollBase = remember(employees) { employees.sumOf { it.baseSalary } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.testTag("add_employee_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Employee", modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Employee", fontWeight = FontWeight.Bold)
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
            // Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmployeeMetricCard(
                    title = "Total Staff",
                    value = "${employees.size}",
                    color = BrandBluePrimary,
                    icon = Icons.Default.Badge,
                    modifier = Modifier.weight(1f)
                )
                EmployeeMetricCard(
                    title = "Active Staff",
                    value = "$activeCount",
                    color = SuccessGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                EmployeeMetricCard(
                    title = "Base Payroll",
                    value = "Rs. ${"%,.0f".format(totalPayrollBase)}",
                    color = InfoIndigo,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search employees by name, role, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("employee_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBluePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Employee List
            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No employees found matching \"$searchQuery\"" else "No employees registered yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Employee")
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
                    items(filteredEmployees, key = { it.id }) { employee ->
                        EmployeeCard(
                            employee = employee,
                            onEdit = { employeeToEdit = employee },
                            onDelete = { employeeToDelete = employee },
                            onClick = { selectedEmployeeForDetails = employee }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Employee Dialog
    if (showAddDialog || employeeToEdit != null) {
        EmployeeFormDialog(
            initialEmployee = employeeToEdit,
            onDismiss = {
                showAddDialog = false
                employeeToEdit = null
            },
            onSave = { savedEmp ->
                onSaveEmployee(savedEmp)
                showAddDialog = false
                employeeToEdit = null
            }
        )
    }

    // Delete Confirmation
    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${employeeToDelete!!.name}? All past salary records for this employee will remain in ledger.") },
            confirmButton = {
                Button(
                    onClick = {
                        employeeToDelete?.let { onDeleteEmployee(it) }
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Employee Detail Dialog
    if (selectedEmployeeForDetails != null) {
        val emp = selectedEmployeeForDetails!!
        AlertDialog(
            onDismissRequest = { selectedEmployeeForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emp.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(emp.position, style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (emp.phone.isNotBlank()) {
                        Text("Phone: ${emp.phone}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (emp.email.isNotBlank()) {
                        Text("Email: ${emp.email}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (emp.address.isNotBlank()) {
                        Text("Address: ${emp.address}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (emp.joiningDate.isNotBlank()) {
                        Text("Joined: ${emp.joiningDate}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Monthly Salary:", fontWeight = FontWeight.SemiBold)
                        Text("Rs. ${"%,.2f".format(emp.baseSalary)}", fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Employment Status:", fontWeight = FontWeight.SemiBold)
                        Text(emp.status, fontWeight = FontWeight.Bold, color = if (emp.status == "Active") SuccessGreen else DangerRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEmployeeForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmployeeMetricCard(
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
private fun EmployeeCard(
    employee: EmployeeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("employee_card_${employee.id}"),
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
                Text(
                    text = employee.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = BrandBluePrimary,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = employee.position,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTextSecondary
                )
                if (employee.phone.isNotBlank()) {
                    Text(
                        text = employee.phone,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (employee.status == "Active") SuccessGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = employee.status,
                        color = if (employee.status == "Active") SuccessGreen else DangerRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Rs. ${"%,.0f".format(employee.baseSalary)}/mo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandBluePrimary
                )

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
