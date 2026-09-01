package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [
        CustomerEntity::class,
        SaleOrderEntity::class,
        ExpenseEntity::class,
        EmployeeEntity::class,
        SalaryPaymentEntity::class,
        UserEntity::class,
        BankAccountEntity::class,
        BankTransactionEntity::class,
        VoucherEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun salaryDao(): SalaryDao
    abstract fun userDao(): UserDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun bankTransactionDao(): BankTransactionDao
    abstract fun voucherDao(): VoucherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        var appContext: Context? = null
            private set

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createBankAndVoucherTables(db)
            }
        }

        private fun createBankAndVoucherTables(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `bank_accounts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bankName` TEXT NOT NULL,
                    `accountTitle` TEXT NOT NULL,
                    `accountNumber` TEXT NOT NULL,
                    `iban` TEXT NOT NULL,
                    `branchName` TEXT NOT NULL,
                    `openingBalance` REAL NOT NULL,
                    `currentBalance` REAL NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `bank_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bankAccountId` INTEGER NOT NULL,
                    `transactionType` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `debit` REAL NOT NULL,
                    `credit` REAL NOT NULL,
                    `description` TEXT NOT NULL,
                    `referenceVoucher` TEXT NOT NULL,
                    `targetAccountId` INTEGER,
                    `transactionDate` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vouchers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `voucherNumber` TEXT NOT NULL,
                    `voucherType` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `accountName` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `debit` REAL NOT NULL,
                    `credit` REAL NOT NULL,
                    `amount` REAL NOT NULL,
                    `referenceNotes` TEXT NOT NULL,
                    `bankAccountId` INTEGER,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }

        private fun ensureAllSchemaUpToDate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `customers` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `address` TEXT NOT NULL,
                    `city` TEXT NOT NULL,
                    `openingBalance` REAL NOT NULL,
                    `totalPurchases` REAL NOT NULL,
                    `totalPaid` REAL NOT NULL,
                    `outstandingBalance` REAL NOT NULL,
                    `status` TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `sales` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `invoiceNumber` TEXT NOT NULL,
                    `taxInvoiceNumber` TEXT NOT NULL,
                    `taxId` TEXT NOT NULL,
                    `customerId` INTEGER NOT NULL,
                    `customerName` TEXT NOT NULL,
                    `customerPhone` TEXT NOT NULL,
                    `customerAddress` TEXT NOT NULL,
                    `itemsJson` TEXT NOT NULL,
                    `subtotal` REAL NOT NULL,
                    `discountAmount` REAL NOT NULL,
                    `taxAmount` REAL NOT NULL,
                    `taxRatePercent` REAL NOT NULL,
                    `grandTotal` REAL NOT NULL,
                    `paidAmount` REAL NOT NULL,
                    `remainingBalance` REAL NOT NULL,
                    `paymentMethod` TEXT NOT NULL,
                    `paymentStatus` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `category` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `paidAmount` REAL NOT NULL,
                    `remainingBalance` REAL NOT NULL,
                    `paymentStatus` TEXT NOT NULL,
                    `paymentMethod` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `date` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `employees` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `address` TEXT NOT NULL,
                    `position` TEXT NOT NULL,
                    `joiningDate` TEXT NOT NULL,
                    `baseSalary` REAL NOT NULL,
                    `status` TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `salaries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `employeeId` INTEGER NOT NULL,
                    `employeeName` TEXT NOT NULL,
                    `monthYear` TEXT NOT NULL,
                    `baseSalary` REAL NOT NULL,
                    `bonus` REAL NOT NULL,
                    `deductions` REAL NOT NULL,
                    `netSalary` REAL NOT NULL,
                    `paymentStatus` TEXT NOT NULL,
                    `paymentMethod` TEXT NOT NULL,
                    `paymentDate` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `firstName` TEXT NOT NULL,
                    `lastName` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `passwordHash` TEXT NOT NULL,
                    `salt` TEXT NOT NULL,
                    `isFingerprintEnabled` INTEGER NOT NULL,
                    `securityQuestionsJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

            createBankAndVoucherTables(db)
        }

        private val ALL_MIGRATIONS = arrayOf(
            object : Migration(1, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(2, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(3, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(4, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(5, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(6, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(7, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(8, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(9, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(10, 12) { override fun migrate(db: SupportSQLiteDatabase) { ensureAllSchemaUpToDate(db) } },
            object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) { createBankAndVoucherTables(db) } }
        )

        fun getInstance(context: Context): AppDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_master.db"
                )
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object OrderJsonParser {
    fun saleItemsToJson(items: List<SaleOrderItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                val desc = item.description.ifBlank { item.productName }
                put("description", desc)
                put("productName", item.productName.ifBlank { desc })
                put("sku", item.sku)
                put("unitPrice", item.unitPrice)
                put("quantity", item.quantity)
                put("discountPercent", item.discountPercent)
                put("subtotal", item.subtotal)
                put("imageUrl", item.imageUrl)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToSaleItems(json: String): List<SaleOrderItem> {
        val list = mutableListOf<SaleOrderItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val desc = obj.optString("description", obj.optString("productName", ""))
                val name = obj.optString("productName", desc)
                list.add(
                    SaleOrderItem(
                        description = desc,
                        productName = name,
                        sku = obj.optString("sku"),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        quantity = obj.optInt("quantity", 1),
                        discountPercent = obj.optDouble("discountPercent", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
