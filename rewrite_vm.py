import sys

content = """package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import com.example.ui.util.ValidationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class NavigateToInvoice(val sale: SaleOrderEntity) : UiEvent()
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
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
}

data class DashboardTotals(
    val totalSales: Double,
    val totalExpenses: Double,
    val totalOutstanding: Double
)
"""

with open('app/src/main/java/com/example/ui/viewmodel/InventoryViewModel.kt', 'w') as f:
    f.write(content)
print("Rewritten VM from scratch!")
