package com.example.ui.analytics

import com.example.data.model.ExpenseEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.data.model.SaleOrderEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class ProfitLossPeriodMode {
    DAY,
    MONTH,
    YEAR
}

enum class FinancialResultType {
    PROFIT,
    LOSS,
    BREAK_EVEN
}

data class FinancialPeriodMetrics(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val netLoss: Double = 0.0,
    val financialResultType: FinancialResultType = FinancialResultType.BREAK_EVEN,
    val totalPaidSales: Double = 0.0,
    val totalOutstandingSales: Double = 0.0,
    val salesCount: Int = 0,
    val expensesCount: Int = 0
)

data class PeriodComparisonData(
    val currentPeriodMetrics: FinancialPeriodMetrics,
    val previousPeriodMetrics: FinancialPeriodMetrics,
    val incomeDifference: Double,
    val expenseDifference: Double,
    val profitDifference: Double,
    val salesCountDifference: Int,
    val expensesCountDifference: Int,
    val incomePercentChange: Double?,
    val expensePercentChange: Double?,
    val profitPercentChange: Double?
)

data class DailyPoint(
    val dayOfMonth: Int,
    val dateFormatted: String,
    val income: Double,
    val expenses: Double,
    val netProfitLoss: Double
)

data class MonthlyPoint(
    val monthOfYear: Int,
    val monthName: String,
    val income: Double,
    val expenses: Double,
    val netProfitLoss: Double
)

object ProfitLossAnalytics {

    fun getDayStartAndEnd(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        return Pair(start, end)
    }

    fun getPreviousDayTimestamp(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return cal.timeInMillis
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthStartAndEnd(year: Int, month: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = startCal.timeInMillis

        val daysCount = getDaysInMonth(year, month)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.DAY_OF_MONTH, daysCount)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val end = endCal.timeInMillis
        return Pair(start, end)
    }

    fun getPreviousMonthTimestamp(year: Int, month: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        return cal.timeInMillis
    }

    fun getYearStartAndEnd(year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = startCal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val end = endCal.timeInMillis
        return Pair(start, end)
    }

    fun getPreviousYearTimestamp(year: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year - 1)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun calculatePeriodMetrics(
        sales: List<SaleOrderEntity>,
        expenses: List<ExpenseEntity>,
        salaryPayments: List<SalaryPaymentEntity>,
        startTime: Long,
        endTime: Long
    ): FinancialPeriodMetrics {
        val periodSales = sales.filter { it.createdAt in startTime..endTime }
        val periodExpenses = expenses.filter { it.date in startTime..endTime }
        val periodSalaries = salaryPayments.filter { it.paymentDate in startTime..endTime }

        val totalIncome = periodSales.sumOf { it.grandTotal }
        val totalPaidSales = periodSales.sumOf { it.paidAmount }
        val totalOutstandingSales = periodSales.sumOf { (it.grandTotal - it.paidAmount).coerceAtLeast(0.0) }

        val generalExpensesSum = periodExpenses.sumOf { it.amount }
        val salaryExpensesSum = periodSalaries.sumOf { it.netSalary }
        val totalExpenses = generalExpensesSum + salaryExpensesSum

        val netProfit = totalIncome - totalExpenses
        val netLoss = if (totalExpenses > totalIncome) totalExpenses - totalIncome else 0.0

        val resultType = when {
            netProfit > 0.001 -> FinancialResultType.PROFIT
            netProfit < -0.001 -> FinancialResultType.LOSS
            else -> FinancialResultType.BREAK_EVEN
        }

        return FinancialPeriodMetrics(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            netLoss = netLoss,
            financialResultType = resultType,
            totalPaidSales = totalPaidSales,
            totalOutstandingSales = totalOutstandingSales,
            salesCount = periodSales.size,
            expensesCount = periodExpenses.size + periodSalaries.size
        )
    }

    fun comparePeriods(
        current: FinancialPeriodMetrics,
        previous: FinancialPeriodMetrics
    ): PeriodComparisonData {
        val incomeDiff = current.totalIncome - previous.totalIncome
        val expenseDiff = current.totalExpenses - previous.totalExpenses
        val profitDiff = current.netProfit - previous.netProfit
        val salesCountDiff = current.salesCount - previous.salesCount
        val expCountDiff = current.expensesCount - previous.expensesCount

        val incomePct = calculatePercentChange(current.totalIncome, previous.totalIncome)
        val expensePct = calculatePercentChange(current.totalExpenses, previous.totalExpenses)
        val profitPct = calculatePercentChange(current.netProfit, previous.netProfit)

        return PeriodComparisonData(
            currentPeriodMetrics = current,
            previousPeriodMetrics = previous,
            incomeDifference = incomeDiff,
            expenseDifference = expenseDiff,
            profitDifference = profitDiff,
            salesCountDifference = salesCountDiff,
            expensesCountDifference = expCountDiff,
            incomePercentChange = incomePct,
            expensePercentChange = expensePct,
            profitPercentChange = profitPct
        )
    }

    fun calculatePercentChange(current: Double, previous: Double): Double? {
        if (abs(previous) < 0.0001) return null
        return ((current - previous) / abs(previous)) * 100.0
    }

    fun calculateDailyPointsForMonth(
        sales: List<SaleOrderEntity>,
        expenses: List<ExpenseEntity>,
        salaryPayments: List<SalaryPaymentEntity>,
        year: Int,
        month: Int
    ): List<DailyPoint> {
        val daysCount = getDaysInMonth(year, month)
        val monthShortName = SimpleDateFormat("MMM", Locale.US).format(
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }.time
        )

        val points = mutableListOf<DailyPoint>()
        for (day in 1..daysCount) {
            val dayCalStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = dayCalStart.timeInMillis
            val end = dayCalStart.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val metrics = calculatePeriodMetrics(sales, expenses, salaryPayments, start, end)
            points.add(
                DailyPoint(
                    dayOfMonth = day,
                    dateFormatted = "$monthShortName $day",
                    income = metrics.totalIncome,
                    expenses = metrics.totalExpenses,
                    netProfitLoss = metrics.netProfit
                )
            )
        }
        return points
    }

    fun calculateMonthlyPointsForYear(
        sales: List<SaleOrderEntity>,
        expenses: List<ExpenseEntity>,
        salaryPayments: List<SalaryPaymentEntity>,
        year: Int
    ): List<MonthlyPoint> {
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val points = mutableListOf<MonthlyPoint>()
        for (m in 0..11) {
            val (start, end) = getMonthStartAndEnd(year, m)
            val metrics = calculatePeriodMetrics(sales, expenses, salaryPayments, start, end)
            points.add(
                MonthlyPoint(
                    monthOfYear = m,
                    monthName = monthNames[m],
                    income = metrics.totalIncome,
                    expenses = metrics.totalExpenses,
                    netProfitLoss = metrics.netProfit
                )
            )
        }
        return points
    }
}
