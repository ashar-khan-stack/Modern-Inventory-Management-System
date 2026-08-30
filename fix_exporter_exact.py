import sys

exporter_code = """package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.ui.theme.ThemePreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupValidationResult(
    val isValid: Boolean,
    val backupVersion: Int = 0,
    val appVersion: String = "",
    val backupDate: String = "",
    val customersCount: Int = 0,
    val salesCount: Int = 0,
    val saleItemsCount: Int = 0,
    val expensesCount: Int = 0,
    val employeesCount: Int = 0,
    val salariesCount: Int = 0,
    val usersCount: Int = 0,
    val hasBusinessProfile: Boolean = false,
    val errorMessage: String? = null
)

object DatabaseExporter {

    private const val BACKUP_VERSION = 1
    private const val APP_VERSION = "1.0.0"

    suspend fun exportToJsonString(
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager
    ): String {
        return withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("backupVersion", BACKUP_VERSION)
            root.put("appVersion", APP_VERSION)
            root.put("backupDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

            val customersArray = JSONArray()
            db.customerDao().getAllCustomersList().forEach { customersArray.put(customerToJson(it)) }
            root.put("customers", customersArray)

            val salesArray = JSONArray()
            db.saleDao().getAllSalesList().forEach { salesArray.put(saleToJson(it)) }
            root.put("sales", salesArray)

            val expensesArray = JSONArray()
            db.expenseDao().getAllExpensesList().forEach { expensesArray.put(expenseToJson(it)) }
            root.put("expenses", expensesArray)

            val employeesArray = JSONArray()
            db.employeeDao().getAllEmployeesList().forEach { employeesArray.put(employeeToJson(it)) }
            root.put("employees", employeesArray)

            val salariesArray = JSONArray()
            db.salaryDao().getAllSalariesList().forEach { salariesArray.put(salaryToJson(it)) }
            root.put("salaryPayments", salariesArray)

            val usersArray = JSONArray()
            db.userDao().getAllUsersList().forEach { usersArray.put(userToJson(it)) }
            root.put("users", usersArray)

            val profile = profileManager.profile.value
            val profileJson = JSONObject()
            profileJson.put("companyName", profile.companyName)
            profileJson.put("taxId", profile.taxId)
            profileJson.put("currencySymbol", profile.currencySymbol)
            profileJson.put("phone", profile.phone)
            profileJson.put("email", profile.email)
            profileJson.put("address", profile.address)
            profileJson.put("website", profile.website)
            profileJson.put("logoUrl", profile.logoUrl)
            root.put("businessProfile", profileJson)

            root.toString(4)
        }
    }

    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            val v = root.optInt("backupVersion", 0)
            if (v == 0) return BackupValidationResult(isValid = false, errorMessage = "Missing or invalid backupVersion")

            val salesArr = root.optJSONArray("sales")
            var totalSaleItems = 0
            if (salesArr != null) {
                for (i in 0 until salesArr.length()) {
                    val sObj = salesArr.optJSONObject(i)
                    if (sObj != null) {
                        val itemsJsonStr = sObj.optString("itemsJson", "")
                        if (itemsJsonStr.isNotBlank()) {
                            try {
                                val arr = JSONArray(itemsJsonStr)
                                totalSaleItems += arr.length()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            BackupValidationResult(
                isValid = true,
                backupVersion = v,
                appVersion = root.optString("appVersion", ""),
                backupDate = root.optString("backupDate", ""),
                customersCount = root.optJSONArray("customers")?.length() ?: 0,
                salesCount = salesArr?.length() ?: 0,
                saleItemsCount = totalSaleItems,
                expensesCount = root.optJSONArray("expenses")?.length() ?: 0,
                employeesCount = root.optJSONArray("employees")?.length() ?: 0,
                salariesCount = root.optJSONArray("salaryPayments")?.length() ?: 0,
                usersCount = root.optJSONArray("users")?.length() ?: 0,
                hasBusinessProfile = root.has("businessProfile")
            )
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Malformed JSON: ${e.message}")
        }
    }

    suspend fun importFromJsonString(
        jsonString: String,
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager,
        clearExistingBeforeRestore: Boolean = true
    ): Result<BackupValidationResult> {
        return withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val validation = validateBackupJson(jsonString)
                if (!validation.isValid) {
                    return@withContext Result.failure(Exception(validation.errorMessage))
                }

                if (clearExistingBeforeRestore) {
                    db.clearAllTables()
                }

                val customersArr = root.optJSONArray("customers")
                if (customersArr != null) {
                    for (i in 0 until customersArr.length()) {
                        db.customerDao().insertCustomer(jsonToCustomer(customersArr.getJSONObject(i)))
                    }
                }

                val salesArr = root.optJSONArray("sales")
                if (salesArr != null) {
                    for (i in 0 until salesArr.length()) {
                        db.saleDao().insertSale(jsonToSale(salesArr.getJSONObject(i)))
                    }
                }

                val expensesArr = root.optJSONArray("expenses")
                if (expensesArr != null) {
                    for (i in 0 until expensesArr.length()) {
                        db.expenseDao().insertExpense(jsonToExpense(expensesArr.getJSONObject(i)))
                    }
                }

                val empArr = root.optJSONArray("employees")
                if (empArr != null) {
                    for (i in 0 until empArr.length()) {
                        db.employeeDao().insertEmployee(jsonToEmployee(empArr.getJSONObject(i)))
                    }
                }

                val salArr = root.optJSONArray("salaryPayments")
                if (salArr != null) {
                    for (i in 0 until salArr.length()) {
                        db.salaryDao().insertSalary(jsonToSalary(salArr.getJSONObject(i)))
                    }
                }

                val usersArr = root.optJSONArray("users")
                if (usersArr != null) {
                    for (i in 0 until usersArr.length()) {
                        db.userDao().insertOrReplaceUser(jsonToUser(usersArr.getJSONObject(i)))
                    }
                }

                val profileJson = root.optJSONObject("businessProfile")
                if (profileJson != null) {
                    profileManager.saveProfile(jsonToBusinessProfile(profileJson))
                }

                Result.success(validation)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun customerToJson(c: CustomerEntity): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("name", c.name)
        o.put("phone", c.phone)
        o.put("email", c.email)
        o.put("address", c.address)
        o.put("city", c.city)
        o.put("openingBalance", c.openingBalance)
        o.put("totalPurchases", c.totalPurchases)
        o.put("totalPaid", c.totalPaid)
        o.put("outstandingBalance", c.outstandingBalance)
        o.put("status", c.status)
        return o
    }

    private fun jsonToCustomer(o: JSONObject): CustomerEntity {
        return CustomerEntity(
            id = 0,
            name = o.optString("name", ""),
            phone = o.optString("phone", ""),
            email = o.optString("email", ""),
            address = o.optString("address", ""),
            city = o.optString("city", ""),
            openingBalance = o.optDouble("openingBalance", 0.0),
            totalPurchases = o.optDouble("totalPurchases", 0.0),
            totalPaid = o.optDouble("totalPaid", 0.0),
            outstandingBalance = o.optDouble("outstandingBalance", 0.0),
            status = o.optString("status", "Active")
        )
    }

    private fun saleToJson(s: SaleOrderEntity): JSONObject {
        val o = JSONObject()
        o.put("id", s.id)
        o.put("invoiceNumber", s.invoiceNumber)
        o.put("taxInvoiceNumber", s.taxInvoiceNumber)
        o.put("taxId", s.taxId)
        o.put("customerId", s.customerId)
        o.put("customerName", s.customerName)
        o.put("customerPhone", s.customerPhone)
        o.put("customerAddress", s.customerAddress)
        o.put("itemsJson", s.itemsJson)
        o.put("subtotal", s.subtotal)
        o.put("discountAmount", s.discountAmount)
        o.put("taxAmount", s.taxAmount)
        o.put("taxRatePercent", s.taxRatePercent)
        o.put("grandTotal", s.grandTotal)
        o.put("paidAmount", s.paidAmount)
        o.put("remainingBalance", s.remainingBalance)
        o.put("paymentMethod", s.paymentMethod)
        o.put("paymentStatus", s.paymentStatus)
        o.put("notes", s.notes)
        o.put("createdAt", s.createdAt)
        return o
    }

    private fun jsonToSale(o: JSONObject): SaleOrderEntity {
        return SaleOrderEntity(
            id = 0,
            invoiceNumber = o.optString("invoiceNumber", ""),
            taxInvoiceNumber = o.optString("taxInvoiceNumber", ""),
            taxId = o.optString("taxId", ""),
            customerId = o.optLong("customerId", 0),
            customerName = o.optString("customerName", ""),
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
    }

    private fun expenseToJson(e: ExpenseEntity): JSONObject {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("category", e.category)
        o.put("description", e.description)
        o.put("amount", e.amount)
        o.put("paidAmount", e.paidAmount)
        o.put("remainingBalance", e.remainingBalance)
        o.put("paymentStatus", e.paymentStatus)
        o.put("paymentMethod", e.paymentMethod)
        o.put("notes", e.notes)
        o.put("date", e.date)
        return o
    }

    private fun jsonToExpense(o: JSONObject): ExpenseEntity {
        val amt = o.optDouble("amount", 0.0)
        val paid = o.optDouble("paidAmount", amt)
        return ExpenseEntity(
            id = 0,
            category = o.optString("category", "General"),
            description = o.optString("description", ""),
            amount = amt,
            paidAmount = paid,
            remainingBalance = o.optDouble("remainingBalance", (amt - paid).coerceAtLeast(0.0)),
            paymentStatus = o.optString("paymentStatus", "Paid"),
            paymentMethod = o.optString("paymentMethod", "Cash"),
            notes = o.optString("notes", ""),
            date = o.optLong("date", System.currentTimeMillis())
        )
    }

    private fun employeeToJson(emp: EmployeeEntity): JSONObject {
        val o = JSONObject()
        o.put("id", emp.id)
        o.put("name", emp.name)
        o.put("phone", emp.phone)
        o.put("email", emp.email)
        o.put("address", emp.address)
        o.put("position", emp.position)
        o.put("joiningDate", emp.joiningDate)
        o.put("baseSalary", emp.baseSalary)
        o.put("status", emp.status)
        return o
    }

    private fun jsonToEmployee(o: JSONObject): EmployeeEntity {
        return EmployeeEntity(
            id = 0,
            name = o.optString("name", ""),
            phone = o.optString("phone", ""),
            email = o.optString("email", ""),
            address = o.optString("address", ""),
            position = o.optString("position", ""),
            joiningDate = o.optString("joiningDate", ""),
            baseSalary = o.optDouble("baseSalary", 0.0),
            status = o.optString("status", "Active")
        )
    }

    private fun salaryToJson(sal: SalaryPaymentEntity): JSONObject {
        val o = JSONObject()
        o.put("id", sal.id)
        o.put("employeeId", sal.employeeId)
        o.put("employeeName", sal.employeeName)
        o.put("monthYear", sal.monthYear)
        o.put("baseSalary", sal.baseSalary)
        o.put("bonus", sal.bonus)
        o.put("deductions", sal.deductions)
        o.put("netSalary", sal.netSalary)
        o.put("paymentStatus", sal.paymentStatus)
        o.put("paymentMethod", sal.paymentMethod)
        o.put("paymentDate", sal.paymentDate)
        o.put("notes", sal.notes)
        return o
    }

    private fun jsonToSalary(o: JSONObject): SalaryPaymentEntity {
        return SalaryPaymentEntity(
            id = 0,
            employeeId = o.optLong("employeeId", 0),
            employeeName = o.optString("employeeName", ""),
            monthYear = o.optString("monthYear", ""),
            baseSalary = o.optDouble("baseSalary", 0.0),
            bonus = o.optDouble("bonus", 0.0),
            deductions = o.optDouble("deductions", 0.0),
            netSalary = o.optDouble("netSalary", 0.0),
            paymentStatus = o.optString("paymentStatus", "Paid"),
            paymentMethod = o.optString("paymentMethod", "Bank Transfer"),
            paymentDate = o.optLong("paymentDate", System.currentTimeMillis()),
            notes = o.optString("notes", "")
        )
    }

    private fun userToJson(u: UserEntity): JSONObject {
        val o = JSONObject()
        o.put("id", u.id)
        o.put("firstName", u.firstName)
        o.put("lastName", u.lastName)
        o.put("email", u.email)
        o.put("passwordHash", u.passwordHash)
        o.put("salt", u.salt)
        o.put("isFingerprintEnabled", u.isFingerprintEnabled)
        o.put("securityQuestionsJson", u.securityQuestionsJson)
        o.put("createdAt", u.createdAt)
        return o
    }

    private fun jsonToUser(o: JSONObject): UserEntity {
        return UserEntity(
            id = 0,
            firstName = o.optString("firstName", ""),
            lastName = o.optString("lastName", ""),
            email = o.optString("email", ""),
            passwordHash = o.optString("passwordHash", ""),
            salt = o.optString("salt", ""),
            isFingerprintEnabled = o.optBoolean("isFingerprintEnabled", false),
            securityQuestionsJson = o.optString("securityQuestionsJson", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun jsonToBusinessProfile(pObj: JSONObject): BusinessProfile {
        return BusinessProfile(
            companyName = pObj.optString("companyName", pObj.optString("businessName", "")),
            taxId = pObj.optString("taxId", ""),
            currencySymbol = pObj.optString("currencySymbol", "Rs"),
            phone = pObj.optString("phone", pObj.optString("contactNumber", "")),
            email = pObj.optString("email", ""),
            address = pObj.optString("address", ""),
            website = pObj.optString("website", ""),
            logoUrl = pObj.optString("logoUrl", "")
        )
    }
}
"""

with open('app/src/main/java/com/example/data/repository/DatabaseExporter.kt', 'w') as f:
    f.write(exporter_code)

print("DatabaseExporter updated with exact fields!")
