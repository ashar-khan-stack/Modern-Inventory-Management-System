package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private val testQuestions = listOf(
        Pair("What was the name of your first school?", "School"),
        Pair("What was your childhood nickname?", "Nick"),
        Pair("What was the name of your favorite childhood teacher?", "Teacher"),
        Pair("What was the name of your first pet?", "Pet"),
        Pair("What was your favorite childhood game?", "Game")
    )

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertTrue(appName.isNotEmpty())
    }

    @Test
    fun `test password policy validation rules`() {
        fun validatePassword(pwd: String): Boolean {
            val minLen = pwd.length >= 8
            val upper = pwd.any { it.isUpperCase() }
            val lower = pwd.any { it.isLowerCase() }
            val digit = pwd.any { it.isDigit() }
            val special = pwd.any { !it.isLetterOrDigit() }
            return minLen && upper && lower && digit && special
        }

        assertFalse("Too short", validatePassword("Pass1!"))
        assertFalse("Missing uppercase", validatePassword("password123!"))
        assertFalse("Missing lowercase", validatePassword("PASSWORD123!"))
        assertFalse("Missing digit", validatePassword("Password!@#"))
        assertFalse("Missing special char", validatePassword("Password123"))
        assertTrue("Valid strong password", validatePassword("Test@1234"))
    }

    @Test
    fun `test name and email regex rules`() {
        val nameRegex = Regex("^[a-zA-ZÀ-ÿ' -]+$")
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        // Name validation
        assertTrue(nameRegex.matches("Ali"))
        assertTrue(nameRegex.matches("Khan"))
        assertTrue(nameRegex.matches("Jean-Luc"))
        assertTrue(nameRegex.matches("O'Connor"))
        assertFalse(nameRegex.matches("123"))
        assertFalse(nameRegex.matches("Ali123"))

        // Email validation
        assertFalse(emailRegex.matches("a"))
        assertFalse(emailRegex.matches("abc@"))
        assertFalse(emailRegex.matches("abc@gmail"))
        assertTrue(emailRegex.matches("abc@gmail.com"))
        assertTrue(emailRegex.matches("admin.test@company.co.uk"))
    }

    @Test
    fun `test auth repository registration and login`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = AuthRepository.getInstance(context)

        val regResult = authRepo.register(
            firstName = "Ali",
            lastName = "Khan",
            email = "ali.khan@inventorymaster.com",
            password = "SecurePassword123!",
            securityQuestions = testQuestions
        )

        assertTrue(regResult.isSuccess)
        val session = regResult.getOrThrow()
        assertEquals("ali.khan@inventorymaster.com", session.email)
        assertEquals("Ali", session.firstName)
        assertEquals("Khan", session.lastName)

        // Login with correct credentials
        val loginResult = authRepo.login("ali.khan@inventorymaster.com", "SecurePassword123!")
        assertTrue(loginResult.isSuccess)
        assertEquals("ali.khan@inventorymaster.com", loginResult.getOrThrow().email)

        // Login with biometric
        val bioResult = authRepo.loginWithBiometric("ali.khan@inventorymaster.com")
        assertTrue(bioResult.isSuccess)
        assertEquals("ali.khan@inventorymaster.com", bioResult.getOrThrow().email)

        // Test Find User by Email
        val findResult = authRepo.findUserByEmail("ali.khan@inventorymaster.com")
        assertTrue("Find user by email should succeed", findResult.isSuccess)
        assertEquals("Ali", findResult.getOrThrow().firstName)

        // Test Password Reset
        val resetResult = authRepo.resetPassword("ali.khan@inventorymaster.com", "NewStrongPass999#")
        assertTrue("Password reset should succeed", resetResult.isSuccess)

        // Verify login with old password fails
        val oldLoginResult = authRepo.login("ali.khan@inventorymaster.com", "SecurePassword123!")
        assertFalse("Old password should be rejected", oldLoginResult.isSuccess)

        // Verify login with new password succeeds
        val newLoginResult = authRepo.login("ali.khan@inventorymaster.com", "NewStrongPass999#")
        assertTrue("New password should be accepted", newLoginResult.isSuccess)
    }

    @Test
    fun `test business profile manager lifecycle`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profileManager = com.example.data.repository.BusinessProfileManager.getInstance(context)

        // Save a new profile
        val sampleProfile = com.example.data.repository.BusinessProfile(
            companyName = "Acme Global Solutions",
            phone = "+1 (555) 987-6543",
            email = "contact@acmeglobal.com",
            address = "742 Evergreen Terrace, Springfield",
            taxId = "US-987654321",
            currencySymbol = "$"
        )
        profileManager.saveProfile(sampleProfile)

        // Read and verify
        val saved = profileManager.profile.value
        assertEquals("Acme Global Solutions", saved.companyName)
        assertEquals("contact@acmeglobal.com", saved.email)
        assertEquals("+1 (555) 987-6543", saved.phone)
        assertTrue("Profile should now be marked as saved", saved.isSaved)

        // Delete profile
        profileManager.deleteProfile()
        val afterDelete = profileManager.profile.value
        assertFalse("Profile should not be marked saved after deletion", afterDelete.isSaved)
        assertEquals("", afterDelete.companyName)
    }

    @Test
    fun `test backup manager full round trip export and import`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getInstance(context)
        val backupManager = com.example.data.repository.BackupManager(context, db)
        val profileManager = com.example.data.repository.BusinessProfileManager.getInstance(context)

        // 1. Seed some sample data
        profileManager.saveProfile(
            com.example.data.repository.BusinessProfile(
                companyName = "Apex Warehouses",
                phone = "1234567890",
                email = "apex@warehouses.com",
                address = "100 Industrial Parkway"
            )
        )

        val customerDao = db.customerDao()

        val sampleCustomer = com.example.data.model.CustomerEntity(
            id = 201L,
            name = "Johnathan Doe",
            phone = "555-0199",
            email = "john.doe@sample.com",
            address = "456 Market Street",
            city = "New York",
            totalPurchases = 159.98,
            outstandingBalance = 0.0
        )
        customerDao.insertCustomer(sampleCustomer)

        // 2. Export Database to JSON String
        val exportResult = backupManager.generateBackupJson()
        assertTrue("Export result should be success", exportResult.isSuccess)
        val exportJson = exportResult.getOrThrow()
        assertTrue("Export JSON should contain backupVersion", exportJson.contains("backupVersion"))
        assertTrue("Export JSON should contain apex company name", exportJson.contains("Apex Warehouses"))
        assertTrue("Export JSON should contain customer", exportJson.contains("Johnathan Doe"))

        // 3. Clear database entities
        customerDao.deleteCustomer(sampleCustomer)
        profileManager.deleteProfile()

        // 4. Import the JSON back into Database
        val importResult = backupManager.restoreBackupFromJsonString(exportJson)
        assertTrue("Import should succeed without errors", importResult.isSuccess)
        val stats = importResult.getOrThrow()
        assertTrue("Restored customers count should be at least 1", stats.customersCount >= 1)

        // 5. Verify restored items in Database
        val restoredCustomer = customerDao.getAllCustomersList().find { it.name == "Johnathan Doe" }
        org.junit.Assert.assertNotNull("Customer should be restored", restoredCustomer)
        assertEquals("Johnathan Doe", restoredCustomer?.name)

        val restoredProfile = profileManager.profile.value
        assertEquals("Apex Warehouses", restoredProfile.companyName)
    }

    @Test
    fun `test database exporter comprehensive mapping across all tables`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getInstance(context)
        val profileManager = com.example.data.repository.BusinessProfileManager.getInstance(context)
        val themeManager = com.example.ui.theme.ThemePreferenceManager.getInstance(context)

        val expense = com.example.data.model.ExpenseEntity(
            id = 401L,
            category = "Electricity",
            description = "Warehouse Power Bill",
            amount = 320.50,
            paymentMethod = "Bank Transfer"
        )
        db.expenseDao().insertExpense(expense)

        val employee = com.example.data.model.EmployeeEntity(
            id = 501L,
            name = "Sarah Jenkins",
            phone = "555-8811",
            email = "sarah.j@company.com",
            address = "12 Maple Ave",
            position = "Inventory Supervisor",
            joiningDate = "2024-01-15",
            baseSalary = 4500.0
        )
        db.employeeDao().insertEmployee(employee)

        val salary = com.example.data.model.SalaryPaymentEntity(
            id = 601L,
            employeeId = 501L,
            employeeName = "Sarah Jenkins",
            monthYear = "August 2026",
            baseSalary = 4500.0,
            bonus = 250.0,
            netSalary = 4750.0
        )
        db.salaryDao().insertSalary(salary)

        val sale = com.example.data.model.SaleOrderEntity(
            id = 701L,
            invoiceNumber = "INV-2026-0099",
            customerId = 201L,
            customerName = "Johnathan Doe",
            itemsJson = "[{\"productId\":101,\"productName\":\"Wireless Barcode Scanner\",\"sku\":\"SKU-SCAN-01\",\"unitPrice\":79.99,\"quantity\":2,\"subtotal\":159.98}]",
            subtotal = 159.98,
            grandTotal = 159.98,
            paidAmount = 159.98,
            remainingBalance = 0.0,
            paymentMethod = "Cash",
            paymentStatus = "Paid"
        )
        db.saleDao().insertSale(sale)

        // Export via DatabaseExporter
        val jsonString = com.example.data.repository.DatabaseExporter.exportToJsonString(db, profileManager, themeManager)
        assertTrue("JSON must contain expenses", jsonString.contains("Warehouse Power Bill"))
        assertTrue("JSON must contain employees", jsonString.contains("Sarah Jenkins"))
        assertTrue("JSON must contain invoices", jsonString.contains("INV-2026-0099"))

        // Validate
        val validation = com.example.data.repository.DatabaseExporter.validateBackupJson(jsonString)
        assertTrue("Validation should be valid", validation.isValid)
        assertTrue("Validation expenses count >= 1", validation.expensesCount >= 1)
        assertTrue("Validation employees count >= 1", validation.employeesCount >= 1)
        assertTrue("Validation sales count >= 1", validation.salesCount >= 1)

        // Atomic restore with clear existing
        val restoreResult = com.example.data.repository.DatabaseExporter.importFromJsonString(
            jsonString = jsonString,
            db = db,
            profileManager = profileManager,
            themeManager = themeManager,
            clearExistingBeforeRestore = true
        )
        assertTrue("Atomic restore should succeed", restoreResult.isSuccess)
    }

    @Test
    fun `test email duplicate registration prevention`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = AuthRepository.getInstance(context)

        // Register first time
        val regResult1 = authRepo.register(
            firstName = "Duplicate",
            lastName = "Test",
            email = "duplicate@test.com",
            password = "Password123!",
            securityQuestions = testQuestions
        )
        assertTrue("First registration should succeed", regResult1.isSuccess)

        // Register second time with exact same email
        val regResult2 = authRepo.register(
            firstName = "Duplicate2",
            lastName = "Test2",
            email = "duplicate@test.com",
            password = "Password123!",
            securityQuestions = testQuestions
        )
        assertFalse("Second registration with same email should fail", regResult2.isSuccess)
        assertEquals("This email is already registered. Please log in instead.", regResult2.exceptionOrNull()?.message)

        // Register second time with different casing and spaces
        val regResult3 = authRepo.register(
            firstName = "Duplicate3",
            lastName = "Test3",
            email = "  DuPliCaTe@TeSt.CoM  ",
            password = "Password123!",
            securityQuestions = testQuestions
        )
        assertFalse("Second registration with same email (case/space variations) should fail", regResult3.isSuccess)
        assertEquals("This email is already registered. Please log in instead.", regResult3.exceptionOrNull()?.message)
        
        // Ensure isEmailRegistered method works
        assertTrue(authRepo.isEmailRegistered("duplicate@test.com"))
        assertTrue(authRepo.isEmailRegistered("  DuPliCaTe@TeSt.CoM  "))
        assertFalse(authRepo.isEmailRegistered("new@test.com"))
    }
}
