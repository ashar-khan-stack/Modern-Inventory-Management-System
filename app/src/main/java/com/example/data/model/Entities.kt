package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val openingBalance: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val status: String = "Active"
)

data class SaleOrderItem(
    val description: String = "",
    val productName: String = description,
    val productId: Long = 0L,
    val sku: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val discountPercent: Double = 0.0,
    val subtotal: Double = 0.0,
    val imageUrl: String = ""
)

@Entity(tableName = "sales")
data class SaleOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val taxInvoiceNumber: String = "",
    val taxId: String = "",
    val customerId: Long,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val itemsJson: String, // List of SaleOrderItem as JSON
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val taxRatePercent: Double = 0.0,
    val grandTotal: Double,
    val paidAmount: Double,
    val remainingBalance: Double,
    val paymentMethod: String, // Cash, Bank Transfer, Card, Other
    val paymentStatus: String, // Paid, Partial, Unpaid
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // Rent, Electricity, Internet, Salary, Transportation, Maintenance, Marketing, Packaging, Other
    val description: String,
    val amount: Double,
    val paidAmount: Double = amount,
    val remainingBalance: Double = (amount - paidAmount).coerceAtLeast(0.0),
    val paymentStatus: String = "Paid", // Paid, Unpaid, Partially Paid
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, Card
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val position: String,
    val joiningDate: String,
    val baseSalary: Double,
    val status: String = "Active" // Active, On Leave, Inactive
)

@Entity(tableName = "salaries")
data class SalaryPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val monthYear: String, // e.g. "August 2026"
    val baseSalary: Double,
    val bonus: Double = 0.0,
    val deductions: Double = 0.0,
    val netSalary: Double,
    val paymentStatus: String = "Paid", // Paid, Unpaid, Pending
    val paymentMethod: String = "Bank Transfer",
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val isFingerprintEnabled: Boolean = false,
    val securityQuestionsJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class SecurityQuestionItem(
    val questionText: String,
    val answerHash: String
)

object SecurityQuestionParser {
    fun toJson(items: List<SecurityQuestionItem>): String {
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("questionText", item.questionText)
                put("answerHash", item.answerHash)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToList(json: String): List<SecurityQuestionItem> {
        val list = mutableListOf<SecurityQuestionItem>()
        if (json.isBlank()) return list
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SecurityQuestionItem(
                        questionText = obj.optString("questionText", ""),
                        answerHash = obj.optString("answerHash", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
