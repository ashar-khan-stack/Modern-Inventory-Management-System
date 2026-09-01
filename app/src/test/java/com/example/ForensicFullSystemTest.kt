package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.DatabaseExporter
import com.example.data.repository.InventoryRepository
import com.example.ui.theme.ThemePreferenceManager
import com.example.data.repository.BusinessProfileManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForensicFullSystemTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: InventoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = InventoryRepository(db)
    }

    @Test
    fun testFreshDatabaseInstallationAndAllTablesExist() = runBlocking {
        // Verify all DAOs can insert and query
        val custId = repository.addCustomer(
            CustomerEntity(
                name = "Test Customer",
                phone = "03001234567",
                email = "test@customer.com",
                address = "Main Street",
                city = "Lahore",
                openingBalance = 1000.0
            )
        )
        assertTrue("Customer inserted", custId > 0)

        val bankId = repository.addBankAccount(
            BankAccountEntity(
                bankName = "Meezan Bank",
                accountTitle = "Business Account",
                accountNumber = "123456789",
                openingBalance = 50000.0,
                currentBalance = 50000.0
            )
        )
        assertTrue("Bank account inserted", bankId > 0)

        val vchId = repository.recordVoucher(
            VoucherEntity(
                voucherNumber = "VCH-001",
                voucherType = "Receipt",
                accountName = "Cash",
                description = "Initial capital",
                debit = 0.0,
                credit = 50000.0,
                amount = 50000.0
            )
        )
        assertTrue("Voucher inserted", vchId > 0)

        val expId = repository.addExpense(
            ExpenseEntity(
                category = "Electricity",
                description = "Office Bill",
                amount = 2500.0
            )
        )
        assertTrue("Expense inserted", expId > 0)

        val empId = repository.addEmployee(
            EmployeeEntity(
                name = "Ahmad Raza",
                phone = "03111234567",
                email = "ahmad@company.com",
                address = "Model Town",
                position = "Accountant",
                joiningDate = "2024-01-01",
                baseSalary = 60000.0
            )
        )
        assertTrue("Employee inserted", empId > 0)

        val salId = repository.recordSalaryPayment(
            SalaryPaymentEntity(
                employeeId = empId,
                employeeName = "Ahmad Raza",
                monthYear = "August 2026",
                baseSalary = 60000.0,
                netSalary = 60000.0,
                paymentStatus = "Paid"
            )
        )
        assertTrue("Salary inserted", salId > 0)
    }

    @Test
    fun testBankModuleFinancialMathAndAutoVouchers() = runBlocking {
        // Create 2 Bank Accounts
        val acc1Id = repository.addBankAccount(
            BankAccountEntity(
                bankName = "HBL",
                accountTitle = "Primary Operations",
                accountNumber = "11112222",
                openingBalance = 100000.0,
                currentBalance = 100000.0
            )
        )

        val acc2Id = repository.addBankAccount(
            BankAccountEntity(
                bankName = "UBL",
                accountTitle = "Reserve Savings",
                accountNumber = "33334444",
                openingBalance = 20000.0,
                currentBalance = 20000.0
            )
        )

        // 1. Deposit Rs. 30,000 in Acc 1
        repository.recordBankTransaction(
            bankAccountId = acc1Id,
            type = "Deposit",
            amount = 30000.0,
            description = "Owner Capital Deposit"
        )

        // 2. Withdrawal Rs. 10,000 from Acc 1
        repository.recordBankTransaction(
            bankAccountId = acc1Id,
            type = "Withdrawal",
            amount = 10000.0,
            description = "Cash withdrawal for petty cash"
        )

        // 3. Transfer Rs. 25,000 from Acc 1 to Acc 2
        repository.recordBankTransaction(
            bankAccountId = acc1Id,
            type = "Transfer",
            amount = 25000.0,
            description = "Inter-bank funds transfer",
            targetAccountId = acc2Id
        )

        val acc1 = db.bankAccountDao().getBankAccountById(acc1Id)!!
        val acc2 = db.bankAccountDao().getBankAccountById(acc2Id)!!

        // Expected Acc 1: 100,000 + 30,000 - 10,000 - 25,000 = 95,000
        assertEquals(95000.0, acc1.currentBalance, 0.001)

        // Expected Acc 2: 20,000 + 25,000 = 45,000
        assertEquals(45000.0, acc2.currentBalance, 0.001)

        // Verify Auto-Vouchers created
        val vouchers = db.voucherDao().getAllVouchersList()
        assertTrue("Vouchers auto generated", vouchers.size >= 3)
    }

    @Test
    fun testSalesPOSCustomerLedgerAndSettlement() = runBlocking {
        val cust = CustomerEntity(
            name = "Babar Azam",
            phone = "03211234567",
            email = "babar@cricket.pk",
            address = "Lahore",
            city = "Lahore",
            openingBalance = 5000.0
        )
        val custId = repository.addCustomer(cust)
        val freshCust = db.customerDao().getCustomerById(custId)!!

        val items = listOf(
            SaleOrderItem(productName = "Cricket Bat", sku = "BAT-01", unitPrice = 15000.0, quantity = 2, subtotal = 30000.0)
        )

        // Process sale of Rs 30,000 with Rs 20,000 paid and Rs 10,000 remaining
        val saleResult = repository.processSale(
            customer = freshCust,
            items = items,
            subtotal = 30000.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            taxRatePercent = 0.0,
            grandTotal = 30000.0,
            paidAmount = 20000.0,
            paymentMethod = "Cash",
            notes = "Test POS Sale"
        )

        assertTrue(saleResult.isSuccess)
        val sale = saleResult.getOrThrow()
        assertEquals(10000.0, sale.remainingBalance, 0.001)

        // Customer ledger check:
        // totalPurchases = 30000, totalPaid = 20000, outstandingBalance = 10000 + 5000 (opening) = 15000
        val updatedCust1 = db.customerDao().getCustomerById(custId)!!
        assertEquals(30000.0, updatedCust1.totalPurchases, 0.001)
        assertEquals(20000.0, updatedCust1.totalPaid, 0.001)
        assertEquals(15000.0, updatedCust1.outstandingBalance, 0.001)

        // Partial payment on sale order of Rs 5,000
        val payResult = repository.updateSalePayment(sale, 5000.0)
        assertTrue(payResult.isSuccess)

        val updatedCust2 = db.customerDao().getCustomerById(custId)!!
        assertEquals(25000.0, updatedCust2.totalPaid, 0.001)
        assertEquals(10000.0, updatedCust2.outstandingBalance, 0.001)

        // Settle remaining Rs 10,000
        val settleOk = repository.settleCustomerPayment(custId, 10000.0)
        assertTrue(settleOk)

        val updatedCust3 = db.customerDao().getCustomerById(custId)!!
        assertEquals(0.0, updatedCust3.outstandingBalance, 0.001)
    }

    @Test
    fun testAtomicBackupExportAndRestore() = runBlocking {
        // Seed database
        val custId = repository.addCustomer(CustomerEntity(name = "Original Customer", phone = "1234", email = "orig@test.com", address = "A", city = "B"))
        val bankId = repository.addBankAccount(BankAccountEntity(bankName = "Original Bank", accountTitle = "Title", accountNumber = "000"))
        val expId = repository.addExpense(ExpenseEntity(category = "Rent", description = "Shop Rent", amount = 15000.0))

        val profileManager = BusinessProfileManager.getInstance(context)
        val themeManager = ThemePreferenceManager.getInstance(context)

        // Export JSON
        val jsonBackup = DatabaseExporter.exportToJsonString(db, profileManager, themeManager)
        assertNotNull(jsonBackup)
        assertTrue(jsonBackup.contains("Original Customer"))
        assertTrue(jsonBackup.contains("Original Bank"))
        assertTrue(jsonBackup.contains("Shop Rent"))

        // Validate JSON
        val valResult = DatabaseExporter.validateBackupJson(jsonBackup)
        assertTrue(valResult.isValid)
        assertEquals(1, valResult.customersCount)
        assertEquals(1, valResult.expensesCount)

        // Restore into clean DB
        val cleanDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val restoreResult = DatabaseExporter.importFromJsonString(
            jsonString = jsonBackup,
            db = cleanDb,
            profileManager = profileManager,
            themeManager = themeManager,
            clearExistingBeforeRestore = true
        )

        assertTrue(restoreResult.isSuccess)

        val restoredCustomers = cleanDb.customerDao().getAllCustomersList()
        val restoredBanks = cleanDb.bankAccountDao().getAllBankAccountsList()
        val restoredExpenses = cleanDb.expenseDao().getAllExpensesList()

        assertEquals(1, restoredCustomers.size)
        assertEquals("Original Customer", restoredCustomers[0].name)
        assertEquals(1, restoredBanks.size)
        assertEquals("Original Bank", restoredBanks[0].bankName)
        assertEquals(1, restoredExpenses.size)
        assertEquals("Shop Rent", restoredExpenses[0].description)
    }

    @Test
    fun testSecurityAndSaltedPasswordHashing() = runBlocking {
        val authRepo = AuthRepository(context, db)

        val questions = listOf(
            Pair("What was the name of your first school?", "School A"),
            Pair("What was your childhood nickname?", "Nick B"),
            Pair("What was the name of your favorite childhood teacher?", "Teacher C"),
            Pair("What was the name of your first pet?", "Pet D"),
            Pair("What was your favorite childhood game?", "Game E")
        )

        val regRes = authRepo.register(
            firstName = "Security",
            lastName = "Tester",
            email = "security.tester@test.com",
            password = "StrongPassword123!",
            securityQuestions = questions
        )
        assertTrue(regRes.isSuccess)

        // Verify password login
        val loginRes = authRepo.login("security.tester@test.com", "StrongPassword123!")
        assertTrue(loginRes.isSuccess)

        // Verify wrong password fails
        val wrongLogin = authRepo.login("security.tester@test.com", "WrongPassword123!")
        assertTrue(wrongLogin.isFailure)

        // Verify user entity has non-empty salt and non-empty passwordHash, and NO plaintext password
        val user = db.userDao().getUserByEmail("security.tester@test.com")!!
        assertTrue(user.salt.isNotBlank())
        assertTrue(user.passwordHash.isNotBlank())
        assertNotEquals("StrongPassword123!", user.passwordHash)
    }
}
