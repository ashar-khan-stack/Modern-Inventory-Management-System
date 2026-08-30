package com.example.ui.util

import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppStartupDiagnostics {
    suspend fun runStartupDiagnostics(db: AppDatabase) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("SQLiteDiag", "Running SQLite diagnostics...")
                val customerDao = db.customerDao()
                
                val customers = customerDao.getAllCustomersList()
                Log.d("SQLiteDiag", "Customers count: ${customers.size}")
                customers.take(3).forEach {
                    Log.d("SQLiteDiag", "  - ${it.name}")
                }
                
                Log.d("SQLiteDiag", "SQLite diagnostics completed successfully.")
            } catch (e: Exception) {
                Log.e("SQLiteDiag", "Error during SQLite diagnostics", e)
            }
        }
    }
}
