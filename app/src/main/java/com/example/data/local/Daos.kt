package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<SaleOrderEntity>>

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    suspend fun getAllSalesList(): List<SaleOrderEntity>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleOrderEntity?

    @Query("SELECT * FROM sales WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getSaleByInvoice(invoiceNumber: String): SaleOrderEntity?

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleOrderEntity>)

    @Update
    suspend fun updateSale(sale: SaleOrderEntity)

    @Delete
    suspend fun deleteSale(sale: SaleOrderEntity)

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAllEmployeesList(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees")
    suspend fun deleteAllEmployees()
}

@Dao
interface SalaryDao {
    @Query("SELECT * FROM salaries ORDER BY paymentDate DESC")
    fun getAllSalaries(): Flow<List<SalaryPaymentEntity>>

    @Query("SELECT * FROM salaries ORDER BY paymentDate DESC")
    suspend fun getAllSalariesList(): List<SalaryPaymentEntity>

    @Query("SELECT * FROM salaries WHERE employeeId = :employeeId ORDER BY paymentDate DESC")
    fun getSalariesByEmployee(employeeId: Long): Flow<List<SalaryPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalary(salary: SalaryPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaries(salaries: List<SalaryPaymentEntity>)

    @Update
    suspend fun updateSalary(salary: SalaryPaymentEntity)

    @Delete
    suspend fun deleteSalary(salary: SalaryPaymentEntity)

    @Query("DELETE FROM salaries")
    suspend fun deleteAllSalaries()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersList(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePassword(email: String, passwordHash: String, salt: String)

    @Query("UPDATE users SET isFingerprintEnabled = :enabled WHERE id = :userId")
    suspend fun setFingerprintEnabled(userId: Long, enabled: Boolean)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
