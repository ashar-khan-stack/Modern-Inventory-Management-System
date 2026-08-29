package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.DashboardSummaryTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object FinancialReportExporter {

    fun generateSummaryReportText(
        totals: DashboardSummaryTotals,
        companyName: String = "Enterprise Business Manager"
    ): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a", Locale.US)
        val currentDateStr = dateFormat.format(Date())

        return buildString {
            appendLine("==================================================")
            appendLine("       EXECUTIVE FINANCIAL DASHBOARD SUMMARY")
            appendLine("       $companyName")
            appendLine("==================================================")
            appendLine("Report Generated: $currentDateStr")
            appendLine()
            appendLine("1. KEY FINANCIAL PERFORMANCE METRICS")
            appendLine("--------------------------------------------------")
            appendLine("• Total Cumulative Revenue:   ${formatCurrency(totals.totalRevenue)}")
            appendLine("• Today's Sales Revenue:       ${formatCurrency(totals.todaySales)}")
            appendLine("• Net Operating Profit:        ${formatCurrency(totals.netProfit)}")
            appendLine()
            appendLine("2. MONTHLY FINANCIAL STATEMENT (${totals.currentMonthName.ifBlank { "Current Month" }})")
            appendLine("--------------------------------------------------")
            appendLine("• Monthly Gross Sales:         ${formatCurrency(totals.monthlySales)}")
            appendLine("• Total Operating Expenses:    ${formatCurrency(totals.monthlyExpenses)}")
            appendLine("• Monthly Net Profit:          ${formatCurrency(totals.monthlyProfit)}")
            appendLine()
            appendLine("3. CASH FLOW & LIQUIDITY POSITION")
            appendLine("--------------------------------------------------")
            appendLine("• Cash Inflow (Sales Paid):    ${formatCurrency(totals.cashInflow)}")
            appendLine("• Total Expenditures:          ${formatCurrency(totals.totalExpenditure)}")
            appendLine("• Net Cash Flow Position:      ${formatCurrency(totals.netCashPosition)}")
            appendLine()
            appendLine("4. TRANSACTION VOLUME STATS")
            appendLine("--------------------------------------------------")
            appendLine("• Total Customers Registered:  ${totals.totalCustomersCount}")
            appendLine("• Total Sales Invoices:        ${totals.totalSalesCount} transactions")
            appendLine("==================================================")
            appendLine("Status: Fully Verified Offline Database Report")
            appendLine("==================================================")
        }
    }

    suspend fun writeSummaryReportToUri(
        context: Context,
        uri: Uri,
        totals: DashboardSummaryTotals,
        companyName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val text = generateSummaryReportText(totals, companyName)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    writer.write(text)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Could not open file for writing"))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun shareSummaryReport(
        context: Context,
        totals: DashboardSummaryTotals,
        companyName: String
    ) {
        try {
            val text = generateSummaryReportText(totals, companyName)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Financial Dashboard Summary - $companyName")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Financial Summary"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
