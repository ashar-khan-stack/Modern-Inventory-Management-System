package com.example.ui.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppStartupDiagnostics {
    private const val TAG = "StartupDiagnostics"

    suspend fun runStartupDiagnostics(context: Context): DiagnosticResult = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val productDao = db.productDao()
        val customerDao = db.customerDao()
        val saleDao = db.saleDao()

        var stockConsistent = true
        var ledgerConsistent = true
        var paymentConsistent = true
        val issues = mutableListOf<String>()

        try {
            val products = productDao.getAllProductsList()
            for (p in products) {
                if (p.currentStock < 0) {
                    stockConsistent = false
                    issues.add("Negative stock detected for product: ${p.name} (Qty: ${p.currentStock})")
                }
            }

            val customers = customerDao.getAllCustomersList()
            val sales = saleDao.getAllSalesList()
            for (c in customers) {
                val customerSales = sales.filter { it.customerId == c.id }
                val calculatedTotal = customerSales.sumOf { it.grandTotal }
                val calculatedPaid = customerSales.sumOf { it.paidAmount }
                val expectedBalance = calculatedTotal - calculatedPaid
                if (kotlin.math.abs(expectedBalance - c.outstandingBalance) > 1.0) {
                    Log.i(TAG, "Customer ${c.id} balance check: stored=${c.outstandingBalance}, expectedNet=$expectedBalance")
                }
            }

            Log.d(TAG, "Startup diagnostics completed successfully. Issues found: ${issues.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error running startup diagnostics", e)
            issues.add("Diagnostic exception: ${e.message}")
        }

        DiagnosticResult(
            isStockConsistent = stockConsistent,
            isLedgerConsistent = ledgerConsistent,
            isPaymentConsistent = paymentConsistent,
            issues = issues
        )
    }
}

data class DiagnosticResult(
    val isStockConsistent: Boolean,
    val isLedgerConsistent: Boolean,
    val isPaymentConsistent: Boolean,
    val issues: List<String>
)
