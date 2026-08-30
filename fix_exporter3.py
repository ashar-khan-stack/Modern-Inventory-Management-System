import sys

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'r') as f:
    content = f.read()

# I will append the json helpers before the last }

json_helpers = """
    fun customerToJson(c: CustomerEntity): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("phone", c.phone)
        put("email", c.email)
        put("address", c.address)
        put("city", c.city)
        put("openingBalance", c.openingBalance)
        put("totalPurchases", c.totalPurchases)
        put("totalPaid", c.totalPaid)
        put("outstandingBalance", c.outstandingBalance)
        put("status", c.status)
    }

    fun jsonToCustomer(o: JSONObject): CustomerEntity = CustomerEntity(
        id = o.optLong("id", 0L),
        name = o.optString("name"),
        phone = o.optString("phone"),
        email = o.optString("email"),
        address = o.optString("address"),
        city = o.optString("city"),
        openingBalance = o.optDouble("openingBalance", 0.0),
        totalPurchases = o.optDouble("totalPurchases", 0.0),
        totalPaid = o.optDouble("totalPaid", 0.0),
        outstandingBalance = o.optDouble("outstandingBalance", 0.0),
        status = o.optString("status", "Active")
    )

    fun saleToJson(s: SaleOrderEntity): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("invoiceNumber", s.invoiceNumber)
        put("taxInvoiceNumber", s.taxInvoiceNumber)
        put("taxId", s.taxId)
        put("customerId", s.customerId)
        put("customerName", s.customerName)
        put("customerPhone", s.customerPhone)
        put("customerAddress", s.customerAddress)
        put("itemsJson", s.itemsJson)
        put("subtotal", s.subtotal)
        put("discountAmount", s.discountAmount)
        put("taxAmount", s.taxAmount)
        put("taxRatePercent", s.taxRatePercent)
        put("grandTotal", s.grandTotal)
        put("paidAmount", s.paidAmount)
        put("remainingBalance", s.remainingBalance)
        put("paymentMethod", s.paymentMethod)
        put("paymentStatus", s.paymentStatus)
        put("notes", s.notes)
        put("createdAt", s.createdAt)
    }

    fun jsonToSale(o: JSONObject): SaleOrderEntity = SaleOrderEntity(
        id = o.optLong("id", 0L),
        invoiceNumber = o.optString("invoiceNumber"),
        taxInvoiceNumber = o.optString("taxInvoiceNumber", ""),
        taxId = o.optString("taxId", ""),
        customerId = o.optLong("customerId"),
        customerName = o.optString("customerName"),
        customerPhone = o.optString("customerPhone", ""),
        customerAddress = o.optString("customerAddress", ""),
        itemsJson = o.optString("itemsJson", "[]"),
        subtotal = o.optDouble("subtotal", 0.0),
        discountAmount = o.optDouble("discountAmount", 0.0),
        taxAmount = o.optDouble("taxAmount", 0.0),
        taxRatePercent = o.optDouble("taxRatePercent", 0.0),
        grandTotal = o.optDouble("grandTotal", 0.0),
        paidAmount = o.optDouble("paidAmount", 0.0),
        remainingBalance = o.optDouble("remainingBalance", 0.0),
        paymentMethod = o.optString("paymentMethod", "Cash"),
        paymentStatus = o.optString("paymentStatus", "Paid"),
        notes = o.optString("notes", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun expenseToJson(e: ExpenseEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("category", e.category)
        put("description", e.description)
        put("amount", e.amount)
        put("paymentMethod", e.paymentMethod)
        put("notes", e.notes)
        put("date", e.date)
    }

    fun jsonToExpense(o: JSONObject): ExpenseEntity = ExpenseEntity(
        id = o.optLong("id", 0L),
        category = o.optString("category"),
        description = o.optString("description"),
        amount = o.optDouble("amount", 0.0),
        paymentMethod = o.optString("paymentMethod", "Cash"),
        notes = o.optString("notes", ""),
        date = o.optLong("date", System.currentTimeMillis())
    )

    fun employeeToJson(emp: EmployeeEntity): JSONObject = JSONObject().apply {
        put("id", emp.id)
        put("name", emp.name)
        put("phone", emp.phone)
        put("email", emp.email)
        put("address", emp.address)
        put("position", emp.position)
        put("joiningDate", emp.joiningDate)
        put("baseSalary", emp.baseSalary)
        put("status", emp.status)
    }

    fun jsonToEmployee(o: JSONObject): EmployeeEntity = EmployeeEntity(
        id = o.optLong("id", 0L),
        name = o.optString("name"),
        phone = o.optString("phone"),
        email = o.optString("email"),
        address = o.optString("address"),
        position = o.optString("position"),
        joiningDate = o.optString("joiningDate"),
        baseSalary = o.optDouble("baseSalary", 0.0),
        status = o.optString("status", "Active")
    )

    fun salaryToJson(sal: SalaryPaymentEntity): JSONObject = JSONObject().apply {
        put("id", sal.id)
        put("employeeId", sal.employeeId)
        put("employeeName", sal.employeeName)
        put("monthYear", sal.monthYear)
        put("baseSalary", sal.baseSalary)
        put("bonus", sal.bonus)
        put("deductions", sal.deductions)
        put("netSalary", sal.netSalary)
        put("paymentStatus", sal.paymentStatus)
        put("paymentMethod", sal.paymentMethod)
        put("paymentDate", sal.paymentDate)
        put("notes", sal.notes)
    }

    fun jsonToSalary(o: JSONObject): SalaryPaymentEntity = SalaryPaymentEntity(
        id = o.optLong("id", 0L),
        employeeId = o.optLong("employeeId"),
        employeeName = o.optString("employeeName"),
        monthYear = o.optString("monthYear"),
        baseSalary = o.optDouble("baseSalary", 0.0),
        bonus = o.optDouble("bonus", 0.0),
        deductions = o.optDouble("deductions", 0.0),
        netSalary = o.optDouble("netSalary", 0.0),
        paymentStatus = o.optString("paymentStatus", "Paid"),
        paymentMethod = o.optString("paymentMethod", "Bank Transfer"),
        paymentDate = o.optLong("paymentDate", System.currentTimeMillis()),
        notes = o.optString("notes", "")
    )

    fun userToJson(u: UserEntity): JSONObject = JSONObject().apply {
        put("id", u.id)
        put("firstName", u.firstName)
        put("lastName", u.lastName)
        put("email", u.email)
        put("passwordHash", u.passwordHash)
        put("salt", u.salt)
        put("isFingerprintEnabled", u.isFingerprintEnabled)
        put("securityQuestionsJson", u.securityQuestionsJson)
        put("createdAt", u.createdAt)
    }

    fun jsonToUser(o: JSONObject): UserEntity = UserEntity(
        id = o.optLong("id", 0L),
        firstName = o.optString("firstName"),
        lastName = o.optString("lastName"),
        email = o.optString("email"),
        passwordHash = o.optString("passwordHash"),
        salt = o.optString("salt"),
        isFingerprintEnabled = o.optBoolean("isFingerprintEnabled", false),
        securityQuestionsJson = o.optString("securityQuestionsJson", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun jsonToBusinessProfile(pObj: JSONObject): BusinessProfile = BusinessProfile(
        companyName = pObj.optString("companyName", ""),
        taxId = pObj.optString("taxId", ""),
        currencySymbol = pObj.optString("currencySymbol", "Rs"),
        phone = pObj.optString("phone", ""),
        email = pObj.optString("email", ""),
        address = pObj.optString("address", ""),
        website = pObj.optString("website", ""),
        logoUrl = pObj.optString("logoUrl", ""),
        isSaved = pObj.optBoolean("isSaved", true)
    )
}
"""
idx = content.rfind('}')
new_content = content[:idx] + json_helpers

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'w') as f:
    f.write(new_content)
print("Fixed JSON Helpers!")

