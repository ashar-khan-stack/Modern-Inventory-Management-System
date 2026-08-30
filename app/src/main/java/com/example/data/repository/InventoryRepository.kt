package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.OrderJsonParser
import com.example.data.model.*
import com.example.ui.util.OutstandingPaymentNotificationManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class InventoryRepository(private val db: AppDatabase) {

    private fun triggerNotificationSync() {
        AppDatabase.appContext?.let { ctx: android.content.Context ->
            OutstandingPaymentNotificationManager.syncOutstandingNotifications(ctx)
        }
    }

    // Flows for reactive UI
    val customers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val sales: Flow<List<SaleOrderEntity>> = db.saleDao().getAllSales()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val employees: Flow<List<EmployeeEntity>> = db.employeeDao().getAllEmployees()
    val salaries: Flow<List<SalaryPaymentEntity>> = db.salaryDao().getAllSalaries()

    // Customer Operations
    suspend fun addCustomer(customer: CustomerEntity): Long = db.customerDao().insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = db.customerDao().updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) {
        val hasSales = db.saleDao().getAllSalesList().any { it.customerId == customer.id }
        if (hasSales) {
            db.customerDao().updateCustomer(customer.copy(status = "Inactive"))
        } else {
            db.customerDao().deleteCustomer(customer)
        }
    }
    suspend fun settleCustomerPayment(customerId: Long, amountPaid: Double): Boolean {
        val customer = db.customerDao().getCustomerById(customerId) ?: return false
        
        // Record settlement as a special sale order with 0 total to keep ledger intact
        val settlementNo = "SET-${(10000..99999).random()}"
        val saleOrder = SaleOrderEntity(
            invoiceNumber = settlementNo,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            itemsJson = "[]",
            subtotal = 0.0,
            grandTotal = 0.0,
            paidAmount = amountPaid,
            remainingBalance = -amountPaid,
            paymentMethod = "Cash/Manual",
            paymentStatus = "Paid",
            notes = "Direct Account Settlement / Manual Payment",
            createdAt = System.currentTimeMillis()
        )
        db.saleDao().insertSale(saleOrder)

        val newPaid = customer.totalPaid + amountPaid
        val newOutstanding = (customer.outstandingBalance - amountPaid).coerceAtLeast(0.0)
        db.customerDao().updateCustomer(
            customer.copy(
                totalPaid = newPaid,
                outstandingBalance = newOutstanding
            )
        )
        triggerNotificationSync()
        return true
    }

    // Complete Sale Transaction (POS / Invoicing - Decoupled from Stock)
    suspend fun processSale(
        customer: CustomerEntity,
        items: List<SaleOrderItem>,
        subtotal: Double,
        discountAmount: Double,
        taxAmount: Double,
        taxRatePercent: Double,
        grandTotal: Double,
        paidAmount: Double,
        paymentMethod: String,
        notes: String
    ): Result<SaleOrderEntity> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Sale must have at least one item."))
        }

        // Generate invoice number, tax invoice number and tax id
        val datePrefix = SimpleDateFormat("yyMM", Locale.US).format(Date())
        val randomSuffix = (1000..9999).random()
        val invoiceNo = "INV-$datePrefix-$randomSuffix"

        val taxInvoiceNo = "TX-${(1000000..9999999).random()}-${('A'..'Z').random()}"
        val taxIdStr = "TX-${(100000..999999).random()}-${('A'..'Z').random()}"

        val remainingBalance = (grandTotal - paidAmount).coerceAtLeast(0.0)
        val paymentStatus = when {
            paidAmount >= grandTotal -> "Paid"
            paidAmount > 0.0 -> "Partial"
            else -> "Unpaid"
        }

        val saleOrder = SaleOrderEntity(
            invoiceNumber = invoiceNo,
            taxInvoiceNumber = taxInvoiceNo,
            taxId = taxIdStr,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            customerAddress = customer.address,
            itemsJson = OrderJsonParser.saleItemsToJson(items),
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            taxRatePercent = taxRatePercent,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            remainingBalance = remainingBalance,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        val saleId = db.saleDao().insertSale(saleOrder)
        val insertedSale = saleOrder.copy(id = saleId)

        // Update customer balance if customer exists
        if (customer.id > 0) {
            val freshCustomer = db.customerDao().getCustomerById(customer.id) ?: customer
            db.customerDao().updateCustomer(
                freshCustomer.copy(
                    totalPurchases = freshCustomer.totalPurchases + grandTotal,
                    totalPaid = freshCustomer.totalPaid + paidAmount,
                    outstandingBalance = freshCustomer.outstandingBalance + remainingBalance
                )
            )
        }

        triggerNotificationSync()
        return Result.success(insertedSale)
    }

    suspend fun deleteSale(sale: SaleOrderEntity) {
        if (sale.customerId > 0) {
            val customer = db.customerDao().getCustomerById(sale.customerId)
            if (customer != null) {
                val updatedPurchases = (customer.totalPurchases - sale.grandTotal).coerceAtLeast(0.0)
                val updatedPaid = (customer.totalPaid - sale.paidAmount).coerceAtLeast(0.0)
                val updatedOutstanding = (customer.outstandingBalance - sale.remainingBalance).coerceAtLeast(0.0)
                db.customerDao().updateCustomer(
                    customer.copy(
                        totalPurchases = updatedPurchases,
                        totalPaid = updatedPaid,
                        outstandingBalance = updatedOutstanding
                    )
                )
            }
        }
        db.saleDao().deleteSale(sale)
        triggerNotificationSync()
    }

    suspend fun updateSalePayment(sale: SaleOrderEntity, additionalAmount: Double): Result<SaleOrderEntity> {
        val existingSale = db.saleDao().getSaleById(sale.id) ?: sale

        if (additionalAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("Please enter a valid payment amount."))
        }

        if (additionalAmount > existingSale.remainingBalance + 0.01) {
            return Result.failure(IllegalArgumentException("Payment cannot be greater than the remaining balance."))
        }

        val newPaidAmount = existingSale.paidAmount + additionalAmount
        val newRemainingBalance = (existingSale.grandTotal - newPaidAmount).coerceAtLeast(0.0)
        val newPaymentStatus = when {
            newRemainingBalance <= 0.0 -> "Paid"
            newPaidAmount > 0.0 -> "Partial"
            else -> "Unpaid"
        }

        val updatedSale = existingSale.copy(
            paidAmount = newPaidAmount,
            remainingBalance = newRemainingBalance,
            paymentStatus = newPaymentStatus
        )

        db.saleDao().updateSale(updatedSale)

        if (existingSale.customerId > 0) {
            val customer = db.customerDao().getCustomerById(existingSale.customerId)
            if (customer != null) {
                val newCustomerPaid = customer.totalPaid + additionalAmount
                val newCustomerOutstanding = (customer.outstandingBalance - additionalAmount).coerceAtLeast(0.0)
                db.customerDao().updateCustomer(
                    customer.copy(
                        totalPaid = newCustomerPaid,
                        outstandingBalance = newCustomerOutstanding
                    )
                )
            }
        }

        triggerNotificationSync()
        return Result.success(updatedSale)
    }

    // Expenses
    suspend fun addExpense(expense: ExpenseEntity): Long {
        val id = db.expenseDao().insertExpense(expense)
        triggerNotificationSync()
        return id
    }
    suspend fun updateExpense(expense: ExpenseEntity) {
        db.expenseDao().updateExpense(expense)
        triggerNotificationSync()
    }
    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.expenseDao().deleteExpense(expense)
        triggerNotificationSync()
    }

    // Employees & Payroll
    suspend fun addEmployee(employee: EmployeeEntity): Long = db.employeeDao().insertEmployee(employee)
    suspend fun updateEmployee(employee: EmployeeEntity) = db.employeeDao().updateEmployee(employee)
    suspend fun deleteEmployee(employee: EmployeeEntity) {
        val hasSalaries = db.salaryDao().getAllSalariesList().any { it.employeeId == employee.id }
        if (hasSalaries) {
            db.employeeDao().updateEmployee(employee.copy(status = "Inactive"))
        } else {
            db.employeeDao().deleteEmployee(employee)
        }
    }

    suspend fun recordSalaryPayment(salary: SalaryPaymentEntity): Long {
        val id = db.salaryDao().insertSalary(salary)
        // If paid, also register as an expense under "Salary"
        if (salary.paymentStatus == "Paid") {
            db.expenseDao().insertExpense(
                ExpenseEntity(
                    category = "Salary",
                    description = "Salary payout to ${salary.employeeName} for ${salary.monthYear}",
                    amount = salary.netSalary,
                    paymentMethod = salary.paymentMethod,
                    notes = "Payroll transaction ref #${id}",
                    date = salary.paymentDate
                )
            )
        }
        triggerNotificationSync()
        return id
    }

    suspend fun updateSalary(salary: SalaryPaymentEntity) {
        db.salaryDao().updateSalary(salary)
        triggerNotificationSync()
    }
    suspend fun deleteSalary(salary: SalaryPaymentEntity) {
        db.salaryDao().deleteSalary(salary)
        triggerNotificationSync()
    }

    suspend fun clearAllData() {
        db.clearAllTables()
        triggerNotificationSync()
    }

    suspend fun resetAndReseedData() {
        db.clearAllTables()
        triggerNotificationSync()
    }

    suspend fun seedInitialDataIfEmpty() {
        // No-op: DB remains clean and empty for real user data
    }
}
typealias SalaryDaoPayment = SalaryPaymentEntity
