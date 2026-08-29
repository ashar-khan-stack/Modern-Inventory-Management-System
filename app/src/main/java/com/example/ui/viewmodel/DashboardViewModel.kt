package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing aggregated totals and metrics computed from the Room database
 * for the dashboard summary cards and financial overview widgets.
 */
data class DashboardSummaryTotals(
    val totalRevenue: Double = 0.0,
    val todaySales: Double = 0.0,
    val netProfit: Double = 0.0,
    val monthlySales: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val currentMonthName: String = "",
    val cashInflow: Double = 0.0,
    val totalExpenditure: Double = 0.0,
    val netCashPosition: Double = 0.0,
    val recentSalesTrends: List<Pair<String, Double>> = emptyList(),
    val totalCustomersCount: Int = 0,
    val totalSalesCount: Int = 0
)

/**
 * ViewModel that aggregates data from the Room database to provide calculated totals
 * needed for the dashboard summary cards.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = InventoryRepository(db)

    val sales: Flow<List<SaleOrderEntity>> = repository.sales
    val expenses: Flow<List<ExpenseEntity>> = repository.expenses
    val customers: Flow<List<CustomerEntity>> = repository.customers

    /**
     * Aggregated summary state reactively computed from Room Database flows.
     */
    val summaryTotals: StateFlow<DashboardSummaryTotals> = combine(
        sales,
        expenses,
        customers
    ) { salesList, expList, customerList ->
        calculateDashboardTotals(salesList, expList, customerList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardSummaryTotals()
    )

    companion object {
        /**
         * Pure function to aggregate Room database entities into calculated dashboard totals.
         */
        fun calculateDashboardTotals(
            sales: List<SaleOrderEntity>,
            expenses: List<ExpenseEntity>,
            customers: List<CustomerEntity>,
            now: Long = System.currentTimeMillis()
        ): DashboardSummaryTotals {
            val monthStartCal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStart = monthStartCal.timeInMillis

            val todayStartCal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayStartCal.timeInMillis

            // 1. Top financial metrics
            val totalSales = sales.sumOf { it.grandTotal }
            val todaySales = sales.filter { it.createdAt >= todayStart }.sumOf { it.grandTotal }
            val totalExpenses = expenses.sumOf { it.amount }

            val netProfit = totalSales - totalExpenses

            // 2. Monthly profit calculations
            val monthlySales = sales.filter { it.createdAt >= monthStart }.sumOf { it.grandTotal }
            val monthlyExpenses = expenses.filter { it.date >= monthStart }.sumOf { it.amount }
            val monthlyProfit = monthlySales - monthlyExpenses

            // 3. Cash flow & Liquidity
            val cashInflow = sales.sumOf { it.paidAmount }
            val totalExpenditure = totalExpenses
            val netCashPosition = cashInflow - totalExpenditure

            // 4. Recent 7-day sales trends
            val dateFormat = SimpleDateFormat("EEE", Locale.US)
            val daySalesMap = LinkedHashMap<String, Double>()
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dayName = dateFormat.format(c.time)
                daySalesMap[dayName] = 0.0
            }

            sales.forEach { sale ->
                val saleCal = Calendar.getInstance().apply { timeInMillis = sale.createdAt }
                val diffDays = (now - sale.createdAt) / (1000 * 60 * 60 * 24)
                if (diffDays in 0..6) {
                    val dayName = dateFormat.format(saleCal.time)
                    if (daySalesMap.containsKey(dayName)) {
                        daySalesMap[dayName] = (daySalesMap[dayName] ?: 0.0) + sale.grandTotal
                    }
                }
            }

            val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(now))

            return DashboardSummaryTotals(
                totalRevenue = totalSales,
                todaySales = todaySales,
                netProfit = netProfit,
                monthlySales = monthlySales,
                monthlyExpenses = monthlyExpenses,
                monthlyProfit = monthlyProfit,
                currentMonthName = monthName,
                cashInflow = cashInflow,
                totalExpenditure = totalExpenditure,
                netCashPosition = netCashPosition,
                recentSalesTrends = daySalesMap.toList(),
                totalCustomersCount = customers.size,
                totalSalesCount = sales.size
            )
        }
    }
}
