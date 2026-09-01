package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccountEntity
import com.example.data.model.BankTransactionEntity
import com.example.data.model.VoucherEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankScreen(
    bankAccounts: List<BankAccountEntity>,
    bankTransactions: List<BankTransactionEntity>,
    vouchers: List<VoucherEntity>,
    onSaveAccount: (BankAccountEntity) -> Unit,
    onDeleteAccount: (BankAccountEntity) -> Unit,
    onRecordTransaction: (bankAccountId: Long, type: String, amount: Double, desc: String, refVch: String, targetAccountId: Long?) -> Unit,
    onCreateVoucher: (type: String, accountName: String, desc: String, amount: Double, isDebit: Boolean, bankAccountId: Long?, notes: String) -> Unit,
    onDeleteVoucher: (VoucherEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Accounts & Summary", "Bank Statement", "Vouchers")

    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<BankAccountEntity?>(null) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionTypeToAdd by remember { mutableStateOf("Deposit") }

    var showVoucherDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> BankAccountsAndSummaryTab(
                bankAccounts = bankAccounts,
                bankTransactions = bankTransactions,
                onAddAccount = {
                    accountToEdit = null
                    showAccountDialog = true
                },
                onEditAccount = { account ->
                    accountToEdit = account
                    showAccountDialog = true
                },
                onDeleteAccount = onDeleteAccount
            )
            1 -> BankStatementTab(
                bankAccounts = bankAccounts,
                bankTransactions = bankTransactions,
                onNewDeposit = {
                    transactionTypeToAdd = "Deposit"
                    showTransactionDialog = true
                },
                onNewWithdrawal = {
                    transactionTypeToAdd = "Withdrawal"
                    showTransactionDialog = true
                },
                onNewTransfer = {
                    transactionTypeToAdd = "Transfer"
                    showTransactionDialog = true
                }
            )
            2 -> VouchersTab(
                vouchers = vouchers,
                bankAccounts = bankAccounts,
                onCreateVoucherClick = { showVoucherDialog = true },
                onDeleteVoucher = onDeleteVoucher
            )
        }
    }

    if (showAccountDialog) {
        BankAccountDialog(
            account = accountToEdit,
            onDismiss = { showAccountDialog = false },
            onConfirm = { acc ->
                onSaveAccount(acc)
                showAccountDialog = false
            }
        )
    }

    if (showTransactionDialog) {
        BankTransactionDialog(
            initialType = transactionTypeToAdd,
            bankAccounts = bankAccounts,
            onDismiss = { showTransactionDialog = false },
            onConfirm = { accId, type, amt, desc, refVch, targetAccId ->
                onRecordTransaction(accId, type, amt, desc, refVch, targetAccId)
                showTransactionDialog = false
            }
        )
    }

    if (showVoucherDialog) {
        CreateVoucherDialog(
            bankAccounts = bankAccounts,
            onDismiss = { showVoucherDialog = false },
            onConfirm = { type, accName, desc, amt, isDebit, bankAccId, notes ->
                onCreateVoucher(type, accName, desc, amt, isDebit, bankAccId, notes)
                showVoucherDialog = false
            }
        )
    }
}

@Composable
private fun BankAccountsAndSummaryTab(
    bankAccounts: List<BankAccountEntity>,
    bankTransactions: List<BankTransactionEntity>,
    onAddAccount: () -> Unit,
    onEditAccount: (BankAccountEntity) -> Unit,
    onDeleteAccount: (BankAccountEntity) -> Unit
) {
    val totalBalance = bankAccounts.sumOf { it.currentBalance }
    val totalDebit = bankTransactions.sumOf { it.debit }
    val totalCredit = bankTransactions.sumOf { it.credit }
    val activeAccounts = bankAccounts.count { it.status.equals("Active", ignoreCase = true) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BANK SUMMARY OVERVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rs. ${"%,.2f".format(totalBalance)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text("Total Current Bank Balance across $activeAccounts active account(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Debit (Outflow)", style = MaterialTheme.typography.labelMedium)
                            Text("Rs. ${"%,.2f".format(totalDebit)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Credit (Inflow)", style = MaterialTheme.typography.labelMedium)
                            Text("Rs. ${"%,.2f".format(totalCredit)}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bank Accounts (${bankAccounts.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Button(
                    onClick = onAddAccount,
                    modifier = Modifier.testTag("add_bank_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Account")
                }
            }
        }

        if (bankAccounts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No bank accounts registered yet.")
                        Text("Click 'Add Account' above to set up your business bank account.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(bankAccounts, key = { it.id }) { account ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(account.bankName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Title: ${account.accountTitle}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (account.status == "Active") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    account.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (account.status == "Active") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Account #: ${account.accountNumber}", style = MaterialTheme.typography.bodyMedium)
                        if (account.iban.isNotBlank()) Text("IBAN: ${account.iban}", style = MaterialTheme.typography.bodySmall)
                        if (account.branchName.isNotBlank()) Text("Branch: ${account.branchName}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Opening: Rs. ${"%,.2f".format(account.openingBalance)}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Current Balance: Rs. ${"%,.2f".format(account.currentBalance)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                IconButton(onClick = { onEditAccount(account) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Account")
                                }
                                IconButton(onClick = { onDeleteAccount(account) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankStatementTab(
    bankAccounts: List<BankAccountEntity>,
    bankTransactions: List<BankTransactionEntity>,
    onNewDeposit: () -> Unit,
    onNewWithdrawal: () -> Unit,
    onNewTransfer: () -> Unit
) {
    var selectedBankId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }

    val filteredTransactions = remember(selectedBankId, searchQuery, selectedTypeFilter, bankTransactions) {
        bankTransactions.filter { tx ->
            val matchesBank = selectedBankId == null || tx.bankAccountId == selectedBankId
            val matchesSearch = searchQuery.isBlank() || tx.description.contains(searchQuery, ignoreCase = true) || tx.referenceVoucher.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == "All" || tx.transactionType.equals(selectedTypeFilter, ignoreCase = true)
            matchesBank && matchesSearch && matchesType
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNewDeposit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deposit", fontSize = 12.sp)
                }
                Button(
                    onClick = onNewWithdrawal,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdrawal", fontSize = 12.sp)
                }
                Button(
                    onClick = onNewTransfer,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Transfer", fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search description or voucher #") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Statement Entries (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No bank transactions found.")
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                val account = bankAccounts.find { it.id == tx.bankAccountId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                tx.referenceVoucher.ifBlank { "Voucher #${tx.id}" },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                dateFormat.format(Date(tx.transactionDate)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Account: ${account?.bankName ?: "Bank Account #${tx.bankAccountId}"} (${account?.accountNumber ?: ""})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(tx.description, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (tx.debit > 0) {
                                Text("Debit: Rs. ${"%,.2f".format(tx.debit)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            }
                            if (tx.credit > 0) {
                                Text("Credit: Rs. ${"%,.2f".format(tx.credit)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                            Text("Amount: Rs. ${"%,.2f".format(tx.amount)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VouchersTab(
    vouchers: List<VoucherEntity>,
    bankAccounts: List<BankAccountEntity>,
    onCreateVoucherClick: () -> Unit,
    onDeleteVoucher: (VoucherEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Accounting Vouchers (${vouchers.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Button(
                    onClick = onCreateVoucherClick,
                    modifier = Modifier.testTag("create_voucher_button")
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Voucher")
                }
            }
        }

        if (vouchers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No accounting vouchers created yet.")
                    }
                }
            }
        } else {
            items(vouchers, key = { it.id }) { vch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vch.voucherNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    vch.voucherType,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Date: ${dateFormat.format(Date(vch.date))}", style = MaterialTheme.typography.bodySmall)
                        Text("Account: ${vch.accountName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Description: ${vch.description}", style = MaterialTheme.typography.bodyMedium)
                        if (vch.referenceNotes.isNotBlank()) Text("Notes: ${vch.referenceNotes}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (vch.debit > 0) {
                                    Text("Debit: Rs. ${"%,.2f".format(vch.debit)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                }
                                if (vch.credit > 0) {
                                    Text("Credit: Rs. ${"%,.2f".format(vch.credit)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { onDeleteVoucher(vch) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Voucher", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankAccountDialog(
    account: BankAccountEntity?,
    onDismiss: () -> Unit,
    onConfirm: (BankAccountEntity) -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var accountTitle by remember { mutableStateOf(account?.accountTitle ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var iban by remember { mutableStateOf(account?.iban ?: "") }
    var branchName by remember { mutableStateOf(account?.branchName ?: "") }
    var openingBalanceStr by remember { mutableStateOf(account?.openingBalance?.toString() ?: "0.0") }
    var status by remember { mutableStateOf(account?.status ?: "Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Add Bank Account" else "Edit Bank Account") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_name_input")
                )
                OutlinedTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it },
                    label = { Text("Account Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_title_input")
                )
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("account_number_input")
                )
                OutlinedTextField(
                    value = iban,
                    onValueChange = { iban = it },
                    label = { Text("IBAN (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch Name / Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = openingBalanceStr,
                    onValueChange = { openingBalanceStr = it },
                    label = { Text("Opening Balance (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val openBal = openingBalanceStr.toDoubleOrNull() ?: 0.0
                    val currentBal = account?.currentBalance ?: openBal
                    val acc = BankAccountEntity(
                        id = account?.id ?: 0L,
                        bankName = bankName.trim(),
                        accountTitle = accountTitle.trim(),
                        accountNumber = accountNumber.trim(),
                        iban = iban.trim(),
                        branchName = branchName.trim(),
                        openingBalance = openBal,
                        currentBalance = currentBal,
                        status = status
                    )
                    onConfirm(acc)
                },
                enabled = bankName.isNotBlank() && accountTitle.isNotBlank() && accountNumber.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankTransactionDialog(
    initialType: String,
    bankAccounts: List<BankAccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (bankAccountId: Long, type: String, amount: Double, desc: String, refVch: String, targetAccountId: Long?) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var selectedBank by remember { mutableStateOf(bankAccounts.firstOrNull()) }
    var targetBank by remember { mutableStateOf(bankAccounts.getOrNull(1)) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var refVoucher by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Bank $type") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Bank Account *", style = MaterialTheme.typography.labelMedium)
                bankAccounts.forEach { acc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedBank?.id == acc.id,
                            onClick = { selectedBank = acc }
                        )
                        Text("${acc.bankName} (${acc.accountNumber}) - Bal: Rs. ${"%,.2f".format(acc.currentBalance)}")
                    }
                }

                if (type == "Transfer") {
                    Text("Select Destination Bank *", style = MaterialTheme.typography.labelMedium)
                    bankAccounts.filter { it.id != selectedBank?.id }.forEach { acc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = targetBank?.id == acc.id,
                                onClick = { targetBank = acc }
                            )
                            Text("${acc.bankName} (${acc.accountNumber})")
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (Rs.) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = refVoucher,
                    onValueChange = { refVoucher = it },
                    label = { Text("Voucher / Reference # (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val acc = selectedBank ?: return@Button
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && description.isNotBlank()) {
                        onConfirm(
                            acc.id,
                            type,
                            amt,
                            description.trim(),
                            refVoucher.trim(),
                            if (type == "Transfer") targetBank?.id else null
                        )
                    }
                },
                enabled = selectedBank != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0 && description.isNotBlank()
            ) {
                Text("Record Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateVoucherDialog(
    bankAccounts: List<BankAccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (type: String, accountName: String, desc: String, amount: Double, isDebit: Boolean, bankAccountId: Long?, notes: String) -> Unit
) {
    var voucherType by remember { mutableStateOf("Receipt") }
    var accountName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isDebit by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var selectedBankId by remember { mutableStateOf<Long?>(null) }

    val voucherTypes = listOf("Receipt", "Payment", "Bank Deposit", "Bank Withdrawal", "Bank Transfer", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Accounting Voucher") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voucher Type", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    voucherTypes.take(3).forEach { vType ->
                        FilterChip(
                            selected = voucherType == vType,
                            onClick = {
                                voucherType = vType
                                isDebit = vType in listOf("Payment", "Bank Withdrawal")
                            },
                            label = { Text(vType, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account / Party Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_account_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Particulars / Description *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_desc_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (Rs.) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Entry Mode: ")
                    RadioButton(selected = isDebit, onClick = { isDebit = true })
                    Text("Debit")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = !isDebit, onClick = { isDebit = false })
                    Text("Credit")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (accountName.isNotBlank() && description.isNotBlank() && amt > 0) {
                        onConfirm(voucherType, accountName.trim(), description.trim(), amt, isDebit, selectedBankId, notes.trim())
                    }
                },
                enabled = accountName.isNotBlank() && description.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Create Voucher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
