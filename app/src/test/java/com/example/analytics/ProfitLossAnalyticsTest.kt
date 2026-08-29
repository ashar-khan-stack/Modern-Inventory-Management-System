package com.example.analytics

import com.example.data.model.ExpenseEntity
import com.example.data.model.SalaryPaymentEntity
import com.example.data.model.SaleOrderEntity
import com.example.ui.analytics.FinancialResultType
import com.example.ui.analytics.ProfitLossAnalytics
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ProfitLossAnalyticsTest {

    @Test
    fun testDayStartAndEnd() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 14, 30, 0)
        }
        val (start, end) = ProfitLossAnalytics.getDayStartAndEnd(cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))

        assertEquals(15, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
    }

    @Test
    fun testDaysInMonth_LeapYearVsNonLeapYear() {
        assertEquals(28, ProfitLossAnalytics.getDaysInMonth(2025, Calendar.FEBRUARY))
        assertEquals(29, ProfitLossAnalytics.getDaysInMonth(2028, Calendar.FEBRUARY))
        assertEquals(30, ProfitLossAnalytics.getDaysInMonth(2026, Calendar.APRIL))
        assertEquals(31, ProfitLossAnalytics.getDaysInMonth(2026, Calendar.AUGUST))
    }

    @Test
    fun testPreviousMonthTransition_JanToDec() {
        val prevTimestamp = ProfitLossAnalytics.getPreviousMonthTimestamp(2026, Calendar.JANUARY)
        val cal = Calendar.getInstance().apply { timeInMillis = prevTimestamp }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH))
    }

    @Test
    fun testCalculatePeriodMetrics_ProfitLossBreakEven() {
        val timestamp = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 10, 10, 0) }.timeInMillis

        val sales = listOf(
            SaleOrderEntity(
                id = 1, invoiceNumber = "INV-001", customerId = 1, customerName = "Test",
                itemsJson = "[]", subtotal = 1000.0, grandTotal = 1000.0, paidAmount = 800.0,
                remainingBalance = 200.0, paymentMethod = "Cash", paymentStatus = "Partial", createdAt = timestamp
            )
        )
        val expenses = listOf(
            ExpenseEntity(id = 1, category = "Rent", description = "Shop", amount = 300.0, paymentMethod = "Cash", date = timestamp)
        )
        val salaries = listOf(
            SalaryPaymentEntity(id = 1, employeeId = 1, employeeName = "John", monthYear = "August 2026", baseSalary = 200.0, netSalary = 200.0, paymentDate = timestamp)
        )

        val (start, end) = ProfitLossAnalytics.getDayStartAndEnd(timestamp)
        val metrics = ProfitLossAnalytics.calculatePeriodMetrics(sales, expenses, salaries, start, end)

        assertEquals(1000.0, metrics.totalIncome, 0.01)
        assertEquals(500.0, metrics.totalExpenses, 0.01)
        assertEquals(500.0, metrics.netProfit, 0.01)
        assertEquals(0.0, metrics.netLoss, 0.01)
        assertEquals(FinancialResultType.PROFIT, metrics.financialResultType)
        assertEquals(800.0, metrics.totalPaidSales, 0.01)
        assertEquals(200.0, metrics.totalOutstandingSales, 0.01)
        assertEquals(1, metrics.salesCount)
        assertEquals(2, metrics.expensesCount)
    }

    @Test
    fun testComparePeriods_ZeroDivisionProtection() {
        val curr = ProfitLossAnalytics.calculatePeriodMetrics(emptyList(), emptyList(), emptyList(), 0, 100)
        val prev = ProfitLossAnalytics.calculatePeriodMetrics(emptyList(), emptyList(), emptyList(), 0, 100)

        val comparison = ProfitLossAnalytics.comparePeriods(curr, prev)

        assertEquals(0.0, comparison.incomeDifference, 0.01)
        assertNull("Income percentage change must be null when previous period is zero", comparison.incomePercentChange)
        assertNull("Expense percentage change must be null when previous period is zero", comparison.expensePercentChange)
        assertNull("Profit percentage change must be null when previous period is zero", comparison.profitPercentChange)
    }

    @Test
    fun testComparePeriods_PercentageChangeCalculations() {
        val calCurr = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 15, 12, 0) }.timeInMillis
        val calPrev = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 14, 12, 0) }.timeInMillis

        val salesCurr = listOf(
            SaleOrderEntity(id = 1, invoiceNumber = "INV-001", customerId = 1, customerName = "A", itemsJson = "[]", subtotal = 1000.0, grandTotal = 1500.0, paidAmount = 1500.0, remainingBalance = 0.0, paymentMethod = "Cash", paymentStatus = "Paid", createdAt = calCurr)
        )
        val salesPrev = listOf(
            SaleOrderEntity(id = 2, invoiceNumber = "INV-002", customerId = 1, customerName = "A", itemsJson = "[]", subtotal = 1000.0, grandTotal = 1000.0, paidAmount = 1000.0, remainingBalance = 0.0, paymentMethod = "Cash", paymentStatus = "Paid", createdAt = calPrev)
        )

        val metricsCurr = ProfitLossAnalytics.calculatePeriodMetrics(salesCurr, emptyList(), emptyList(), calCurr - 1000, calCurr + 1000)
        val metricsPrev = ProfitLossAnalytics.calculatePeriodMetrics(salesPrev, emptyList(), emptyList(), calPrev - 1000, calPrev + 1000)

        val comparison = ProfitLossAnalytics.comparePeriods(metricsCurr, metricsPrev)

        assertEquals(500.0, comparison.incomeDifference, 0.01)
        assertNotNull(comparison.incomePercentChange)
        assertEquals(50.0, comparison.incomePercentChange!!, 0.01)
    }

    @Test
    fun testDailyPointsForMonth() {
        val points = ProfitLossAnalytics.calculateDailyPointsForMonth(
            sales = emptyList(),
            expenses = emptyList(),
            salaryPayments = emptyList(),
            year = 2026,
            month = Calendar.FEBRUARY
        )
        assertEquals(28, points.size)
        assertEquals(1, points.first().dayOfMonth)
        assertEquals(28, points.last().dayOfMonth)
    }

    @Test
    fun testMonthlyPointsForYear() {
        val points = ProfitLossAnalytics.calculateMonthlyPointsForYear(
            sales = emptyList(),
            expenses = emptyList(),
            salaryPayments = emptyList(),
            year = 2026
        )
        assertEquals(12, points.size)
        assertEquals("Jan", points.first().monthName)
        assertEquals("Dec", points.last().monthName)
    }
}
