package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.ExpenseEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    onSaveExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEditPayment by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val categories = remember(expenses) {
        listOf("All") + expenses.map { it.category }.distinct().sorted()
    }

    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }

    val filteredExpenses = remember(expenses, searchQuery, selectedCategory) {
        expenses.filter { exp ->
            val matchQuery = searchQuery.isBlank() ||
                    exp.description.contains(searchQuery, ignoreCase = true) ||
                    exp.category.contains(searchQuery, ignoreCase = true) ||
                    exp.notes.contains(searchQuery, ignoreCase = true)

            val matchCat = selectedCategory == "All" || exp.category == selectedCategory
            matchQuery && matchCat
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                },
                containerColor = DangerRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Metrics Summary
            SummaryMetricCard(
                title = "Total Business Expenses",
                value = formatCurrency(totalExpenses),
                subtitle = "${expenses.size} recorded expense entries",
                icon = Icons.Default.MoneyOff,
                accentColor = DangerRed,
                containerColor = DangerRedContainer
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search expense description, notes...") },
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

            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = (selectedCategory == cat),
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Expense Logs (${filteredExpenses.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expense entries found.", color = AppTextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(exp.date))
                        val status = exp.paymentStatus.ifBlank { "Paid" }
                        val (statusBg, statusFg) = when (status) {
                            "Paid" -> Pair(SuccessGreenContainer, SuccessGreen)
                            "Partially Paid", "Partial" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                            else -> Pair(DangerRedContainer, DangerRed)
                        }

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(exp.description, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    exp.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Via ${exp.paymentMethod} • $dateFormatted", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            status.uppercase(),
                                            color = statusFg,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
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
                                            Text("Total", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(exp.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Column {
                                            Text("Paid", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(exp.paidAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                                        }
                                        Column {
                                            Text("Balance", style = MaterialTheme.typography.labelSmall.copy(color = AppTextSecondary))
                                            Text(formatCurrency(exp.remainingBalance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (exp.remainingBalance > 0) DangerRed else SuccessGreen))
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                expenseToEditPayment = exp
                                            },
                                            modifier = Modifier.size(32.dp).testTag("edit_expense_payment_btn")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Payment Status", tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { expenseToDelete = exp },
                                            modifier = Modifier.size(32.dp).testTag("delete_expense_btn")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Expense", tint = DangerRed, modifier = Modifier.size(18.dp))
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
        ExpenseFormDialog(
            initialExpense = null,
            onDismiss = { showAddDialog = false },
            onSave = { newExp ->
                // New expenses default to fully paid unless edited
                val finalExp = if (newExp.paidAmount == 0.0 && newExp.paymentStatus.isBlank()) {
                    newExp.copy(paidAmount = newExp.amount, remainingBalance = 0.0, paymentStatus = "Paid")
                } else newExp
                onSaveExpense(finalExp)
            }
        )
    }

    if (expenseToEditPayment != null) {
        ExpensePaymentStatusDialog(
            expense = expenseToEditPayment!!,
            onDismiss = { expenseToEditPayment = null },
            onSaveStatus = { updatedExp ->
                onSaveExpense(updatedExp)
                expenseToEditPayment = null
            }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(expenseToDelete!!)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
