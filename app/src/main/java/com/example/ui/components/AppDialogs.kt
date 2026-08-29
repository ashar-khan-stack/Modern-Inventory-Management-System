package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.util.ValidationUtils

// Helper currency formatter for AppDialogs
private fun formatCurrency(amount: Double): String {
    return String.format("Rs. %,.2f", amount)
}

/**
 * 4-Step Multi-Step Customer Form Dialog
 */
@Composable
fun CustomerFormDialog(
    initialCustomer: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val stepTitles = listOf("Basic Info", "Contact & Location", "Financial Account", "Review & Confirm")

    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phoneDigits by remember { mutableStateOf(ValidationUtils.sanitizePkPhoneDigits(initialCustomer?.phone ?: "")) }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var city by remember { mutableStateOf(initialCustomer?.city ?: "") }
    var openingBalance by remember { mutableStateOf(if (initialCustomer != null && initialCustomer.openingBalance != 0.0) initialCustomer.openingBalance.toString() else "") }
    var error by remember { mutableStateOf<String?>(null) }

    val (isNameValid, nameError) = remember(name) { ValidationUtils.validateRequired(name, "Customer Name") }
    val (isPhoneValid, phoneError) = remember(phoneDigits) { ValidationUtils.validatePkPhone(phoneDigits, isRequired = false) }
    val (isEmailValid, emailError) = remember(email) { ValidationUtils.validateEmail(email, isRequired = false) }

    val isStep1Valid = isNameValid && isPhoneValid
    val isStep2Valid = isEmailValid

    MultiStepFormDialog(
        title = if (initialCustomer == null) "Add Customer" else "Edit Customer",
        currentStep = step,
        totalSteps = 4,
        stepTitles = stepTitles,
        onDismissRequest = onDismiss,
        onBack = { if (step > 1) step-- },
        onNext = {
            if (step == 1 && !isStep1Valid) {
                error = nameError ?: phoneError ?: "Please enter valid basic details."
            } else if (step == 2 && !isStep2Valid) {
                error = emailError ?: "Please enter valid contact details."
            } else {
                error = null
                if (step < 4) step++
            }
        },
        onSave = {
            val canonicalPhone = ValidationUtils.toCanonicalPkPhone(phoneDigits)
            val ob = openingBalance.toDoubleOrNull() ?: 0.0
            val customer = CustomerEntity(
                id = initialCustomer?.id ?: 0L,
                name = name.trim(),
                phone = canonicalPhone,
                email = email.trim(),
                address = address.trim(),
                city = city.trim(),
                openingBalance = ob,
                totalPurchases = initialCustomer?.totalPurchases ?: 0.0,
                totalPaid = initialCustomer?.totalPaid ?: 0.0,
                outstandingBalance = initialCustomer?.outstandingBalance ?: ob,
                status = initialCustomer?.status ?: "Active"
            )
            onSave(customer)
            onDismiss()
        },
        isNextEnabled = when (step) {
            1 -> isStep1Valid
            2 -> isStep2Valid
            else -> true
        },
        saveButtonText = "Save Customer"
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (currentStep) {
                1 -> {
                    Text("Step 1: Primary Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { newValue ->
                            name = newValue.filter { it.isLetter() || it.isWhitespace() || it == '\'' || it == '-' || it == '.' }
                            error = null
                        },
                        label = { Text("Customer Full Name *") },
                        singleLine = true,
                        isError = !isNameValid && name.isNotBlank(),
                        supportingText = { if (!isNameValid && name.isNotBlank()) Text(nameError ?: "Required", color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = phoneDigits,
                        onValueChange = { newValue ->
                            phoneDigits = ValidationUtils.sanitizePkPhoneDigits(newValue)
                            error = null
                        },
                        label = { Text("Phone Number (Pakistan Mobile)") },
                        placeholder = { Text("300 1234567") },
                        prefix = { Text("+92 ", fontWeight = FontWeight.Bold, color = BrandBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = !isPhoneValid && phoneDigits.isNotBlank(),
                        supportingText = {
                            if (!isPhoneValid && phoneDigits.isNotBlank()) Text(phoneError ?: "Invalid number", color = MaterialTheme.colorScheme.error)
                            else Text("Format: +92 3XX XXXXXXX", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
                    )
                }

                2 -> {
                    Text("Step 2: Contact & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); error = null },
                        label = { Text("Email Address") },
                        placeholder = { Text("customer@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = !isEmailValid && email.isNotBlank(),
                        supportingText = { if (!isEmailValid && email.isNotBlank()) Text(emailError ?: "Invalid email", color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("customer_email_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; error = null },
                        label = { Text("Street Address") },
                        placeholder = { Text("House 25, Block A, Street 5") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it; error = null },
                        label = { Text("City / Region") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    Text("Step 3: Opening Account Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = openingBalance,
                        onValueChange = { newValue ->
                            openingBalance = newValue.filter { it.isDigit() || it == '.' }
                            error = null
                        },
                        label = { Text("Opening Balance (Rs.)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Leave as 0.00 if customer has no initial balance or debt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                4 -> {
                    Text("Step 4: Review Customer Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReviewRow("Customer Name", name)
                            ReviewRow("Phone Number", if (phoneDigits.isBlank()) "None" else "+92 $phoneDigits")
                            ReviewRow("Email Address", email.ifBlank { "None" })
                            ReviewRow("City", city.ifBlank { "None" })
                            ReviewRow("Address", address.ifBlank { "None" })
                            ReviewRow("Opening Balance", formatCurrency(openingBalance.toDoubleOrNull() ?: 0.0))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-Step Multi-Step Expense Form Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    initialExpense: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val stepTitles = listOf("Category & Description", "Financial Details", "Review & Confirm")

    val categories = listOf("Rent", "Electricity", "Internet", "Transportation", "Maintenance", "Marketing", "Packaging", "Office Supplies", "Other")
    var category by remember { mutableStateOf(initialExpense?.category?.takeIf { it.isNotEmpty() } ?: categories.first()) }
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod?.takeIf { it.isNotEmpty() } ?: "Cash") }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var categoryDropdownOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isStep1Valid = description.isNotBlank() && category.isNotBlank()
    val isStep2Valid = amount > 0.0

    MultiStepFormDialog(
        title = if (initialExpense == null) "Record Expense" else "Edit Expense",
        currentStep = step,
        totalSteps = 3,
        stepTitles = stepTitles,
        onDismissRequest = onDismiss,
        onBack = { if (step > 1) step-- },
        onNext = {
            if (step == 1 && !isStep1Valid) {
                error = "Description is required."
            } else if (step == 2 && !isStep2Valid) {
                error = "Amount must be greater than 0."
            } else {
                error = null
                if (step < 3) step++
            }
        },
        onSave = {
            val expense = ExpenseEntity(
                id = initialExpense?.id ?: 0L,
                category = category,
                description = description.trim(),
                amount = amount,
                paymentMethod = paymentMethod.trim().ifBlank { "Cash" },
                notes = notes.trim(),
                date = initialExpense?.date ?: System.currentTimeMillis()
            )
            onSave(expense)
            onDismiss()
        },
        isNextEnabled = when (step) {
            1 -> isStep1Valid
            2 -> isStep2Valid
            else -> true
        },
        saveButtonText = "Save Expense"
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            when (currentStep) {
                1 -> {
                    Text("Step 1: Category & Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownOpen,
                        onExpandedChange = { categoryDropdownOpen = !categoryDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownOpen) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownOpen,
                            onDismissRequest = { categoryDropdownOpen = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; error = null },
                        label = { Text("Expense Description *") },
                        placeholder = { Text("e.g. Shop Utility Bill for August") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text("Step 2: Financial Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; error = null },
                        label = { Text("Amount (Rs.) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        placeholder = { Text("Cash, Bank Transfer, Card") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Receipt Reference") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    Text("Step 3: Review Expense Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReviewRow("Category", category)
                            ReviewRow("Description", description)
                            ReviewRow("Total Amount", formatCurrency(amount))
                            ReviewRow("Payment Method", paymentMethod)
                            ReviewRow("Notes", notes.ifBlank { "None" })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Expense Payment Status Update Popup (Part 12, 13, 14, 15)
 */
@Composable
fun ExpensePaymentStatusDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onSaveStatus: (ExpenseEntity) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(expense.paymentStatus.ifBlank { "Paid" }) }
    var paidAmountText by remember {
        mutableStateOf(
            when (expense.paymentStatus) {
                "Paid" -> expense.amount.toString()
                "Unpaid" -> "0"
                else -> if (expense.paidAmount > 0) expense.paidAmount.toString() else ""
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    val totalAmount = expense.amount

    val calculatedPaid = when (selectedStatus) {
        "Paid" -> totalAmount
        "Unpaid" -> 0.0
        else -> paidAmountText.toDoubleOrNull() ?: 0.0
    }
    val calculatedBalance = (totalAmount - calculatedPaid).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Expense Payment Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expense Total:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(formatCurrency(totalAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Already Paid:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(formatCurrency(calculatedPaid), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Balance:", style = MaterialTheme.typography.bodyMedium, color = AppTextSecondary)
                            Text(formatCurrency(calculatedBalance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DangerRed)
                        }
                    }
                }

                Text("Select Payment Status Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == "Paid",
                        onClick = {
                            selectedStatus = "Paid"
                            paidAmountText = totalAmount.toString()
                            error = null
                        },
                        label = { Text("Paid") }
                    )
                    FilterChip(
                        selected = selectedStatus == "Partially Paid" || selectedStatus == "Partial",
                        onClick = {
                            selectedStatus = "Partially Paid"
                            error = null
                        },
                        label = { Text("Partially Paid") }
                    )
                    FilterChip(
                        selected = selectedStatus == "Unpaid",
                        onClick = {
                            selectedStatus = "Unpaid"
                            paidAmountText = "0"
                            error = null
                        },
                        label = { Text("Unpaid") }
                    )
                }

                if (selectedStatus == "Partially Paid" || selectedStatus == "Partial") {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { newValue ->
                            paidAmountText = newValue.filter { it.isDigit() || it == '.' }
                            error = null
                        },
                        label = { Text("Enter Amount Paid (Rs.) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = error != null,
                        supportingText = { if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("expense_payment_status_amount_input")
                    )
                }

                if (error != null && selectedStatus != "Partially Paid" && selectedStatus != "Partial") {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pAmount = when (selectedStatus) {
                        "Paid" -> totalAmount
                        "Unpaid" -> 0.0
                        else -> paidAmountText.toDoubleOrNull() ?: -1.0
                    }

                    if (pAmount < 0) {
                        error = "Please enter a valid numeric payment amount."
                        return@Button
                    }

                    if (pAmount > totalAmount + 0.01) {
                        error = "Payment cannot be greater than the remaining balance."
                        return@Button
                    }

                    val finalStatus = when {
                        pAmount >= totalAmount -> "Paid"
                        pAmount > 0 -> "Partially Paid"
                        else -> "Unpaid"
                    }

                    val updated = expense.copy(
                        paidAmount = pAmount,
                        remainingBalance = (totalAmount - pAmount).coerceAtLeast(0.0),
                        paymentStatus = finalStatus
                    )
                    onSaveStatus(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * 4-Step Multi-Step Employee Form Dialog
 */
@Composable
fun EmployeeFormDialog(
    initialEmployee: EmployeeEntity? = null,
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val stepTitles = listOf("Personal Info", "Contact Details", "Compensation & Status", "Review & Confirm")

    var name by remember { mutableStateOf(initialEmployee?.name ?: "") }
    var phone by remember { mutableStateOf(initialEmployee?.phone ?: "") }
    var email by remember { mutableStateOf(initialEmployee?.email ?: "") }
    var position by remember { mutableStateOf(initialEmployee?.position ?: "") }
    var salaryText by remember { mutableStateOf(if (initialEmployee != null && initialEmployee.baseSalary > 0) initialEmployee.baseSalary.toString() else "") }
    var address by remember { mutableStateOf(initialEmployee?.address ?: "") }
    var status by remember { mutableStateOf(initialEmployee?.status ?: "Active") }
    var error by remember { mutableStateOf<String?>(null) }

    val isStep1Valid = name.isNotBlank() && position.isNotBlank()
    val isStep3Valid = (salaryText.toDoubleOrNull() ?: 0.0) >= 0.0

    MultiStepFormDialog(
        title = if (initialEmployee == null) "Add Employee" else "Edit Employee",
        currentStep = step,
        totalSteps = 4,
        stepTitles = stepTitles,
        onDismissRequest = onDismiss,
        onBack = { if (step > 1) step-- },
        onNext = {
            if (step == 1 && !isStep1Valid) {
                error = "Full Name and Job Position are required."
            } else {
                error = null
                if (step < 4) step++
            }
        },
        onSave = {
            val sal = salaryText.toDoubleOrNull() ?: 0.0
            val employee = EmployeeEntity(
                id = initialEmployee?.id ?: 0L,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                position = position.trim(),
                joiningDate = initialEmployee?.joiningDate ?: "Aug 2026",
                baseSalary = sal,
                status = status
            )
            onSave(employee)
            onDismiss()
        },
        isNextEnabled = when (step) {
            1 -> isStep1Valid
            3 -> isStep3Valid
            else -> true
        },
        saveButtonText = "Save Employee"
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            when (currentStep) {
                1 -> {
                    Text("Step 1: Personal Info & Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it; error = null },
                        label = { Text("Job Position / Designation *") },
                        placeholder = { Text("e.g. Manager, Sales Staff") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text("Step 2: Contact Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Home Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    Text("Step 3: Compensation & Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it; error = null },
                        label = { Text("Base Monthly Salary (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Employment Status", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "Active",
                            onClick = { status = "Active" },
                            label = { Text("Active") }
                        )
                        FilterChip(
                            selected = status == "On Leave",
                            onClick = { status = "On Leave" },
                            label = { Text("On Leave") }
                        )
                        FilterChip(
                            selected = status == "Inactive",
                            onClick = { status = "Inactive" },
                            label = { Text("Inactive") }
                        )
                    }
                }

                4 -> {
                    Text("Step 4: Review Employee Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReviewRow("Full Name", name)
                            ReviewRow("Job Position", position)
                            ReviewRow("Phone Number", phone.ifBlank { "None" })
                            ReviewRow("Email Address", email.ifBlank { "None" })
                            ReviewRow("Home Address", address.ifBlank { "None" })
                            ReviewRow("Base Salary", formatCurrency(salaryText.toDoubleOrNull() ?: 0.0))
                            ReviewRow("Status", status)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4-Step Multi-Step Process Salary Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessSalaryDialog(
    employees: List<EmployeeEntity>,
    initialSalary: SalaryPaymentEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SalaryPaymentEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val stepTitles = listOf("Employee & Period", "Salary Breakdown", "Payment Details", "Review & Disburse")

    var selectedEmployee by remember {
        mutableStateOf<EmployeeEntity?>(
            employees.find { it.id == initialSalary?.employeeId } ?: employees.find { it.name == initialSalary?.employeeName }
        )
    }
    var monthYear by remember { mutableStateOf(initialSalary?.monthYear ?: "August 2026") }
    var baseSalaryText by remember { mutableStateOf(initialSalary?.baseSalary?.toString() ?: "") }
    var bonusText by remember { mutableStateOf(initialSalary?.bonus?.toString() ?: "0") }
    var deductionText by remember { mutableStateOf(initialSalary?.deductions?.toString() ?: "0") }
    var paymentStatus by remember { mutableStateOf(initialSalary?.paymentStatus ?: "Paid") }
    var paymentMethod by remember { mutableStateOf(initialSalary?.paymentMethod ?: "Bank Transfer") }
    var notes by remember { mutableStateOf(initialSalary?.notes ?: "") }
    var empDropdownOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedEmployee) {
        if (selectedEmployee != null && baseSalaryText.isBlank() && initialSalary == null) {
            baseSalaryText = selectedEmployee!!.baseSalary.toString()
        }
    }

    val base = baseSalaryText.toDoubleOrNull() ?: 0.0
    val bonus = bonusText.toDoubleOrNull() ?: 0.0
    val deduction = deductionText.toDoubleOrNull() ?: 0.0
    val netSalary = (base + bonus - deduction).coerceAtLeast(0.0)

    val isStep1Valid = (selectedEmployee != null || initialSalary != null) && monthYear.isNotBlank()

    MultiStepFormDialog(
        title = if (initialSalary == null) "Process Salary" else "Edit Salary Payment",
        currentStep = step,
        totalSteps = 4,
        stepTitles = stepTitles,
        onDismissRequest = onDismiss,
        onBack = { if (step > 1) step-- },
        onNext = {
            if (step == 1 && !isStep1Valid) {
                error = "Please select an employee and enter salary period."
            } else {
                error = null
                if (step < 4) step++
            }
        },
        onSave = {
            val record = SalaryPaymentEntity(
                id = initialSalary?.id ?: 0L,
                employeeId = selectedEmployee?.id ?: initialSalary?.employeeId ?: 0L,
                employeeName = selectedEmployee?.name ?: initialSalary?.employeeName ?: "Employee",
                monthYear = monthYear.trim(),
                baseSalary = base,
                bonus = bonus,
                deductions = deduction,
                netSalary = netSalary,
                paymentStatus = paymentStatus.trim(),
                paymentMethod = paymentMethod.trim(),
                notes = notes.trim(),
                paymentDate = System.currentTimeMillis()
            )
            onSave(record)
            onDismiss()
        },
        isNextEnabled = when (step) {
            1 -> isStep1Valid
            else -> true
        },
        saveButtonText = "Disburse Salary"
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            when (currentStep) {
                1 -> {
                    Text("Step 1: Select Employee & Period", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = empDropdownOpen,
                        onExpandedChange = { empDropdownOpen = !empDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee?.let { "${it.name} (${it.position})" } ?: "Select Employee *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Employee *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empDropdownOpen) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = empDropdownOpen,
                            onDismissRequest = { empDropdownOpen = false }
                        ) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.name} - ${emp.position}") },
                                    onClick = {
                                        selectedEmployee = emp
                                        empDropdownOpen = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = monthYear,
                        onValueChange = { monthYear = it; error = null },
                        label = { Text("Salary Period / Month *") },
                        placeholder = { Text("e.g. August 2026") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text("Step 2: Base, Bonus & Deductions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = baseSalaryText,
                        onValueChange = { baseSalaryText = it },
                        label = { Text("Base Salary (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bonusText,
                            onValueChange = { bonusText = it },
                            label = { Text("Bonus (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deductionText,
                            onValueChange = { deductionText = it },
                            label = { Text("Deductions (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Calculated Net Payable:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(netSalary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                3 -> {
                    Text("Step 3: Payment Method & Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        placeholder = { Text("Bank Transfer, Cash, Cheque") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Payment Status", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = paymentStatus == "Paid", onClick = { paymentStatus = "Paid" }, label = { Text("Paid") })
                        FilterChip(selected = paymentStatus == "Pending", onClick = { paymentStatus = "Pending" }, label = { Text("Pending") })
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Payroll Notes / Reference") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                4 -> {
                    Text("Step 4: Review Payroll Disbursal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReviewRow("Employee Name", selectedEmployee?.name ?: "N/A")
                            ReviewRow("Job Position", selectedEmployee?.position ?: "N/A")
                            ReviewRow("Salary Period", monthYear)
                            ReviewRow("Base Salary", formatCurrency(base))
                            ReviewRow("Bonus", formatCurrency(bonus))
                            ReviewRow("Deductions", formatCurrency(deduction))
                            ReviewRow("Net Disbursed Salary", formatCurrency(netSalary))
                            ReviewRow("Payment Method", paymentMethod)
                            ReviewRow("Status", paymentStatus)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2-Step Multi-Step Settle Payment Dialog
 */
@Composable
fun SettlePaymentDialog(
    title: String,
    partyName: String,
    outstandingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    val stepTitles = listOf("Payment Amount", "Review Settlement")

    var amountText by remember { mutableStateOf(outstandingAmount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    MultiStepFormDialog(
        title = title,
        currentStep = step,
        totalSteps = 2,
        stepTitles = stepTitles,
        onDismissRequest = onDismiss,
        onBack = { if (step > 1) step-- },
        onNext = {
            if (amount <= 0.0) {
                error = "Please enter a valid amount greater than 0."
            } else {
                error = null
                step = 2
            }
        },
        onSave = {
            onConfirm(amount)
            onDismiss()
        },
        isNextEnabled = amount > 0.0,
        saveButtonText = "Record Settlement"
    ) { currentStep ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            when (currentStep) {
                1 -> {
                    Text("Party Name: $partyName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Current Outstanding: ${formatCurrency(outstandingAmount)}", style = MaterialTheme.typography.bodyMedium, color = DangerRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; error = null },
                        label = { Text("Settlement Amount (Rs.) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text("Review Payment Settlement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReviewRow("Party Name", partyName)
                            ReviewRow("Total Outstanding", formatCurrency(outstandingAmount))
                            ReviewRow("Amount Being Settled", formatCurrency(amount))
                            ReviewRow("Remaining Balance", formatCurrency((outstandingAmount - amount).coerceAtLeast(0.0)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
