package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.util.BiometricHelper
import com.example.ui.util.FinancialReportExporter
import com.example.ui.util.ValidationUtils
import com.example.ui.viewmodel.DashboardSummaryTotals
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    currentSession: UserSession?,
    authRepository: AuthRepository,
    themePreferenceManager: ThemePreferenceManager,
    businessProfileManager: BusinessProfileManager,
    dashboardTotals: DashboardSummaryTotals,
    onSignOut: () -> Unit,
    onResetData: () -> Unit,
    onLaunchBiometricPrompt: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val backupManager = remember { BackupManager(context, db) }

    val currentTheme by themePreferenceManager.themeMode.collectAsState()
    val currentProfile by businessProfileManager.profile.collectAsState()

    // Profile form state with live validation
    var companyName by remember(currentProfile) { mutableStateOf(currentProfile.companyName) }
    var taxId by remember(currentProfile) { mutableStateOf(currentProfile.taxId) }
    var currencySymbol by remember(currentProfile) { mutableStateOf(currentProfile.currencySymbol.ifBlank { "Rs" }) }
    var phoneDigits by remember(currentProfile) { mutableStateOf(ValidationUtils.sanitizePkPhoneDigits(currentProfile.phone)) }
    var email by remember(currentProfile) { mutableStateOf(currentProfile.email) }
    var address by remember(currentProfile) { mutableStateOf(currentProfile.address) }
    var website by remember(currentProfile) { mutableStateOf(currentProfile.website) }

    val isCompanyNameValid = companyName.trim().isNotBlank()
    val (isPhoneValid, phoneError) = remember(phoneDigits) { ValidationUtils.validatePkPhone(phoneDigits, isRequired = false) }
    val (isEmailValid, emailError) = remember(email) { ValidationUtils.validateEmail(email, isRequired = false) }
    val isProfileFormValid = isCompanyNameValid && isPhoneValid && isEmailValid

    // Edit mode state for Business Profile
    var isEditingProfile by remember { mutableStateOf(false) }
    var showDeleteProfileDialog by remember { mutableStateOf(false) }

    var isFingerprintEnabled by remember(currentSession) {
        mutableStateOf(currentSession?.isFingerprintEnabled ?: false)
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreValidation by remember { mutableStateOf<BackupValidationResult?>(null) }
    var isBackupLoading by remember { mutableStateOf(false) }

    val biometricStatus = remember { BiometricHelper.checkBiometricStatus(context) }
    val isBiometricCapable = biometricStatus == BiometricHelper.BiometricStatus.AVAILABLE

    // SAF Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isBackupLoading = true
            scope.launch {
                val result = backupManager.writeBackupToUri(uri)
                isBackupLoading = false
                if (result.isSuccess) {
                    Toast.makeText(context, "Entire dataset exported successfully to JSON!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Backup failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isBackupLoading = true
            scope.launch {
                val validation = backupManager.validateBackupFile(uri)
                isBackupLoading = false
                if (validation.isValid) {
                    pendingRestoreUri = uri
                    pendingRestoreValidation = validation
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "Invalid Backup File: ${validation.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = FinancialReportExporter.writeSummaryReportToUri(
                    context = context,
                    uri = uri,
                    totals = dashboardTotals,
                    companyName = if (currentProfile.isSaved && currentProfile.companyName.isNotBlank()) currentProfile.companyName else "Business Profile"
                )
                if (result.isSuccess) {
                    Toast.makeText(context, "Financial summary exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Manage account, appearance, backups & company profile",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // 1. Account & Security Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSession?.fullName ?: "Administrator Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = currentSession?.email ?: "admin@inventorymaster.com",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                // Biometric / Fingerprint Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isBiometricCapable) BrandBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Fingerprint / Biometric Login",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isBiometricCapable) {
                                    "Quickly authenticate and access your inventory with fingerprint"
                                } else {
                                    "Fingerprint hardware is unavailable or not enrolled on this device"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Switch(
                        modifier = Modifier.testTag("biometric_switch"),
                        checked = isFingerprintEnabled && isBiometricCapable,
                        enabled = isBiometricCapable,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onLaunchBiometricPrompt(
                                    {
                                        scope.launch {
                                            authRepository.setFingerprintEnabled(true)
                                            isFingerprintEnabled = true
                                            Toast.makeText(context, "Fingerprint login enabled!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    { err ->
                                        Toast.makeText(context, "Verification required: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                scope.launch {
                                    authRepository.setFingerprintEnabled(false)
                                    isFingerprintEnabled = false
                                    Toast.makeText(context, "Fingerprint login disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // 2. Appearance & Theme Selection
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Display Theme & Appearance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Select your preferred color theme mode",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppThemeMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = { themePreferenceManager.setThemeMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandBluePrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            tint = if (currentTheme == mode) BrandBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (currentTheme == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        // 3. Backup & Restore (JSON) & Financial Report Export
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Data Backup, Restore & Reporting",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Export complete JSON backup of the entire application dataset, restore records, or export financial reports",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    if (isBackupLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                            exportBackupLauncher.launch("AppBackup_$timestamp.json")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTealTertiary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_button")
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Financial Summary Action
                OutlinedButton(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                        exportReportLauncher.launch("financial_summary_report_$timestamp.txt")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Financial Dashboard Summary Report")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        FinancialReportExporter.shareSummaryReport(
                            context = context,
                            totals = dashboardTotals,
                            companyName = if (currentProfile.isSaved && currentProfile.companyName.isNotBlank()) currentProfile.companyName else "Business Profile"
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Financial Statement Directly", fontSize = 13.sp)
                }
            }
        }

                // 4. Business / Company Profile Card (State Machine: No Profile -> Saved Profile -> Edit / Delete)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Business Profile",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Manage your business information",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Condition A: Saved Profile Display Mode (When profile is saved and not currently in edit mode)
                    if (currentProfile.isSaved && !isEditingProfile) {
                        // Polished VIP Saved Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(BrandBluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = "Business logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = currentProfile.companyName.ifBlank { "Company Name Not Set" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreenContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Active Profile",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Detailed Summary Rows
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (currentProfile.taxId.isNotBlank()) {
                                ProfileDetailRow(icon = Icons.Default.Receipt, label = "Tax / VAT ID", value = currentProfile.taxId)
                            }
                            ProfileDetailRow(icon = Icons.Default.Phone, label = "Phone Number", value = currentProfile.phone.ifBlank { "Not provided" })
                            ProfileDetailRow(icon = Icons.Default.Email, label = "Email Address", value = currentProfile.email.ifBlank { "Not provided" })
                            ProfileDetailRow(icon = Icons.Default.LocationOn, label = "Store / Office Address", value = currentProfile.address.ifBlank { "Not provided" })
                            if (currentProfile.website.isNotBlank()) {
                                ProfileDetailRow(icon = Icons.Default.Language, label = "Website", value = currentProfile.website)
                            }
                            ProfileDetailRow(icon = Icons.Default.AttachMoney, label = "Currency Symbol", value = currentProfile.currencySymbol)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: Edit and Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    companyName = currentProfile.companyName
                                    taxId = currentProfile.taxId
                                    currencySymbol = currentProfile.currencySymbol.ifBlank { "Rs" }
                                    phoneDigits = ValidationUtils.sanitizePkPhoneDigits(currentProfile.phone)
                                    email = currentProfile.email
                                    address = currentProfile.address
                                    website = currentProfile.website
                                    isEditingProfile = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("edit_profile_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showDeleteProfileDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("delete_profile_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", fontWeight = FontWeight.SemiBold)
                            }
                        }

                    } else {
                        // Condition B: Empty / Setup / Edit Form Mode
                        if (!currentProfile.isSaved) {
                            // Professional Empty State Banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BrandBluePrimaryContainer.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BrandBluePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Set Up Your Business Profile",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Add your business information to personalize invoices and receipts.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Section 1: Business Information
                        Text(
                            text = "Business Information",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company / Business Name *") },
                            placeholder = { Text("e.g. Acme Enterprise Corp") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            singleLine = true,
                            isError = !isCompanyNameValid && companyName.isNotBlank(),
                            supportingText = {
                                if (companyName.isBlank()) {
                                    Text("Business Name is required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_company_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = taxId,
                                onValueChange = { taxId = it },
                                label = { Text("Tax / VAT ID") },
                                placeholder = { Text("Enter Tax/VAT ID") },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_tax_id_input")
                            )
                            OutlinedTextField(
                                value = currencySymbol,
                                onValueChange = { currencySymbol = it },
                                label = { Text("Currency Symbol *") },
                                placeholder = { Text("Rs") },
                                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_currency_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Website / Online Portal (Optional)") },
                            placeholder = { Text("https://mycompany.com") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_website_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Section 2: Contact Information
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneDigits,
                            onValueChange = { input ->
                                phoneDigits = ValidationUtils.sanitizePkPhoneDigits(input)
                            },
                            label = { Text("Phone Number (Pakistan Mobile)") },
                            placeholder = { Text("300 1234567") },
                            prefix = { Text("+92 ", fontWeight = FontWeight.Bold, color = BrandBluePrimary) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            isError = !isPhoneValid && phoneDigits.isNotBlank(),
                            supportingText = {
                                if (!isPhoneValid && phoneDigits.isNotBlank()) {
                                    Text(phoneError ?: "Invalid number", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Format: +92 3XX XXXXXXX", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = { Text("Email Address") },
                            placeholder = { Text("contact@company.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            isError = !isEmailValid && email.isNotBlank(),
                            supportingText = {
                                if (!isEmailValid && email.isNotBlank()) {
                                    Text(emailError ?: "Invalid email address", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("e.g. name@domain.com", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_email_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Section 3: Business Address
                        Text(
                            text = "Business Address",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Store / Office Address") },
                            placeholder = { Text("Shop 25, Block A, Gulshan-e-Iqbal, Karachi") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_address_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons for Form Mode
                        val onSaveClick: () -> Unit = {
                            if (companyName.isBlank()) {
                                Toast.makeText(context, "Business name is required.", Toast.LENGTH_SHORT).show()
                            } else if (!isPhoneValid) {
                                Toast.makeText(context, phoneError ?: "Invalid Pakistan phone number.", Toast.LENGTH_SHORT).show()
                            } else if (!isEmailValid) {
                                Toast.makeText(context, emailError ?: "Invalid email address.", Toast.LENGTH_SHORT).show()
                            } else {
                                businessProfileManager.saveProfile(
                                    BusinessProfile(
                                        companyName = companyName.trim(),
                                        taxId = taxId.trim(),
                                        currencySymbol = currencySymbol.trim().ifBlank { "Rs" },
                                        phone = ValidationUtils.toCanonicalPkPhone(phoneDigits),
                                        email = email.trim(),
                                        address = address.trim(),
                                        website = website.trim(),
                                        logoUrl = currentProfile.logoUrl,
                                        isSaved = true
                                    )
                                )
                                isEditingProfile = false
                                Toast.makeText(context, "Business profile saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        if (isEditingProfile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        companyName = currentProfile.companyName
                                        taxId = currentProfile.taxId
                                        currencySymbol = currentProfile.currencySymbol.ifBlank { "Rs" }
                                        phoneDigits = ValidationUtils.sanitizePkPhoneDigits(currentProfile.phone)
                                        email = currentProfile.email
                                        address = currentProfile.address
                                        website = currentProfile.website
                                        isEditingProfile = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("cancel_edit_profile_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = onSaveClick,
                                    enabled = isProfileFormValid,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("save_profile_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Button(
                                onClick = onSaveClick,
                                enabled = isProfileFormValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Profile", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        
        // 5. Database & System Maintenance
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "System Maintenance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Perform database reset or flush temporary tables",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_demo_data_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Application Records")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirmation Dialogs
    if (showDeleteProfileDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProfileDialog = false },
            title = { Text("Delete Business Profile?") },
            text = {
                Text("Are you sure you want to delete this business/company profile?\n\nNote: This will only remove your profile details. Your customers, products, sales, purchases, expenses, employees, and all other business data will NOT be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        businessProfileManager.deleteProfile()
                        companyName = ""
                        taxId = ""
                        currencySymbol = "Rs"
                        phoneDigits = ""
                        email = ""
                        address = ""
                        website = ""
                        isEditingProfile = false
                        showDeleteProfileDialog = false
                        Toast.makeText(context, "Business profile deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_delete_profile_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteProfileDialog = false },
                    modifier = Modifier.testTag("cancel_delete_profile_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out of Account?") },
            text = { Text("You will be returned to the login screen. All your business records, inventory, and database data remain securely saved on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        authRepository.logout()
                        onSignOut()
                        Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear All Application Data?") },
            text = { Text("This will permanently clear all customers, sales orders, expenses, and salary payments. User accounts remain active. Proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                        Toast.makeText(context, "All application data cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestoreValidation != null && pendingRestoreUri != null) {
        val valResult = pendingRestoreValidation!!
        val uriToRestore = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Confirm Backup Restoration") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Valid backup file verified (Date: ${valResult.backupDate}). The following records will be restored into your database:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Customers: ${valResult.customersCount}", fontWeight = FontWeight.SemiBold)
                    Text("• Sales Orders: ${valResult.salesCount} (${valResult.saleItemsCount} items)", fontWeight = FontWeight.SemiBold)
                    Text("• Operating Expenses: ${valResult.expensesCount}", fontWeight = FontWeight.SemiBold)
                    Text("• Staff & Salaries: ${valResult.employeesCount} staff, ${valResult.salariesCount} payments", fontWeight = FontWeight.SemiBold)
                    Text("• User Accounts: ${valResult.usersCount}", fontWeight = FontWeight.SemiBold)
                    if (valResult.hasBusinessProfile) {
                        Text("• Business Profile: Included", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Existing items with matching IDs will be safely merged and updated. Proceed?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        isBackupLoading = true
                        scope.launch {
                            val result = backupManager.restoreBackupFromUri(uriToRestore)
                            isBackupLoading = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Complete backup restored successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text("Proceed with Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BrandBluePrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value.ifBlank { "Not provided" },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
    }
}
