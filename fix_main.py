import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will just write a python script to replace the invocations correctly.
cust = """                    AppScreen.CUSTOMER_INFO -> CustomerInfoScreen(
                        customers = customers,
                        employees = employees,
                        salaryPayments = salaryPayments,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) },
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) },
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
                    )"""
content = re.sub(r'AppScreen\.CUSTOMER_INFO -> CustomerInfoScreen\([\s\S]*?viewModel\.settleCustomerPayment\(id, amt\) \}\n                    \)', cust, content)

emp = """                    AppScreen.EMPLOYEE_INFO -> EmployeeInfoScreen(
                        customers = customers,
                        employees = employees,
                        salaryPayments = salaryPayments,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) },
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) },
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
                    )"""
content = re.sub(r'AppScreen\.EMPLOYEE_INFO -> EmployeeInfoScreen\([\s\S]*?viewModel\.deleteEmployee\(it\) \}\n                    \)', emp, content)

sal = """                    AppScreen.SALARIES -> SalariesScreen(
                        customers = customers,
                        employees = employees,
                        salaryPayments = salaryPayments,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) },
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) },
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
                    )"""
content = re.sub(r'AppScreen\.SALARIES -> SalariesScreen\([\s\S]*?viewModel\.deleteSalary\(it\) \}\n                    \)', sal, content)

sales_history = """                    AppScreen.SALES -> SalesHistoryScreen(
                        products = products,
                        customers = customers,
                        pastSales = sales,
                        cartItems = posCart,
                        selectedCustomer = selectedCustomer,
                        onCustomerSelected = { viewModel.selectCustomer(it) },
                        onAddToCart = { },
                        onUpdateCartQty = { id, qty -> viewModel.updatePosCartItemQuantity(id, qty) },
                        onRemoveFromCart = { id -> viewModel.removeFromPosCart(id) },
                        onClearCart = { viewModel.clearPosCart() },
                        onProcessSale = { customer, items, paid, method, disc, tax, notes, onSuccess ->
                            viewModel.processCustomSale(customer, items, paid, method, disc, tax, notes, onSuccess)
                        },
                        onDeleteSale = { sale -> viewModel.deleteSale(sale) },
                        onUpdateSalePayment = { sale, amount, onSuccess ->
                            viewModel.updateSalePayment(sale, amount, onSuccess)
                        },
                        onViewInvoice = { sale ->
                            previousScreen = AppScreen.SALES
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        },
                        onAddCustomer = { viewModel.saveCustomer(it) },
                        onNavigateToNewSale = { currentScreen = AppScreen.NEW_SALE }
                    )"""
content = re.sub(r'AppScreen\.SALES -> SalesHistoryScreen\([\s\S]*?currentScreen = AppScreen\.NEW_SALE \}\n                    \)', sales_history, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
