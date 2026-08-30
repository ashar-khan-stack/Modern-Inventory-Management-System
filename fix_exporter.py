import sys

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'r') as f:
    content = f.read()

# The broken part is around line 123
# We need to close exportToJsonObject and start importFromJsonObject

fix = """        root.put("users", usersArray)

        return root
    }

    suspend fun importFromJsonObject(
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager,
        root: JSONObject
    ): Result<BackupValidationResult> {
        return try {
            val validation = BackupValidationResult(isValid = true)
            
            db.withTransaction {
                db.customerDao().deleteAllCustomers()
                db.saleDao().deleteAllSales()
                db.expenseDao().deleteAllExpenses()
                db.employeeDao().deleteAllEmployees()
                db.salaryDao().deleteAllSalaries()

                val parsedCustomers = mutableListOf<CustomerEntity>()
                if (root.has("customers")) {
                    val arr = root.getJSONArray("customers")
                    for (i in 0 until arr.length()) {
                        parsedCustomers.add(jsonToCustomer(arr.getJSONObject(i)))
                    }
                }
                if (parsedCustomers.isNotEmpty()) {
                    db.customerDao().insertCustomers(parsedCustomers)
                }

                val parsedSales = mutableListOf<SaleOrderEntity>()
                if (root.has("sales")) {
                    val arr = root.getJSONArray("sales")
                    for (i in 0 until arr.length()) {
                        parsedSales.add(jsonToSale(arr.getJSONObject(i)))
                    }
                }
                
                val parsedExpenses = mutableListOf<ExpenseEntity>()
                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        parsedExpenses.add(jsonToExpense(arr.getJSONObject(i)))
                    }
                }
                
                val parsedEmployees = mutableListOf<EmployeeEntity>()
                if (root.has("employees")) {
                    val arr = root.getJSONArray("employees")
                    for (i in 0 until arr.length()) {
                        parsedEmployees.add(jsonToEmployee(arr.getJSONObject(i)))
                    }
                }
                
                val parsedSalaries = mutableListOf<SalaryPaymentEntity>()
                if (root.has("salaryPayments")) {
                    val arr = root.getJSONArray("salaryPayments")
                    for (i in 0 until arr.length()) {
                        parsedSalaries.add(jsonToSalary(arr.getJSONObject(i)))
"""

start_idx = content.find('root.put("users", usersArray)')
end_idx = content.find('                if (parsedSales.isNotEmpty()) {')

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + fix + content[end_idx:]
    with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'w') as f:
        f.write(new_content)
    print("Fixed!")
else:
    print("Not found!")

