package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class NavigateToInvoice(val sale: SaleOrderEntity) : UiEvent()
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = InventoryRepository(db)

    // Raw Flows from DB
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sales = repository.sales.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val employees = repository.employees.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val salaries = repository.salaries.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val salaryPayments = salaries

    // Aggregated Dashboard Summary State Flow
    val dashboardSummaryTotals: StateFlow<DashboardSummaryTotals> = combine(
        sales,
        expenses,
        customers
    ) { salesList, expList, customerList ->
        DashboardViewModel.calculateDashboardTotals(salesList, expList, customerList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DashboardSummaryTotals()
    )

    // UI Events Channel
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // Current Invoice for display/print
    private val _selectedInvoice = MutableStateFlow<SaleOrderEntity?>(null)
    val selectedInvoice = _selectedInvoice.asStateFlow()

    fun setSelectedInvoice(sale: SaleOrderEntity?) {
        _selectedInvoice.value = sale
    }

    // POS / Sale Builder State
    private val _posCustomer = MutableStateFlow<CustomerEntity?>(null)
    val posCustomer = _posCustomer.asStateFlow()
    val selectedCustomer = posCustomer

    private val _posCartItems = MutableStateFlow<List<SaleOrderItem>>(emptyList())
    val posCartItems = _posCartItems.asStateFlow()
    val posCart = posCartItems

    private val _posDiscountPercent = MutableStateFlow(0.0)
    val posDiscountPercent = _posDiscountPercent.asStateFlow()

    private val _posTaxRate = MutableStateFlow(0.0)
    val posTaxRate = _posTaxRate.asStateFlow()

    private val _posPaidAmount = MutableStateFlow<Double?>(null)
    val posPaidAmount = _posPaidAmount.asStateFlow()

    private val _posPaymentMethod = MutableStateFlow("")
    val posPaymentMethod = _posPaymentMethod.asStateFlow()

    private val _posNotes = MutableStateFlow("")
    val posNotes = _posNotes.asStateFlow()

    init {
        // App starts with a clean, empty database state
    }

    // --- POS Cart Operations ---
    fun selectPosCustomer(customer: CustomerEntity?) {
        _posCustomer.value = customer
    }

    fun updatePosItemQuantity(productId: Long, newQty: Int) {
        if (newQty <= 0) {
            removePosItem(productId)
            return
        }
        val currentList = _posCartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            val item = currentList[index]
            val discountAmt = (item.unitPrice * newQty) * (item.discountPercent / 100.0)
            val subtotal = (item.unitPrice * newQty) - discountAmt
            currentList[index] = item.copy(quantity = newQty, subtotal = subtotal)
            _posCartItems.value = currentList
        }
    }

    fun updatePosItemPrice(productId: Long, newPrice: Double) {
        val currentList = _posCartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            val item = currentList[index]
            val discountAmt = (newPrice * item.quantity) * (item.discountPercent / 100.0)
            val subtotal = (newPrice * item.quantity) - discountAmt
            currentList[index] = item.copy(unitPrice = newPrice, subtotal = subtotal)
            _posCartItems.value = currentList
        }
    }

    fun removePosItem(productId: Long) {
        _posCartItems.value = _posCartItems.value.filter { it.productId != productId }
    }

    fun setPosDiscountPercent(percent: Double) {
        _posDiscountPercent.value = percent.coerceIn(0.0, 100.0)
    }

    fun setPosTaxRate(rate: Double) {
        _posTaxRate.value = rate.coerceAtLeast(0.0)
    }

    fun setPosPaidAmount(amount: Double?) {
        _posPaidAmount.value = amount
    }

    fun setPosPaymentMethod(method: String) {
        _posPaymentMethod.value = method
    }

    fun setPosNotes(notes: String) {
        _posNotes.value = notes
    }

    fun clearPos() {
        _posCustomer.value = null
        _posCartItems.value = emptyList()
        _posDiscountPercent.value = 0.0
        _posTaxRate.value = 0.0
        _posPaidAmount.value = null
        _posPaymentMethod.value = ""
        _posNotes.value = ""
    }

    // --- Customer CRUD & Settlement ---
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

    fun recordCustomerSettlement(customerId: Long, amount: Double) {
        viewModelScope.launch {
            val success = repository.settleCustomerPayment(customerId, amount)
            if (success) {
                _uiEvents.emit(UiEvent.ShowToast("Payment of Rs. $amount recorded for customer."))
            }
        }
    }

    fun settleCustomerPayment(customerId: Long, amount: Double) = recordCustomerSettlement(customerId, amount)
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

    // --- Employee CRUD & Salary ---
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

    fun processEmployeeSalary(salary: SalaryPaymentEntity) {
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

    fun updateSalary(salary: SalaryPaymentEntity) = processEmployeeSalary(salary)

    fun deleteSalary(salary: SalaryPaymentEntity) {
        viewModelScope.launch {
            repository.deleteSalary(salary)
            _uiEvents.emit(UiEvent.ShowToast("Salary payment deleted."))
        }
    }

    // --- Aliases & Shortcuts for UI Screens ---
    fun selectCustomer(customer: CustomerEntity?) = selectPosCustomer(customer)
    fun updatePosCartItemQuantity(productId: Long, qty: Int) = updatePosItemQuantity(productId, qty)
    fun removeFromPosCart(productId: Long) = removePosItem(productId)
    fun clearPosCart() = clearPos()

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
                val inputPhoneDigits = com.example.ui.util.ValidationUtils.sanitizePkPhoneDigits(finalCustomer.phone)
                val existing = customers.value.find { cust ->
                    (inputPhoneDigits.isNotBlank() && com.example.ui.util.ValidationUtils.sanitizePkPhoneDigits(cust.phone) == inputPhoneDigits) ||
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

    fun deleteSale(sale: SaleOrderEntity) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            if (_selectedInvoice.value?.id == sale.id) {
                _selectedInvoice.value = null
            }
            _uiEvents.emit(UiEvent.ShowToast("Invoice #${sale.invoiceNumber} deleted successfully."))
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

    fun disburseSalary(salary: SalaryPaymentEntity) = processEmployeeSalary(salary)

    fun resetAndReseedData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearPos()
            _uiEvents.emit(UiEvent.ShowToast("All application data cleared!"))
        }
    }
}
