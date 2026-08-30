package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        UserEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun salaryDao(): SalaryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        var appContext: Context? = null
            private set

        fun getInstance(context: Context): AppDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_master.db"
                ).fallbackToDestructiveMigration().build()
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
