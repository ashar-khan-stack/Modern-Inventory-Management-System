package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.data.model.SaleOrderEntity
import com.example.ui.analytics.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfitLossAnalyticsSection(
    sales: List<SaleOrderEntity>,
    expenses: List<ExpenseEntity>,
    salaryPayments: List<SalaryPaymentEntity>,
    modifier: Modifier = Modifier
) {
    var periodMode by remember { mutableStateOf(ProfitLossPeriodMode.MONTH) }
    var selectedCal by remember { mutableStateOf(Calendar.getInstance()) }

    // Re-calculate state dynamically whenever dataset or period selection changes
    val (startAndEnd, prevTimestamp) = remember(periodMode, selectedCal.timeInMillis) {
        val year = selectedCal.get(Calendar.YEAR)
        val month = selectedCal.get(Calendar.MONTH)
        val time = selectedCal.timeInMillis

        when (periodMode) {
            ProfitLossPeriodMode.DAY -> {
                Pair(
                    ProfitLossAnalytics.getDayStartAndEnd(time),
                    ProfitLossAnalytics.getPreviousDayTimestamp(time)
                )
            }
            ProfitLossPeriodMode.MONTH -> {
                Pair(
                    ProfitLossAnalytics.getMonthStartAndEnd(year, month),
                    ProfitLossAnalytics.getPreviousMonthTimestamp(year, month)
                )
            }
            ProfitLossPeriodMode.YEAR -> {
                Pair(
                    ProfitLossAnalytics.getYearStartAndEnd(year),
                    ProfitLossAnalytics.getPreviousYearTimestamp(year)
                )
            }
        }
    }

    val prevStartAndEnd = remember(periodMode, prevTimestamp) {
        val prevCal = Calendar.getInstance().apply { timeInMillis = prevTimestamp }
        val pYear = prevCal.get(Calendar.YEAR)
        val pMonth = prevCal.get(Calendar.MONTH)

        when (periodMode) {
            ProfitLossPeriodMode.DAY -> ProfitLossAnalytics.getDayStartAndEnd(prevTimestamp)
            ProfitLossPeriodMode.MONTH -> ProfitLossAnalytics.getMonthStartAndEnd(pYear, pMonth)
            ProfitLossPeriodMode.YEAR -> ProfitLossAnalytics.getYearStartAndEnd(pYear)
        }
    }

    val currentMetrics = remember(sales, expenses, salaryPayments, startAndEnd) {
        ProfitLossAnalytics.calculatePeriodMetrics(sales, expenses, salaryPayments, startAndEnd.first, startAndEnd.second)
    }

    val previousMetrics = remember(sales, expenses, salaryPayments, prevStartAndEnd) {
        ProfitLossAnalytics.calculatePeriodMetrics(sales, expenses, salaryPayments, prevStartAndEnd.first, prevStartAndEnd.second)
    }

    val comparisonData = remember(currentMetrics, previousMetrics) {
        ProfitLossAnalytics.comparePeriods(currentMetrics, previousMetrics)
    }

    val dailyPoints = remember(sales, expenses, salaryPayments, periodMode, selectedCal) {
        if (periodMode == ProfitLossPeriodMode.MONTH) {
            ProfitLossAnalytics.calculateDailyPointsForMonth(
                sales, expenses, salaryPayments,
                selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH)
            )
        } else emptyList()
    }

    val monthlyPoints = remember(sales, expenses, salaryPayments, periodMode, selectedCal) {
        if (periodMode == ProfitLossPeriodMode.YEAR) {
            ProfitLossAnalytics.calculateMonthlyPointsForYear(
                sales, expenses, salaryPayments,
                selectedCal.get(Calendar.YEAR)
            )
        } else emptyList()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header & Period Mode Selector
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Profit & Loss Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Real-time income vs expense performance",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = BrandBluePrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = BrandBlueOnPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector Buttons (Day, Month, Year)
                PeriodSelectorSegmentedButtons(
                    selectedMode = periodMode,
                    onModeSelected = { mode -> periodMode = mode }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Navigation Header
                DatePeriodNavigationHeader(
                    periodMode = periodMode,
                    selectedCal = selectedCal,
                    onCalChanged = { newCal -> selectedCal = newCal }
                )
            }
        }

        // 1. Primary Financial Performance Summary Card
        ProfitLossSummaryCard(
            periodMode = periodMode,
            metrics = currentMetrics,
            selectedCal = selectedCal
        )

        // 2. Period Comparison Card (Today vs Prev Day, Month vs Prev Month, Year vs Prev Year)
        PeriodComparisonCard(
            periodMode = periodMode,
            comparison = comparisonData,
            selectedCal = selectedCal
        )

        // 3. Dynamic Interactive Financial Chart
        when (periodMode) {
            ProfitLossPeriodMode.DAY -> {
                DayHourlyBreakdownCard(
                    metrics = currentMetrics,
                    selectedCal = selectedCal
                )
            }
            ProfitLossPeriodMode.MONTH -> {
                MonthlyDailyTrendChart(
                    selectedMonthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCal.time),
                    dailyPoints = dailyPoints
                )
            }
            ProfitLossPeriodMode.YEAR -> {
                YearlyMonthlyTrendChart(
                    selectedYearName = "${selectedCal.get(Calendar.YEAR)}",
                    monthlyPoints = monthlyPoints
                )
            }
        }
    }
}

@Composable
fun PeriodSelectorSegmentedButtons(
    selectedMode: ProfitLossPeriodMode,
    onModeSelected: (ProfitLossPeriodMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProfitLossPeriodMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) BrandBluePrimary else Color.Transparent,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onModeSelected(mode) }
                        .testTag("period_tab_${mode.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                ProfitLossPeriodMode.DAY -> "Day"
                                ProfitLossPeriodMode.MONTH -> "Month"
                                ProfitLossPeriodMode.YEAR -> "Year"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatePeriodNavigationHeader(
    periodMode: ProfitLossPeriodMode,
    selectedCal: Calendar,
    onCalChanged: (Calendar) -> Unit
) {
    val context = LocalContext.current

    val titleText = remember(periodMode, selectedCal.timeInMillis) {
        when (periodMode) {
            ProfitLossPeriodMode.DAY -> SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US).format(selectedCal.time)
            ProfitLossPeriodMode.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCal.time)
            ProfitLossPeriodMode.YEAR -> SimpleDateFormat("yyyy", Locale.US).format(selectedCal.time)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val newCal = selectedCal.clone() as Calendar
                when (periodMode) {
                    ProfitLossPeriodMode.DAY -> newCal.add(Calendar.DAY_OF_MONTH, -1)
                    ProfitLossPeriodMode.MONTH -> newCal.add(Calendar.MONTH, -1)
                    ProfitLossPeriodMode.YEAR -> newCal.add(Calendar.YEAR, -1)
                }
                onCalChanged(newCal)
            },
            modifier = Modifier.testTag("prev_period_button")
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Period")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    val dialog = DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, y)
                                set(Calendar.MONTH, m)
                                set(Calendar.DAY_OF_MONTH, d)
                            }
                            onCalChanged(newCal)
                        },
                        selectedCal.get(Calendar.YEAR),
                        selectedCal.get(Calendar.MONTH),
                        selectedCal.get(Calendar.DAY_OF_MONTH)
                    )
                    dialog.show()
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = BrandBluePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = {
                val newCal = selectedCal.clone() as Calendar
                when (periodMode) {
                    ProfitLossPeriodMode.DAY -> newCal.add(Calendar.DAY_OF_MONTH, 1)
                    ProfitLossPeriodMode.MONTH -> newCal.add(Calendar.MONTH, 1)
                    ProfitLossPeriodMode.YEAR -> newCal.add(Calendar.YEAR, 1)
                }
                onCalChanged(newCal)
            },
            modifier = Modifier.testTag("next_period_button")
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Period")
        }
    }
}

@Composable
fun ProfitLossSummaryCard(
    periodMode: ProfitLossPeriodMode,
    metrics: FinancialPeriodMetrics,
    selectedCal: Calendar
) {
    val periodLabel = when (periodMode) {
        ProfitLossPeriodMode.DAY -> "Selected Day"
        ProfitLossPeriodMode.MONTH -> "Selected Month"
        ProfitLossPeriodMode.YEAR -> "Selected Year"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "$periodLabel Performance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Financial Result Hero Banner
            val (bannerBg, bannerTextColor, bannerTitle, bannerValue) = when (metrics.financialResultType) {
                FinancialResultType.PROFIT -> Quadruple(
                    SuccessGreenContainer,
                    Color(0xFF065F46),
                    "NET PROFIT",
                    formatCurrency(metrics.netProfit)
                )
                FinancialResultType.LOSS -> Quadruple(
                    DangerRedContainer,
                    DangerRed,
                    "NET LOSS",
                    formatCurrency(metrics.netLoss)
                )
                FinancialResultType.BREAK_EVEN -> Quadruple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurface,
                    "BREAK-EVEN",
                    formatCurrency(0.0)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bannerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bannerTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = bannerTextColor
                            )
                        )
                        Text(
                            text = bannerValue,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = bannerTextColor
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(bannerTextColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (metrics.financialResultType) {
                                FinancialResultType.PROFIT -> Icons.Default.TrendingUp
                                FinancialResultType.LOSS -> Icons.Default.TrendingDown
                                FinancialResultType.BREAK_EVEN -> Icons.Default.Remove
                            },
                            contentDescription = null,
                            tint = bannerTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income & Expense Core Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSubCard(
                    title = "Total Income / Sales",
                    value = formatCurrency(metrics.totalIncome),
                    countText = "${metrics.salesCount} Sales",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = SuccessGreen,
                    bgColor = SuccessGreenContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                MetricSubCard(
                    title = "Total Expenses",
                    value = formatCurrency(metrics.totalExpenses),
                    countText = "${metrics.expensesCount} Records",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = DangerRed,
                    bgColor = DangerRedContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Payment Status Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Paid Cash Received", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(formatCurrency(metrics.totalPaidSales), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrandNavySecondary))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Unpaid / Outstanding", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(formatCurrency(metrics.totalOutstandingSales), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WarningAmber))
                }
            }
        }
    }
}

@Composable
fun PeriodComparisonCard(
    periodMode: ProfitLossPeriodMode,
    comparison: PeriodComparisonData,
    selectedCal: Calendar
) {
    val (currLabel, prevLabel) = when (periodMode) {
        ProfitLossPeriodMode.DAY -> Pair("Selected Day", "Previous Day")
        ProfitLossPeriodMode.MONTH -> Pair("Selected Month", "Previous Month")
        ProfitLossPeriodMode.YEAR -> Pair("Selected Year", "Previous Year")
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    text = "Period Comparison ($currLabel vs $prevLabel)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = BrandBluePrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income Comparison Row
            FinancialComparisonRow(
                title = "Income / Sales",
                currValue = comparison.currentPeriodMetrics.totalIncome,
                prevValue = comparison.previousPeriodMetrics.totalIncome,
                difference = comparison.incomeDifference,
                percentChange = comparison.incomePercentChange,
                isHigherPositive = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Expense Comparison Row
            FinancialComparisonRow(
                title = "Expenses",
                currValue = comparison.currentPeriodMetrics.totalExpenses,
                prevValue = comparison.previousPeriodMetrics.totalExpenses,
                difference = comparison.expenseDifference,
                percentChange = comparison.expensePercentChange,
                isHigherPositive = false // Lower expenses = financially positive
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Net Profit / Loss Comparison Row
            FinancialComparisonRow(
                title = "Net Profit / Loss",
                currValue = comparison.currentPeriodMetrics.netProfit,
                prevValue = comparison.previousPeriodMetrics.netProfit,
                difference = comparison.profitDifference,
                percentChange = comparison.profitPercentChange,
                isHigherPositive = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Count comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sales Transactions Count:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "${comparison.currentPeriodMetrics.salesCount} vs ${comparison.previousPeriodMetrics.salesCount} (${if (comparison.salesCountDifference >= 0) "+${comparison.salesCountDifference}" else "${comparison.salesCountDifference}"})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun FinancialComparisonRow(
    title: String,
    currValue: Double,
    prevValue: Double,
    difference: Double,
    percentChange: Double?,
    isHigherPositive: Boolean
) {
    val isPositiveTrend = if (isHigherPositive) difference >= 0 else difference <= 0
    val trendColor = if (isPositiveTrend) SuccessGreen else DangerRed
    val trendIcon = if (difference >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )

            // Percentage indicator badge
            if (percentChange != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = trendColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${String.format("%.1f", Math.abs(percentChange))}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "No previous-period data",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Selected: ${formatCurrency(currValue)}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "Previous: ${formatCurrency(prevValue)}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "Diff: ${if (difference >= 0) "+" else ""}${formatCurrency(difference)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = trendColor)
            )
        }
    }
}

@Composable
fun MonthlyDailyTrendChart(
    selectedMonthName: String,
    dailyPoints: List<DailyPoint>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Breakdown ($selectedMonthName)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${dailyPoints.size} Days Performance Graph",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartLegendItem(label = "Income", color = SuccessGreen)
                    ChartLegendItem(label = "Expense", color = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasData = dailyPoints.any { it.income > 0 || it.expenses > 0 }

            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial data available for this month.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                val maxVal = (dailyPoints.flatMap { listOf(it.income, it.expenses) }.maxOrNull() ?: 100.0).coerceAtLeast(10.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val width = size.width
                    val height = size.height - 20f
                    val daysCount = dailyPoints.size
                    val step = width / daysCount

                    dailyPoints.forEachIndexed { i, point ->
                        val x = i * step + step / 2
                        val incH = (point.income / maxVal * height).toFloat()
                        val expH = (point.expenses / maxVal * height).toFloat()

                        // Income Bar
                        if (incH > 0) {
                            drawRoundRect(
                                color = SuccessGreen,
                                topLeft = Offset(x - 3f, height - incH),
                                size = Size(3f, incH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                            )
                        }

                        // Expense Bar
                        if (expH > 0) {
                            drawRoundRect(
                                color = DangerRed,
                                topLeft = Offset(x + 1f, height - expH),
                                size = Size(3f, expH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                            )
                        }
                    }
                }

                // X-Axis Day Labels (Sampled to avoid crowding)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val totalDays = dailyPoints.size
                    val sampleIndices = listOf(1, totalDays / 4, totalDays / 2, (totalDays * 3) / 4, totalDays)
                    sampleIndices.distinct().forEach { dayNum ->
                        Text(
                            text = "Day $dayNum",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyMonthlyTrendChart(
    selectedYearName: String,
    monthlyPoints: List<MonthlyPoint>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Annual Monthly Trends ($selectedYearName)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Jan - Dec Income vs Expense Graph",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartLegendItem(label = "Income", color = SuccessGreen)
                    ChartLegendItem(label = "Expense", color = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasData = monthlyPoints.any { it.income > 0 || it.expenses > 0 }

            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial data available for this year.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                val maxVal = (monthlyPoints.flatMap { listOf(it.income, it.expenses) }.maxOrNull() ?: 100.0).coerceAtLeast(10.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val width = size.width
                    val height = size.height - 20f
                    val groupWidth = width / 12f
                    val barW = (groupWidth * 0.35f).coerceAtLeast(4f)

                    monthlyPoints.forEachIndexed { i, point ->
                        val groupStartX = i * groupWidth + (groupWidth - barW * 2 - 4f) / 2
                        val incH = (point.income / maxVal * height).toFloat()
                        val expH = (point.expenses / maxVal * height).toFloat()

                        if (incH > 0) {
                            drawRoundRect(
                                color = SuccessGreen,
                                topLeft = Offset(groupStartX, height - incH),
                                size = Size(barW, incH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                            )
                        }

                        if (expH > 0) {
                            drawRoundRect(
                                color = DangerRed,
                                topLeft = Offset(groupStartX + barW + 2f, height - expH),
                                size = Size(barW, expH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                            )
                        }
                    }
                }

                // X-Axis Month Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    monthlyPoints.forEach { pt ->
                        Text(
                            text = pt.monthName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayHourlyBreakdownCard(
    metrics: FinancialPeriodMetrics,
    selectedCal: Calendar
) {
    val dayText = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(selectedCal.time)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Performance Overview ($dayText)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (metrics.salesCount == 0 && metrics.expensesCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales or expenses recorded on this date.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Day Sales Revenue", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(formatCurrency(metrics.totalIncome), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                    }
                    Column {
                        Text("Day Overhead Expenses", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(formatCurrency(metrics.totalExpenses), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DangerRed))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Day Net Result", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            formatCurrency(metrics.netProfit),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (metrics.netProfit >= 0) SuccessGreen else DangerRed
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSubCard(
    title: String,
    value: String,
    countText: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                countText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// Data helper container
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
