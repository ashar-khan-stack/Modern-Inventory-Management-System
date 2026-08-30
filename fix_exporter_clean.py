import sys

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'r') as f:
    content = f.read()

# I will find the first JSON converter and cut the file there, then rewrite them cleanly.
idx = content.find("fun customerToJson")
if idx != -1:
    content = content[:idx]

helpers = """fun customerToJson(c: CustomerEntity): JSONObject {
    val o = JSONObject()
    o.put("id", c.id)
    o.put("name", c.name)
    o.put("phone", c.phone)
    o.put("address", c.address)
    o.put("outstandingBalance", c.outstandingBalance)
    return o
}
fun jsonToCustomer(o: JSONObject): CustomerEntity {
    return CustomerEntity(
        id = 0,
        name = o.getString("name"),
        phone = o.getString("phone"),
        address = o.optString("address", ""),
        outstandingBalance = o.getDouble("outstandingBalance")
    )
}
fun saleToJson(s: SaleOrderEntity): JSONObject {
    val o = JSONObject()
    o.put("id", s.id)
    o.put("customerId", s.customerId)
    o.put("customerName", s.customerName)
    o.put("subtotal", s.subtotal)
    o.put("discountAmount", s.discountAmount)
    o.put("taxAmount", s.taxAmount)
    o.put("taxRatePercent", s.taxRatePercent)
    o.put("grandTotal", s.grandTotal)
    o.put("paidAmount", s.paidAmount)
    o.put("paymentMethod", s.paymentMethod)
    o.put("notes", s.notes)
    o.put("itemsJson", s.itemsJson)
    o.put("invoiceNumber", s.invoiceNumber)
    o.put("dateTimestamp", s.dateTimestamp)
    return o
}
fun jsonToSale(o: JSONObject): SaleOrderEntity {
    return SaleOrderEntity(
        id = 0,
        customerId = o.getLong("customerId"),
        customerName = o.getString("customerName"),
        subtotal = o.getDouble("subtotal"),
        discountAmount = o.getDouble("discountAmount"),
        taxAmount = o.getDouble("taxAmount"),
        taxRatePercent = o.getDouble("taxRatePercent"),
        grandTotal = o.getDouble("grandTotal"),
        paidAmount = o.getDouble("paidAmount"),
        paymentMethod = o.optString("paymentMethod", ""),
        notes = o.optString("notes", ""),
        itemsJson = o.getString("itemsJson"),
        invoiceNumber = o.getString("invoiceNumber"),
        dateTimestamp = o.getLong("dateTimestamp")
    )
}
fun expenseToJson(e: ExpenseEntity): JSONObject {
    val o = JSONObject()
    o.put("id", e.id)
    o.put("amount", e.amount)
    o.put("category", e.category)
    o.put("description", e.description)
    o.put("dateTimestamp", e.dateTimestamp)
    return o
}
fun jsonToExpense(o: JSONObject): ExpenseEntity {
    return ExpenseEntity(
        id = 0,
        amount = o.getDouble("amount"),
        category = o.getString("category"),
        description = o.getString("description"),
        dateTimestamp = o.getLong("dateTimestamp")
    )
}
fun employeeToJson(emp: EmployeeEntity): JSONObject {
    val o = JSONObject()
    o.put("id", emp.id)
    o.put("name", emp.name)
    o.put("role", emp.role)
    o.put("contactInfo", emp.contactInfo)
    o.put("baseSalary", emp.baseSalary)
    return o
}
fun jsonToEmployee(o: JSONObject): EmployeeEntity {
    return EmployeeEntity(
        id = 0,
        name = o.getString("name"),
        role = o.getString("role"),
        contactInfo = o.getString("contactInfo"),
        baseSalary = o.getDouble("baseSalary")
    )
}
fun salaryToJson(sal: SalaryPaymentEntity): JSONObject {
    val o = JSONObject()
    o.put("id", sal.id)
    o.put("employeeId", sal.employeeId)
    o.put("employeeName", sal.employeeName)
    o.put("baseSalary", sal.baseSalary)
    o.put("bonus", sal.bonus)
    o.put("deductions", sal.deductions)
    o.put("netSalary", sal.netSalary)
    o.put("paymentDateTimestamp", sal.paymentDateTimestamp)
    return o
}
fun jsonToSalary(o: JSONObject): SalaryPaymentEntity {
    return SalaryPaymentEntity(
        id = 0,
        employeeId = o.getLong("employeeId"),
        employeeName = o.getString("employeeName"),
        baseSalary = o.getDouble("baseSalary"),
        bonus = o.getDouble("bonus"),
        deductions = o.getDouble("deductions"),
        netSalary = o.getDouble("netSalary"),
        paymentDateTimestamp = o.getLong("paymentDateTimestamp")
    )
}
fun userToJson(u: UserEntity): JSONObject {
    val o = JSONObject()
    o.put("id", u.id)
    o.put("username", u.username)
    o.put("passwordHash", u.passwordHash)
    o.put("role", u.role)
    o.put("securityQuestionIndex", u.securityQuestionIndex)
    o.put("securityAnswerHash", u.securityAnswerHash)
    return o
}
fun jsonToUser(o: JSONObject): UserEntity {
    return UserEntity(
        id = 0,
        username = o.getString("username"),
        passwordHash = o.getString("passwordHash"),
        role = o.getString("role"),
        securityQuestionIndex = o.getInt("securityQuestionIndex"),
        securityAnswerHash = o.getString("securityAnswerHash")
    )
}
fun jsonToBusinessProfile(pObj: JSONObject): BusinessProfile {
    return BusinessProfile(
        businessName = pObj.optString("businessName", ""),
        ownerName = pObj.optString("ownerName", ""),
        contactNumber = pObj.optString("contactNumber", ""),
        email = pObj.optString("email", ""),
        address = pObj.optString("address", ""),
        taxId = pObj.optString("taxId", ""),
        currencySymbol = pObj.optString("currencySymbol", "Rs.")
    )
}
"""

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'w') as f:
    f.write(content + helpers)

print("Exporter cleaned")
