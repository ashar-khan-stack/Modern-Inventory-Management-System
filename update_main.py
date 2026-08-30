import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace NEW_SALE block
new_sale_replacement = """                    AppScreen.NEW_SALE -> NewSaleScreen(
                        products = products,
                        customers = customers,
                        onProcessSale = { customer, items, paid, method, disc, tax, notes, onSuccess ->
                            viewModel.processCustomSale(customer, items, paid, method, disc, tax, notes, onSuccess)
                        },
                        onViewInvoice = { sale ->
                            previousScreen = AppScreen.NEW_SALE
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        },
                        onAddCustomer = { viewModel.saveCustomer(it) }
                    )"""
content = re.sub(r'AppScreen\.NEW_SALE -> SalesScreen\([\s\S]*?startInCreateMode = true\n                    \)', new_sale_replacement, content)

# Replace SALES block
sales_replacement = """                    AppScreen.SALES -> SalesHistoryScreen(
                        customers = customers,
                        pastSales = sales,
                        onDeleteSale = { sale -> viewModel.deleteSale(sale) },
                        onUpdateSalePayment = { sale, amount, onSuccess ->
                            viewModel.updateSalePayment(sale, amount, onSuccess)
                        },
                        onViewInvoice = { sale ->
                            previousScreen = AppScreen.SALES
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        },
                        onNavigateToNewSale = { currentScreen = AppScreen.NEW_SALE }
                    )"""
content = re.sub(r'AppScreen\.SALES -> SalesScreen\([\s\S]*?startInCreateMode = false\n                    \)', sales_replacement, content)

# Replace CUSTOMER_INFO
cust_replacement = """                    AppScreen.CUSTOMER_INFO -> CustomerInfoScreen(
                        customers = customers,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) }
                    )"""
content = re.sub(r'AppScreen\.CUSTOMER_INFO -> PeopleScreen\([\s\S]*?forceTab = 0\n                    \)', cust_replacement, content)

# Replace EMPLOYEE_INFO
emp_replacement = """                    AppScreen.EMPLOYEE_INFO -> EmployeeInfoScreen(
                        employees = employees,
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) }
                    )"""
content = re.sub(r'AppScreen\.EMPLOYEE_INFO -> PeopleScreen\([\s\S]*?forceEmployeeSubTab = 0\n                    \)', emp_replacement, content)

# Replace SALARIES
sal_replacement = """                    AppScreen.SALARIES -> SalariesScreen(
                        employees = employees,
                        salaryPayments = salaryPayments,
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
                    )"""
content = re.sub(r'AppScreen\.SALARIES -> PeopleScreen\([\s\S]*?forceEmployeeSubTab = 1\n                    \)', sal_replacement, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
