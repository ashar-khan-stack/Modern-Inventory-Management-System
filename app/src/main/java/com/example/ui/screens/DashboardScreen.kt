package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.ProfitLossAnalyticsSection
import com.example.ui.components.SummaryMetricCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardSummaryTotals
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    sales: List<SaleOrderEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    employees: List<EmployeeEntity> = emptyList(),
    salaryPayments: List<SalaryPaymentEntity> = emptyList(),
    summaryTotals: DashboardSummaryTotals? = null,
    currentSession: com.example.data.repository.UserSession? = null,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToPeople: () -> Unit,
    onSelectInvoice: (SaleOrderEntity) -> Unit
) {
    val monthStart = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val todayStart = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val totalSales: Double = remember(sales) { sales.sumOf { it.grandTotal } }
    val todaySales: Double = remember(sales, todayStart) {
        sales.filter { it.createdAt >= todayStart }.sumOf { it.grandTotal }
    }
    val generalExpenses: Double = remember(expenses) { expenses.filter { it.category != "Salary" }.sumOf { it.amount } }
    val totalSalaries: Double = remember(salaryPayments) { salaryPayments.sumOf { it.netSalary } }
    val totalExpenses: Double = generalExpenses + totalSalaries

    val netProfitVal: Double = totalSales - totalExpenses

    val monthlySales: Double = remember(sales, monthStart) {
        sales.filter { it.createdAt >= monthStart }.sumOf { it.grandTotal }
    }
    val monthlyGeneralExpenses: Double = remember(expenses, monthStart) {
        expenses.filter { it.category != "Salary" && it.date >= monthStart }.sumOf { it.amount }
    }
    val monthlySalaries: Double = remember(salaryPayments, monthStart) {
        salaryPayments.filter { it.paymentDate >= monthStart }.sumOf { it.netSalary }
    }
    val monthlyOutflow: Double = monthlyGeneralExpenses + monthlySalaries
    val monthlyProfitVal: Double = monthlySales - monthlyOutflow

    val recentSalesTrendData = remember(sales) {
        val dateFormat = SimpleDateFormat("EEE", Locale.US)
        val daySalesMap = LinkedHashMap<String, Double>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayName = dateFormat.format(c.time)
            daySalesMap[dayName] = 0.0
        }

        sales.forEach { sale ->
            val saleCal = Calendar.getInstance().apply { timeInMillis = sale.createdAt }
            val diffDays = (Calendar.getInstance().timeInMillis - sale.createdAt) / (1000 * 60 * 60 * 24)
            if (diffDays in 0..6) {
                val dayName = dateFormat.format(saleCal.time)
                if (daySalesMap.containsKey(dayName)) {
                    daySalesMap[dayName] = (daySalesMap[dayName] ?: 0.0) + sale.grandTotal
                }
            }
        }
        daySalesMap.toList()
    }

    val currentMonthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Welcome Greeting Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (currentSession != null) "Welcome, ${currentSession.firstName}!" else "Welcome Back!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Executive Financial & Business Overview",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = BrandBluePrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. TOP FINANCIAL CARDS: Total Revenue | Net Profit
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryMetricCard(
                        title = "Total Revenue",
                        value = formatCurrency(totalSales),
                        subtitle = "Today: ${formatCurrency(todaySales)}",
                        icon = Icons.Default.TrendingUp,
                        accentColor = BrandBluePrimary,
                        containerColor = BrandBluePrimaryContainer,
                        badgeText = null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSales
                    )

                    SummaryMetricCard(
                        title = "Net Profit",
                        value = formatCurrency(netProfitVal),
                        subtitle = if (netProfitVal >= 0.0) "Profitable" else "Loss",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (netProfitVal >= 0.0) SuccessGreen else DangerRed,
                        containerColor = if (netProfitVal >= 0.0) SuccessGreenContainer else DangerRedContainer,
                        badgeText = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. PROFIT & LOSS ANALYTICS & PERIOD COMPARISON
        item {
            ProfitLossAnalyticsSection(
                sales = sales,
                expenses = expenses,
                salaryPayments = salaryPayments
            )
        }

        // 3. MONTHLY PROFIT CARD
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AppOutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_profit_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (monthlyProfitVal >= 0.0) SuccessGreenContainer else DangerRedContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (monthlyProfitVal >= 0.0) Icons.Default.MonetizationOn else Icons.Default.TrendingDown,
                                    contentDescription = "Monthly Profit",
                                    tint = if (monthlyProfitVal >= 0.0) SuccessGreen else DangerRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Monthly Profit",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = currentMonthName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AppTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                monthlyProfitVal > 0.0 -> SuccessGreenContainer
                                monthlyProfitVal < 0.0 -> DangerRedContainer
                                else -> BrandNavySecondaryContainer
                            }
                        ) {
                            Text(
                                text = when {
                                    monthlyProfitVal > 0.0 -> "Profitable"
                                    monthlyProfitVal < 0.0 -> "Loss"
                                    else -> "Break-even"
                                },
                                color = when {
                                    monthlyProfitVal > 0.0 -> Color(0xFF065F46)
                                    monthlyProfitVal < 0.0 -> Color(0xFF991B1B)
                                    else -> AppTextSecondary
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = formatCurrency(monthlyProfitVal),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = if (monthlyProfitVal >= 0.0) MaterialTheme.colorScheme.onSurface else DangerRed
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = AppOutlineLight.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Sales",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = formatCurrency(monthlySales),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary
                                )
                            )
                        }

                        Column {
                            Text(
                                text = "Monthly Expenses",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = formatCurrency(monthlyGeneralExpenses),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DangerRed
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Monthly Salaries",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = formatCurrency(monthlySalaries),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DangerRed
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. RECENT SALES TRENDS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AppOutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_trend_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recent Sales Trends",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Daily revenue performance over the past 7 days",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BrandBluePrimaryContainer
                        ) {
                            Text(
                                text = "Last 7 Days",
                                color = BrandBluePrimary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val maxSaleValue = recentSalesTrendData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        recentSalesTrendData.forEach { (day, amount) ->
                            val heightFraction = (amount / maxSaleValue).toFloat().coerceIn(0.08f, 1.0f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = if (amount > 0) formatCurrency(amount).replace("Rs. ", "") else "0",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = AppTextSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (amount > 0) BrandBluePrimary else AppOutlineLight.copy(alpha = 0.5f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. PEOPLE & PARTNERS QUICK LINK
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AppOutlineLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPeople() }
                    .testTag("dashboard_people_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "People Directory",
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "People Directory",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Manage ${customers.size} Customers & ${employees.size} Employees",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Navigate to People",
                        tint = AppTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
