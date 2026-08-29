package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderJsonParser
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ReportsScreen(
    sales: List<SaleOrderEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    salaryPayments: List<SalaryPaymentEntity>
) {
    val context = LocalContext.current

    // Financial Calculations
    val grossSales = remember(sales) { sales.sumOf { it.subtotal } }
    val discountsGiven = remember(sales) { sales.sumOf { it.discountAmount } }
    val taxesCollected = remember(sales) { sales.sumOf { it.taxAmount } }
    val netSalesRevenue = remember(sales) { sales.sumOf { it.grandTotal } }

    val generalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val salaryExpenses = remember(salaryPayments) { salaryPayments.sumOf { it.netSalary } }
    val totalOperatingExpenses = generalExpenses + salaryExpenses

    val netProfit = netSalesRevenue - totalOperatingExpenses
    val netProfitMarginPercent = if (netSalesRevenue > 0) (netProfit / netSalesRevenue * 100) else 0.0

    // Debt & Receivables
    val customerReceivables = remember(customers) { customers.sumOf { it.outstandingBalance } }

    // Top Selling Items
    val topSellingItems = remember(sales) {
        val qtyMap = mutableMapOf<String, Int>()
        val revMap = mutableMapOf<String, Double>()
        sales.forEach { s ->
            val items = OrderJsonParser.jsonToSaleItems(s.itemsJson)
            items.forEach { itm ->
                val name = itm.productName.ifBlank { "Custom Item" }
                qtyMap[name] = (qtyMap[name] ?: 0) + itm.quantity
                revMap[name] = (revMap[name] ?: 0.0) + itm.subtotal
            }
        }
        qtyMap.entries.sortedByDescending { it.value }.take(5).map { entry ->
            Triple(entry.key, entry.value, revMap[entry.key] ?: 0.0)
        }
    }

    // Expense Breakdown
    val expenseCategorySlices = remember(expenses, salaryExpenses) {
        val list = mutableListOf<DonutSliceData>()
        val grouped = expenses.groupBy { it.category }
        val colors = listOf(DangerRed, WarningAmber, BrandBluePrimary, InfoIndigo, SuccessGreen, BrandNavySecondary)
        grouped.entries.forEachIndexed { i, entry ->
            list.add(DonutSliceData(entry.key, entry.value.sumOf { it.amount }, colors[i % colors.size]))
        }
        if (salaryExpenses > 0) {
            list.add(DonutSliceData("Staff Payroll", salaryExpenses, Color(0xFF8B5CF6)))
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header with Export Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Executive Reports", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Comprehensive financial analytics", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary))
                }
                Button(
                    onClick = {
                        shareFinancialReport(
                            context = context,
                            netSales = netSalesRevenue,
                            expenses = totalOperatingExpenses,
                            netProfit = netProfit
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavySecondary),
                    modifier = Modifier.testTag("export_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export")
                }
            }
        }

        // Complete Profit & Loss Analytics and Period Comparison Section
        item {
            ProfitLossAnalyticsSection(
                sales = sales,
                expenses = expenses,
                salaryPayments = salaryPayments
            )
        }

        // Executive Profit / Loss Statement Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Income Statement (P&L)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (netProfit >= 0) SuccessGreenContainer else DangerRedContainer
                        ) {
                            Text(
                                "Net Margin: ${String.format("%.1f", netProfitMarginPercent)}%",
                                color = if (netProfit >= 0) Color(0xFF065F46) else DangerRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ReportLineRow("Gross Revenue / Sales", formatCurrency(grossSales))
                    if (discountsGiven > 0) {
                        ReportLineRow("Discounts Deducted", "-${formatCurrency(discountsGiven)}", color = DangerRed)
                    }
                    if (taxesCollected > 0) {
                        ReportLineRow("Taxes Included", formatCurrency(taxesCollected), color = AppTextSecondary)
                    }
                    ReportLineRow("Net Revenue", formatCurrency(netSalesRevenue), isBold = true)

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = AppOutlineLight)
                    Spacer(modifier = Modifier.height(6.dp))

                    ReportLineRow("Operating Expenses", "-${formatCurrency(generalExpenses)}", color = DangerRed)
                    ReportLineRow("Staff Salaries Payroll", "-${formatCurrency(salaryExpenses)}", color = DangerRed)
                    ReportLineRow("Total Overhead Expenses", "-${formatCurrency(totalOperatingExpenses)}", color = DangerRed, isBold = true)

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (netProfit >= 0) SuccessGreenContainer else DangerRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NET BUSINESS PROFIT:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black))
                            Text(
                                formatCurrency(netProfit),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (netProfit >= 0) Color(0xFF065F46) else DangerRed
                                )
                            )
                        }
                    }
                }
            }
        }

        // Working Capital Position
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Working Capital & Debt Balances",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryMetricCard(
                        title = "Customer Receivables",
                        value = formatCurrency(customerReceivables),
                        subtitle = "Owed to your business",
                        icon = Icons.Default.CallReceived,
                        accentColor = BrandBluePrimary,
                        containerColor = BrandBluePrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Expenses breakdown Chart
        if (expenseCategorySlices.isNotEmpty()) {
            item {
                DonutCategoryChart(
                    title = "Operating Expenditure Distribution",
                    slices = expenseCategorySlices
                )
            }
        }

        // Top Selling Items Section
        if (topSellingItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Top Selling Items / Services", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))

                        topSellingItems.forEachIndexed { i, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBluePrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandBlueOnPrimaryContainer))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text("${item.second} units sold", style = MaterialTheme.typography.bodySmall.copy(color = AppTextSecondary, fontSize = 11.sp))
                                    }
                                }
                                Text(formatCurrency(item.third), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BrandNavySecondary))
                            }
                            if (i < topSellingItems.size - 1) HorizontalDivider(color = AppOutlineLight.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportLineRow(
    label: String,
    value: String,
    color: Color = AppTextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBold) AppTextPrimary else AppTextSecondary
            )
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
        )
    }
}

fun shareFinancialReport(
    context: Context,
    netSales: Double,
    expenses: Double,
    netProfit: Double
) {
    val reportText = """
        ========================================
        EXECUTIVE FINANCIAL SUMMARY REPORT
        ========================================
        Net Sales Revenue: ${formatCurrency(netSales)}
        Overhead Expenses & Payroll: -${formatCurrency(expenses)}
        ----------------------------------------
        NET PROFIT: ${formatCurrency(netProfit)}
        ========================================
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Business Financial Summary Report")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Financial Report"))
}
