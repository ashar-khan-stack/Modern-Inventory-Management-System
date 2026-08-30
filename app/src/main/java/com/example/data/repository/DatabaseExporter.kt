package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.OrderJsonParser
import com.example.data.model.*
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePreferenceManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Result data class representing validation and metadata statistics of an exported backup file.
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
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
    val hasBusinessProfile: Boolean = false
)

/**
 * Unified DatabaseExporter utility for comprehensive export, audit, validation,
 * and single atomic Room database transaction restoration across all persistent tables.
 */
object DatabaseExporter {

    const val CURRENT_SCHEMA_VERSION = 1
    const val APP_VERSION = "1.0.0"

    /**
     * Exports all persistent Room tables and application configurations into a structured JSONObject.
     */
    suspend fun exportToJsonObject(
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager
    ): JSONObject {
        val root = JSONObject()
        root.put("backupVersion", CURRENT_SCHEMA_VERSION)
        root.put("appVersion", APP_VERSION)
        val backupDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        root.put("backupDate", backupDateStr)
        root.put("exportTimestamp", System.currentTimeMillis())

        // 1. Customers Table
        val customers = db.customerDao().getAllCustomersList()
        val customersArray = JSONArray()
        customers.forEach { c ->
            customersArray.put(customerToJson(c))
        }
        root.put("customers", customersArray)

        // 2. Sales Orders & Item Breakdown
        val sales = db.saleDao().getAllSalesList()
        val salesArray = JSONArray()
        val saleItemsArray = JSONArray()
        sales.forEach { s ->
            salesArray.put(saleToJson(s))
            val items = OrderJsonParser.jsonToSaleItems(s.itemsJson)
            items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("saleId", s.id)
                    put("invoiceNumber", s.invoiceNumber)
                    put("description", item.description)
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("quantity", item.quantity)
                    put("discountPercent", item.discountPercent)
                    put("subtotal", item.subtotal)
                }
                saleItemsArray.put(itemObj)
            }
        }
        root.put("sales", salesArray)
        root.put("saleItems", saleItemsArray)

        // 4. Expenses Table
        val expenses = db.expenseDao().getAllExpensesList()
        val expensesArray = JSONArray()
        expenses.forEach { e ->
            expensesArray.put(expenseToJson(e))
        }
        root.put("expenses", expensesArray)

        // 5. Employees Table
        val employees = db.employeeDao().getAllEmployeesList()
        val employeesArray = JSONArray()
        employees.forEach { emp ->
            employeesArray.put(employeeToJson(emp))
        }
        root.put("employees", employeesArray)

        // 6. Salaries Table
        val salaries = db.salaryDao().getAllSalariesList()
        val salariesArray = JSONArray()
        salaries.forEach { sal ->
            salariesArray.put(salaryToJson(sal))
        }
        root.put("salaryPayments", salariesArray)

        // 7. Users Table
        val users = db.userDao().getAllUsersList()
        val usersArray = JSONArray()
        users.forEach { u ->
            usersArray.put(userToJson(u))
        }
        root.put("users", usersArray)

        // 8. Products Table
        val products = db.productDao().getAllProductsList()
        val productsArray = JSONArray()
        products.forEach { p ->
            val pJson = productToJson(p)
            // Include image in backup as Base64 if it exists
            if (p.imageUrl.isNotBlank()) {
                val imgFile = File(p.imageUrl)
                if (imgFile.exists()) {
                    try {
                        val bytes = imgFile.readBytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        pJson.put("imageBase64", base64)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            productsArray.put(pJson)
        }
        root.put("products", productsArray)

        // 9. Business Profile Settings
        val profile = profileManager.profile.value
        root.put("businessProfile", businessProfileToJson(profile))

        // 10. Application Theme and System Preferences
        val settingsObj = JSONObject().apply {
            put("themeMode", themeManager.themeMode.value.name)
            put("currencySymbol", profile.currencySymbol)
            put("appVersion", APP_VERSION)
        }
        root.put("settings", settingsObj)

        return root
    }

    suspend fun exportToJsonString(
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager,
        indentSpaces: Int = 2
    ): String {
        return exportToJsonObject(db, profileManager, themeManager).toString(indentSpaces)
    }

    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            if (jsonString.isBlank()) {
                return BackupValidationResult(isValid = false, errorMessage = "Selected file is empty.")
            }

            val root = JSONObject(jsonString)
            val backupVersion = root.optInt("backupVersion", -1)
            if (backupVersion <= 0) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Invalid backup format: Missing or unsupported backup version."
                )
            }

            val appVersion = root.optString("appVersion", "Unknown")
            val backupDate = root.optString("backupDate", "Unknown")

            val customers = root.optJSONArray("customers")?.length() ?: 0
            val sales = root.optJSONArray("sales")?.length() ?: 0
            val saleItems = root.optJSONArray("saleItems")?.length() ?: 0
            val expenses = root.optJSONArray("expenses")?.length() ?: 0
            val employees = root.optJSONArray("employees")?.length() ?: 0
            val salaries = root.optJSONArray("salaryPayments")?.length() ?: 0
            val users = root.optJSONArray("users")?.length() ?: 0
            val hasBusinessProfile = root.has("businessProfile")

            BackupValidationResult(
                isValid = true,
                backupVersion = backupVersion,
                appVersion = appVersion,
                backupDate = backupDate,
                customersCount = customers,
                salesCount = sales,
                saleItemsCount = saleItems,
                expensesCount = expenses,
                employeesCount = employees,
                salariesCount = salaries,
                usersCount = users,
                hasBusinessProfile = hasBusinessProfile
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Corrupted or incompatible JSON file: ${e.localizedMessage}"
            )
        }
    }

    suspend fun importFromJsonString(
        jsonString: String,
        db: AppDatabase,
        profileManager: BusinessProfileManager,
        themeManager: ThemePreferenceManager,
        clearExistingBeforeRestore: Boolean = false
    ): Result<BackupValidationResult> {
        return try {
            val validation = validateBackupJson(jsonString)
            if (!validation.isValid) {
                return Result.failure(Exception(validation.errorMessage ?: "Invalid backup file"))
            }

            val root = JSONObject(jsonString)

            val parsedUsers = mutableListOf<UserEntity>()
            root.optJSONArray("users")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedUsers.add(jsonToUser(arr.getJSONObject(i)))
                }
            }

            val parsedCustomers = mutableListOf<CustomerEntity>()
            root.optJSONArray("customers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedCustomers.add(jsonToCustomer(arr.getJSONObject(i)))
                }
            }

            val parsedSales = mutableListOf<SaleOrderEntity>()
            root.optJSONArray("sales")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedSales.add(jsonToSale(arr.getJSONObject(i)))
                }
            }

            val parsedExpenses = mutableListOf<ExpenseEntity>()
            root.optJSONArray("expenses")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedExpenses.add(jsonToExpense(arr.getJSONObject(i)))
                }
            }

            val parsedEmployees = mutableListOf<EmployeeEntity>()
            root.optJSONArray("employees")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedEmployees.add(jsonToEmployee(arr.getJSONObject(i)))
                }
            }

            val parsedSalaries = mutableListOf<SalaryPaymentEntity>()
            root.optJSONArray("salaryPayments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parsedSalaries.add(jsonToSalary(arr.getJSONObject(i)))
                }
            }

            val parsedProducts = mutableListOf<ProductEntity>()
            val productImagesMap = mutableMapOf<Long, String>() // Map old ID or SKU to new restored path
            root.optJSONArray("products")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val pObj = arr.getJSONObject(i)
                    val product = jsonToProduct(pObj)
                    
                    // Restore image from Base64 if present
                    val base64 = pObj.optString("imageBase64", "")
                    if (base64.isNotBlank()) {
                        try {
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            val imagesDir = File(db.openHelper.readableDatabase.path).parentFile?.let { 
                                File(it.parentFile, "files/product_images") 
                            } ?: File("/data/data/com.example/files/product_images") // Fallback
                            
                            if (!imagesDir.exists()) imagesDir.mkdirs()
                            
                            val fileName = "restored_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
                            val file = File(imagesDir, fileName)
                            file.writeBytes(bytes)
                            
                            parsedProducts.add(product.copy(imageUrl = file.absolutePath))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            parsedProducts.add(product)
                        }
                    } else {
                        parsedProducts.add(product)
                    }
                }
            }

            // ATOMIC DATABASE TRANSACTION
            db.withTransaction {
                if (clearExistingBeforeRestore) {
                    db.salaryDao().deleteAllSalaries()
                    db.employeeDao().deleteAllEmployees()
                    db.expenseDao().deleteAllExpenses()
                    db.saleDao().deleteAllSales()
                    db.customerDao().deleteAllCustomers()
                    db.productDao().deleteAllProducts()
                    if (parsedUsers.isNotEmpty()) {
                        db.userDao().deleteAllUsers()
                    }
                }

                if (parsedUsers.isNotEmpty()) {
                    db.userDao().insertOrReplaceUsers(parsedUsers)
                }
                if (parsedCustomers.isNotEmpty()) {
                    db.customerDao().insertCustomers(parsedCustomers)
                }
                if (parsedProducts.isNotEmpty()) {
                    for (p in parsedProducts) {
                        db.productDao().insertProduct(p)
                    }
                }
                if (parsedSales.isNotEmpty()) {
                    db.saleDao().insertSales(parsedSales)
                }
                if (parsedExpenses.isNotEmpty()) {
                    db.expenseDao().insertExpenses(parsedExpenses)
                }
                if (parsedEmployees.isNotEmpty()) {
                    db.employeeDao().insertEmployees(parsedEmployees)
                }
                if (parsedSalaries.isNotEmpty()) {
                    db.salaryDao().insertSalaries(parsedSalaries)
                }
            }

            if (root.has("businessProfile")) {
                val profile = jsonToBusinessProfile(root.getJSONObject("businessProfile"))
                profileManager.saveProfile(profile)
            }

            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val themeModeStr = sObj.optString("themeMode", "")
                if (themeModeStr.isNotBlank()) {
                    try {
                        val mode = AppThemeMode.valueOf(themeModeStr)
                        themeManager.setThemeMode(mode)
                    } catch (_: Exception) {}
                }
            }

            Result.success(validation)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

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

    fun productToJson(p: ProductEntity): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("sku", p.sku)
        put("category", p.category)
        put("purchasePrice", p.purchasePrice)
        put("sellingPrice", p.sellingPrice)
        put("currentStock", p.currentStock)
        put("minStock", p.minStock)
        put("imageUrl", p.imageUrl)
        put("description", p.description)
        put("createdAt", p.createdAt)
    }

    fun jsonToProduct(o: JSONObject): ProductEntity = ProductEntity(
        id = o.optLong("id", 0L),
        name = o.optString("name"),
        sku = o.optString("sku"),
        category = o.optString("category"),
        purchasePrice = o.optDouble("purchasePrice", 0.0),
        sellingPrice = o.optDouble("sellingPrice", 0.0),
        currentStock = o.optInt("currentStock", 0),
        minStock = o.optInt("minStock", 0),
        imageUrl = o.optString("imageUrl", ""),
        description = o.optString("description", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun businessProfileToJson(profile: BusinessProfile): JSONObject = JSONObject().apply {
        put("companyName", profile.companyName)
        put("taxId", profile.taxId)
        put("currencySymbol", profile.currencySymbol)
        put("phone", profile.phone)
        put("email", profile.email)
        put("address", profile.address)
        put("website", profile.website)
        put("logoUrl", profile.logoUrl)
        put("isSaved", profile.isSaved)
    }

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
