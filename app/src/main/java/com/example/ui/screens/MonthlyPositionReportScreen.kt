package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.data.model.SaleOrderEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPositionReportScreen(
    sales: List<SaleOrderEntity>,
    expenses: List<ExpenseEntity>,
    salaries: List<SalaryPaymentEntity>
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val monthKeyFormat = remember { SimpleDateFormat("yyyy-MM", Locale.US) }

    // Aggregate monthly data
    val monthlyDataMap = remember(sales, expenses, salaries) {
        val map = mutableMapOf<String, MonthlyPositionData>()

        fun getOrCreate(timeMillis: Long): MonthlyPositionData {
            val key = monthKeyFormat.format(Date(timeMillis))
            val label = monthFormat.format(Date(timeMillis))
            return map.getOrPut(key) {
                MonthlyPositionData(key = key, monthLabel = label)
            }
        }

        sales.forEach { s ->
            val data = getOrCreate(s.createdAt)
            data.totalSales += s.grandTotal
            data.totalCollected += s.paidAmount
            data.totalOutstandingSales += s.remainingBalance
        }

        expenses.forEach { e ->
            if (e.category != "Salary") {
                val data = getOrCreate(e.date)
                data.totalExpenses += e.amount
            }
        }

        salaries.forEach { sal ->
            val data = getOrCreate(sal.paymentDate)
            if (sal.paymentStatus == "Paid") {
                data.totalSalaries += sal.netSalary
            }
        }

        map.values.sortedByDescending { it.key }
    }

    var selectedMonthKey by remember { mutableStateOf("All") }

    val filteredMonthlyData = remember(monthlyDataMap, selectedMonthKey) {
        if (selectedMonthKey == "All") monthlyDataMap
        else monthlyDataMap.filter { it.key == selectedMonthKey }
    }

    val cumulativeSales = monthlyDataMap.sumOf { it.totalSales }
    val cumulativeCollected = monthlyDataMap.sumOf { it.totalCollected }
    val cumulativeExpenses = monthlyDataMap.sumOf { it.totalExpenses }
    val cumulativeSalaries = monthlyDataMap.sumOf { it.totalSalaries }
    val cumulativeNetCashFlow = cumulativeCollected - cumulativeExpenses - cumulativeSalaries

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CUMULATIVE BUSINESS PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Net Cash Flow: Rs. ${"%,.2f".format(cumulativeNetCashFlow)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (cumulativeNetCashFlow >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Sales: Rs. ${"%,.2f".format(cumulativeSales)}", style = MaterialTheme.typography.bodySmall)
                            Text("Total Collections: Rs. ${"%,.2f".format(cumulativeCollected)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Total Expenses: Rs. ${"%,.2f".format(cumulativeExpenses)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                            Text("Payroll Outflow: Rs. ${"%,.2f".format(cumulativeSalaries)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Monthly Financial Statements (${filteredMonthlyData.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (filteredMonthlyData.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No monthly transaction records found.")
                    }
                }
            }
        } else {
            items(filteredMonthlyData, key = { it.key }) { mData ->
                val netProfitLoss = mData.totalCollected - mData.totalExpenses - mData.totalSalaries
                val isPositive = netProfitLoss >= 0

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(mData.monthLabel, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    if (isPositive) "SURPLUS" else "DEFICIT",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Invoiced Sales:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${"%,.2f".format(mData.totalSales)}", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Cash Collected:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${"%,.2f".format(mData.totalCollected)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("General Expenses:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${"%,.2f".format(mData.totalExpenses)}", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Salaries Payout:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${"%,.2f".format(mData.totalSalaries)}", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Net Surplus / Margin:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Rs. ${"%,.2f".format(netProfitLoss)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

private class MonthlyPositionData(
    val key: String,
    val monthLabel: String,
    var totalSales: Double = 0.0,
    var totalCollected: Double = 0.0,
    var totalOutstandingSales: Double = 0.0,
    var totalExpenses: Double = 0.0,
    var totalSalaries: Double = 0.0
)
