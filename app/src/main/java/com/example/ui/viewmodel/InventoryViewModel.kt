package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import com.example.ui.util.ValidationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import android.content.Context
import androidx.lifecycle.ViewModelProvider

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class NavigateToInvoice(val sale: SaleOrderEntity) : UiEvent()
}

class InventoryViewModel(private val db: AppDatabase) : ViewModel() {
    constructor(application: Application) : this(AppDatabase.getInstance(application))

    private val repository = InventoryRepository(db)

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _selectedInvoice = MutableStateFlow<SaleOrderEntity?>(null)
    val selectedInvoice = _selectedInvoice.asStateFlow()

    private val _posPaidAmount = MutableStateFlow<Double?>(null)
    val posPaidAmount = _posPaidAmount.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow("")
    val posPaymentMethod = _posPaymentMethod.asStateFlow()

    private val _posNotes = MutableStateFlow("")
    val posNotes = _posNotes.asStateFlow()

    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sales = repository.sales.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val employees = repository.employees.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val salaries = repository.salaries.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val salaryPayments = salaries
    val bankAccounts = repository.bankAccounts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bankTransactions = repository.bankTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val vouchers = repository.vouchers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Bank Account actions
    fun saveBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            if (account.id == 0L) {
                repository.addBankAccount(account)
                _uiEvents.emit(UiEvent.ShowToast("Bank account '${account.bankName}' added!"))
            } else {
                repository.updateBankAccount(account)
                _uiEvents.emit(UiEvent.ShowToast("Bank account '${account.bankName}' updated!"))
            }
        }
    }

    fun deleteBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
            _uiEvents.emit(UiEvent.ShowToast("Bank account '${account.bankName}' deleted."))
        }
    }

    // Bank Transaction actions
    fun recordBankTransaction(
        bankAccountId: Long,
        type: String,
        amount: Double,
        description: String,
        refVoucher: String = "",
        targetAccountId: Long? = null,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val txId = repository.recordBankTransaction(
                bankAccountId, type, amount, description, refVoucher, targetAccountId, transactionDate
            )
            if (txId > 0) {
                _uiEvents.emit(UiEvent.ShowToast("Bank $type transaction recorded!"))
            } else {
                _uiEvents.emit(UiEvent.ShowToast("Failed to record bank transaction."))
            }
        }
    }

    // Voucher actions
    fun createVoucher(
        voucherType: String,
        accountName: String,
        description: String,
        amount: Double,
        isDebit: Boolean,
        bankAccountId: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val datePrefix = java.text.SimpleDateFormat("yyMM", java.util.Locale.US).format(java.util.Date())
            val vchNo = "VCH-$datePrefix-${(1000..9999).random()}"
            val debit = if (isDebit) amount else 0.0
            val credit = if (!isDebit) amount else 0.0
            val voucher = VoucherEntity(
                voucherNumber = vchNo,
                voucherType = voucherType,
                accountName = accountName,
                description = description,
                debit = debit,
                credit = credit,
                amount = amount,
                referenceNotes = notes,
                bankAccountId = bankAccountId
            )
            repository.recordVoucher(voucher)
            _uiEvents.emit(UiEvent.ShowToast("Voucher #$vchNo created!"))
        }
    }

    fun deleteVoucher(voucher: VoucherEntity) {
        viewModelScope.launch {
            repository.deleteVoucher(voucher)
            _uiEvents.emit(UiEvent.ShowToast("Voucher #${voucher.voucherNumber} deleted."))
        }
    }
    
    val dashboardSummaryTotals = combine(
        sales, expenses, customers
    ) { sList, eList, cList ->
        val totalSales = sList.sumOf { it.grandTotal }
        val totalExpenses = eList.sumOf { it.amount }
        val totalOutstanding = cList.sumOf { it.outstandingBalance }
        DashboardTotals(totalSales, totalExpenses, totalOutstanding)
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardTotals(0.0, 0.0, 0.0))

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            if (customer.id == 0L) {
                repository.addCustomer(customer)
                _uiEvents.emit(UiEvent.ShowToast("Customer '${customer.name}' added!"))
            } else {
                repository.updateCustomer(customer)
                _uiEvents.emit(UiEvent.ShowToast("Customer '${customer.name}' updated!"))
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _uiEvents.emit(UiEvent.ShowToast("Customer deleted."))
        }
    }

    fun settleCustomerPayment(customerId: Long, amount: Double) {
        viewModelScope.launch {
            val success = repository.settleCustomerPayment(customerId, amount)
            if (success) {
                _uiEvents.emit(UiEvent.ShowToast("Payment of Rs. $amount recorded for customer."))
            }
        }
    }

    fun saveExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            if (expense.id == 0L) {
                repository.addExpense(expense)
                _uiEvents.emit(UiEvent.ShowToast("Expense of Rs. ${expense.amount} recorded!"))
            } else {
                repository.updateExpense(expense)
                _uiEvents.emit(UiEvent.ShowToast("Expense updated!"))
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiEvents.emit(UiEvent.ShowToast("Expense deleted."))
        }
    }

    fun saveEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            if (employee.id == 0L) {
                repository.addEmployee(employee)
                _uiEvents.emit(UiEvent.ShowToast("Employee '${employee.name}' added!"))
            } else {
                repository.updateEmployee(employee)
                _uiEvents.emit(UiEvent.ShowToast("Employee '${employee.name}' updated!"))
            }
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            _uiEvents.emit(UiEvent.ShowToast("Employee deleted."))
        }
    }

    fun disburseSalary(salary: SalaryPaymentEntity) {
        viewModelScope.launch {
            if (salary.id == 0L) {
                repository.recordSalaryPayment(salary)
                _uiEvents.emit(UiEvent.ShowToast("Salary of Rs. ${salary.netSalary} processed for ${salary.employeeName}!"))
            } else {
                repository.updateSalary(salary)
                _uiEvents.emit(UiEvent.ShowToast("Salary record updated for ${salary.employeeName}!"))
            }
        }
    }

    fun updateSalary(salary: SalaryPaymentEntity) = disburseSalary(salary)

    fun deleteSalary(salary: SalaryPaymentEntity) {
        viewModelScope.launch {
            repository.deleteSalary(salary)
            _uiEvents.emit(UiEvent.ShowToast("Salary payment deleted."))
        }
    }

    fun processCustomSale(
        customer: CustomerEntity,
        items: List<SaleOrderItem>,
        paidAmount: Double,
        paymentMethod: String,
        discountAmount: Double,
        taxRatePercent: Double,
        notes: String,
        onSuccess: (SaleOrderEntity) -> Unit
    ) {
        if (items.isEmpty()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Please add at least one item.")) }
            return
        }

        val subtotal = items.sumOf { it.subtotal }
        val discountedSubtotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val taxAmt = discountedSubtotal * (taxRatePercent / 100.0)
        val grandTotal = discountedSubtotal + taxAmt

        viewModelScope.launch {
            var finalCustomer = customer
            if (finalCustomer.id == 0L && finalCustomer.name.isNotBlank()) {
                val inputPhoneDigits = ValidationUtils.sanitizePkPhoneDigits(finalCustomer.phone)
                val existing = customers.value.find { cust ->
                    (inputPhoneDigits.isNotBlank() && ValidationUtils.sanitizePkPhoneDigits(cust.phone) == inputPhoneDigits) ||
                    cust.name.equals(finalCustomer.name.trim(), ignoreCase = true)
                }
                if (existing != null) {
                    finalCustomer = existing
                } else {
                    val newId = repository.addCustomer(finalCustomer)
                    finalCustomer = finalCustomer.copy(id = newId)
                }
            }

            val result = repository.processSale(
                customer = finalCustomer,
                items = items,
                subtotal = subtotal,
                discountAmount = discountAmount,
                taxAmount = taxAmt,
                taxRatePercent = taxRatePercent,
                grandTotal = grandTotal,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                notes = notes
            )
            result.onSuccess { sale ->
                _selectedInvoice.value = sale
                _uiEvents.emit(UiEvent.ShowToast("Sale recorded! Invoice #${sale.invoiceNumber} generated."))
                onSuccess(sale)
            }.onFailure { err ->
                _uiEvents.emit(UiEvent.ShowToast("Failed to process sale: ${err.message}"))
            }
        }
    }

    fun updateSalePayment(
        sale: SaleOrderEntity,
        additionalAmount: Double,
        onSuccess: (SaleOrderEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateSalePayment(sale, additionalAmount)
            result.onSuccess { updatedSale ->
                if (_selectedInvoice.value?.id == updatedSale.id) {
                    _selectedInvoice.value = updatedSale
                }
                _uiEvents.emit(UiEvent.ShowToast("Payment updated for Invoice #${updatedSale.invoiceNumber}!"))
                onSuccess(updatedSale)
            }.onFailure { err ->
                _uiEvents.emit(UiEvent.ShowToast(err.message ?: "Failed to update payment"))
            }
        }
    }

    fun deleteSale(sale: SaleOrderEntity) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            if (_selectedInvoice.value?.id == sale.id) {
                _selectedInvoice.value = null
            }
            _uiEvents.emit(UiEvent.ShowToast("Invoice #${sale.invoiceNumber} deleted successfully."))
        }
    }

    fun resetAndReseedData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiEvents.emit(UiEvent.ShowToast("All application data cleared!"))
        }
    }
    companion object {
        fun saveImageUriToAppStorage(context: android.content.Context, uri: android.net.Uri, oldPath: String? = null): String {
            return try {
                val imagesDir = java.io.File(context.filesDir, "product_images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val fileName = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
                val file = java.io.File(imagesDir, fileName)

                // First decode bounds to check image dimensions
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream, null, options)
                }

                // Calculate sample size for max 1024px dimension
                var sampleSize = 1
                val maxDim = maxOf(options.outWidth, options.outHeight)
                if (maxDim > 1024) {
                    sampleSize = Math.round(maxDim.toFloat() / 1024f)
                }

                // Decode downsampled bitmap
                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (bitmap != null) {
                    file.outputStream().use { output ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, output)
                    }
                    bitmap.recycle()
                    file.absolutePath
                } else {
                    // Fallback to direct stream copy if bitmap decoding failed
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                }
            } catch (e: Exception) {
                uri.toString()
            }
        }
    }
}

data class DashboardTotals(
    val totalSales: Double,
    val totalExpenses: Double,
    val totalOutstanding: Double
)

class InventoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InventoryViewModel(AppDatabase.getInstance(context)) as T
    }
}
