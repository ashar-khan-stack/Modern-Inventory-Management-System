package com.example

import com.example.ui.viewmodel.DashboardSummaryTotals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SaleOrderEntity
import com.example.data.repository.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.util.BiometricHelper
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

import com.example.ui.util.PermissionManager
import com.example.ui.components.PermissionRationaleDialog

enum class AppScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    NEW_SALE("New Sale", Icons.Default.AddShoppingCart),
    SALES("Sales / History", Icons.Default.ReceiptLong),
    PURCHASES("Purchases", Icons.Default.ShoppingBag),
    CUSTOMER_INFO("Customer Info", Icons.Default.People),
    EMPLOYEE_INFO("Employee Info", Icons.Default.Badge),
    EXPENSES("Expenses", Icons.Default.Receipt),
    SALARIES("Salaries / Payroll", Icons.Default.Payments),
    LEDGERS("Ledgers", Icons.Default.MenuBook),
    REPORTS("Reports & P&L", Icons.Default.BarChart),
    ACCEPTANCE_REPORT("Acceptance Report", Icons.Default.Verified),
    SETTINGS("Settings", Icons.Default.Settings),
    INVOICE_VIEW("Invoice / Receipt", Icons.Default.Receipt)
}

enum class PendingPermission {
    NONE,
    STORAGE,
    NOTIFICATION,
    CAMERA
}

class MainActivity : FragmentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var themePreferenceManager: ThemePreferenceManager
    private lateinit var businessProfileManager: BusinessProfileManager
    private lateinit var permissionManager: PermissionManager

    override fun onResume() {
        super.onResume()
        if (::authRepository.isInitialized) {
            authRepository.checkAndEnforceSessionExpiration()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authRepository = AuthRepository.getInstance(applicationContext)
        themePreferenceManager = ThemePreferenceManager.getInstance(applicationContext)
        businessProfileManager = BusinessProfileManager.getInstance(applicationContext)
        permissionManager = PermissionManager(this)

        authRepository.checkAndEnforceSessionExpiration()

        setContent {
            val themeMode by themePreferenceManager.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }


            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("app_permissions_prefs", android.content.Context.MODE_PRIVATE) }
            var pendingPermission by remember { mutableStateOf(PendingPermission.NONE) }

            fun checkNextPermission() {
                pendingPermission = when {
                    permissionManager.shouldShowStorageRationale() -> PendingPermission.STORAGE
                    permissionManager.shouldShowNotificationRationale() -> PendingPermission.NOTIFICATION
                    permissionManager.shouldShowCameraRationale() -> PendingPermission.CAMERA
                    else -> PendingPermission.NONE
                }
            }

            LaunchedEffect(Unit) {
                checkNextPermission()
            }

            if (pendingPermission == PendingPermission.STORAGE) {
                PermissionRationaleDialog(
                    title = "Storage Access Required",
                    description = "To save and export your PDF invoices, reports, and backup data, the app requires write access to your device storage.",
                    confirmButtonText = "Grant Access",
                    dismissButtonText = "Not Now",
                    onConfirm = {
                        permissionManager.requestStoragePermission { checkNextPermission() }
                    },
                    onDismiss = {
                        sharedPrefs.edit().putBoolean("has_requested_storage", true).apply()
                        checkNextPermission()
                    }
                )
            }

            if (pendingPermission == PendingPermission.NOTIFICATION) {
                PermissionRationaleDialog(
                    title = "Notification Permission",
                    description = "Allow notifications so the app can send important reminders, alerts, and payroll updates.",
                    confirmButtonText = "Allow",
                    dismissButtonText = "Not Now",
                    onConfirm = {
                        permissionManager.requestNotificationPermission {
                            com.example.ui.util.OutstandingPaymentNotificationManager.syncOutstandingNotifications(this@MainActivity)
                            checkNextPermission()
                        }
                    },
                    onDismiss = {
                        sharedPrefs.edit().putBoolean("has_requested_notification", true).apply()
                        checkNextPermission()
                    }
                )
            }

            if (pendingPermission == PendingPermission.CAMERA) {
                PermissionRationaleDialog(
                    title = "Camera Access Required",
                    description = "Allow camera access to take photos of items for your sales invoices.",
                    confirmButtonText = "Allow",
                    dismissButtonText = "Not Now",
                    onConfirm = {
                        permissionManager.requestCameraPermission { checkNextPermission() }
                    },
                    onDismiss = {
                        sharedPrefs.edit().putBoolean("has_requested_camera", true).apply()
                        checkNextPermission()
                    }
                )
            }

            MyApplicationTheme(darkTheme = isDark) {
                val currentSession by authRepository.currentUserSession.collectAsStateWithLifecycle()

                if (currentSession == null) {
                    AuthScreen(
                        authRepository = authRepository,
                        onAuthSuccess = { /* Session will update reactively */ },
                        onLaunchBiometricPrompt = { onSuccess, onError ->
                            BiometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                title = "Biometric Sign In",
                                subtitle = "Touch the fingerprint sensor or glance at camera to sign in",
                                onSuccess = onSuccess,
                                onError = onError
                            )
                        }
                    )
                } else {
                    MainApp(
                        currentSession = currentSession,
                        authRepository = authRepository,
                        themePreferenceManager = themePreferenceManager,
                        businessProfileManager = businessProfileManager,
                        onLaunchBiometricPrompt = { onSuccess, onError ->
                            BiometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                title = "Enable Biometric Login",
                                subtitle = "Authenticate with your fingerprint or face scan to confirm",
                                onSuccess = onSuccess,
                                onError = onError
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    currentSession: UserSession?,
    authRepository: AuthRepository,
    themePreferenceManager: ThemePreferenceManager,
    businessProfileManager: BusinessProfileManager,
    onLaunchBiometricPrompt: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var previousScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var selectedInvoiceForView by remember { mutableStateOf<SaleOrderEntity?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (com.example.BuildConfig.DEBUG) {
            com.example.ui.util.AppStartupDiagnostics.runStartupDiagnostics(com.example.data.local.AppDatabase.getInstance(context))
            com.example.ui.util.DebugNavigationLogger.logScreenState("Startup", "Diagnostics completed.")
        }
        com.example.ui.util.OutstandingPaymentNotificationManager.syncOutstandingNotifications(context)
        com.example.ui.util.OutstandingPaymentScheduler.schedulePeriodicCheck(context)
        com.example.ui.util.OutstandingPaymentNotificationWorker.scheduleWork(context)
    }

    LaunchedEffect(currentScreen) {
        if (com.example.BuildConfig.DEBUG) {
            com.example.ui.util.DebugNavigationLogger.logNavigation(previousScreen.name, currentScreen.name)
        }
        if (currentScreen != AppScreen.INVOICE_VIEW) {
            previousScreen = currentScreen
        }
    }

    // State Collection from ViewModel
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val salaryPayments by viewModel.salaryPayments.collectAsStateWithLifecycle()
    val dashboardSummaryTotals by viewModel.dashboardSummaryTotals.collectAsStateWithLifecycle()


    // Handle back button when on sub-screens or drawer is open
    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.DASHBOARD) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (currentScreen == AppScreen.INVOICE_VIEW) {
            currentScreen = previousScreen
        } else {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Business Manager",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "Enterprise Business Suite",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    // Navigation Items
                    Text(
                        text = "CORE OPERATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerNavMenuItem(
                        screen = AppScreen.DASHBOARD,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.DASHBOARD
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.NEW_SALE,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.NEW_SALE
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.SALES,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.SALES
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PARTNERS & FINANCES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerNavMenuItem(
                        screen = AppScreen.CUSTOMER_INFO,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.CUSTOMER_INFO
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.EMPLOYEE_INFO,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.EMPLOYEE_INFO
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.SALARIES,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.SALARIES
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.PURCHASES,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.PURCHASES
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.EXPENSES,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.EXPENSES
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerNavMenuItem(
                        screen = AppScreen.LEDGERS,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.LEDGERS
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ANALYTICS & SYSTEM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerNavMenuItem(
                        screen = AppScreen.REPORTS,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.REPORTS
                            scope.launch { drawerState.close() }
                        }
                    )
                    if (com.example.BuildConfig.DEBUG) {
                        DrawerNavMenuItem(
                            screen = AppScreen.ACCEPTANCE_REPORT,
                            currentScreen = currentScreen,
                            onClick = {
                                currentScreen = AppScreen.ACCEPTANCE_REPORT
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                    DrawerNavMenuItem(
                        screen = AppScreen.SETTINGS,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.SETTINGS
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentScreen != AppScreen.INVOICE_VIEW) {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentScreen.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("drawer_menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        sales = sales,
                        expenses = expenses,
                        customers = customers,
                        employees = employees,
                        salaryPayments = salaryPayments,
                        summaryTotals = DashboardSummaryTotals(totalRevenue = dashboardSummaryTotals.totalSales, totalExpenditure = dashboardSummaryTotals.totalExpenses),
                        currentSession = currentSession,
                        onNavigateToSales = { currentScreen = AppScreen.NEW_SALE },
                        onNavigateToExpenses = { currentScreen = AppScreen.EXPENSES },
                        onNavigateToPeople = { currentScreen = AppScreen.CUSTOMER_INFO },
                        onSelectInvoice = { sale ->
                            previousScreen = AppScreen.DASHBOARD
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        }
                    )
                                        AppScreen.NEW_SALE -> NewSaleScreen(
                        customers = customers,
                        onProcessSale = { customer, items, paid, method, disc, tax, notes, onSuccess ->
                            viewModel.processCustomSale(customer, items, paid, method, disc, tax, notes, onSuccess)
                        },
                        onViewInvoice = { sale ->
                            previousScreen = AppScreen.NEW_SALE
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        },
                        onAddCustomer = { viewModel.saveCustomer(it) }
                    )
                                                            AppScreen.SALES -> SalesHistoryScreen(
                        customers = customers,
                        pastSales = sales,
                        onProcessSale = { customer, items, paid, method, disc, tax, notes, onSuccess ->
                            viewModel.processCustomSale(customer, items, paid, method, disc, tax, notes, onSuccess)
                        },
                        onDeleteSale = { sale -> viewModel.deleteSale(sale) },
                        onUpdateSalePayment = { sale, amount, onSuccess ->
                            viewModel.updateSalePayment(sale, amount, onSuccess)
                        },
                        onViewInvoice = { sale ->
                            previousScreen = AppScreen.SALES
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        },
                        onAddCustomer = { viewModel.saveCustomer(it) },
                        onNavigateToNewSale = { currentScreen = AppScreen.NEW_SALE }
                    )
                    AppScreen.CUSTOMER_INFO -> CustomerInfoScreen(
                        customers = customers,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) }
                    )
                    AppScreen.EMPLOYEE_INFO -> EmployeeInfoScreen(
                        employees = employees,
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) }
                    )
                    AppScreen.SALARIES -> SalariesScreen(
                        employees = employees,
                        salaryPayments = salaryPayments,
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
                    )
                    AppScreen.PURCHASES -> PurchasesScreen(
                        expenses = expenses,
                        onSavePurchase = { viewModel.saveExpense(it) },
                        onDeletePurchase = { viewModel.deleteExpense(it) }
                    )
                    AppScreen.LEDGERS -> LedgersScreen(
                        customers = customers,
                        sales = sales
                    )
                    AppScreen.EXPENSES -> ExpensesScreen(
                        expenses = expenses,
                        onSaveExpense = { viewModel.saveExpense(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                    AppScreen.REPORTS -> ReportsScreen(
                        sales = sales,
                        expenses = expenses,
                        customers = customers,
                        salaryPayments = salaryPayments
                    )
                    AppScreen.ACCEPTANCE_REPORT -> {
                        if (com.example.BuildConfig.DEBUG) {
                            com.example.ui.screens.AcceptanceReportScreen(
                                /* No parameter needed */
                            )
                        } else {
                            // Empty in production
                        }
                    }
                    AppScreen.SETTINGS -> SettingsScreen(
                        currentSession = currentSession,
                        authRepository = authRepository,
                        themePreferenceManager = themePreferenceManager,
                        businessProfileManager = businessProfileManager,
                        dashboardTotals = DashboardSummaryTotals(totalRevenue = dashboardSummaryTotals.totalSales, totalExpenditure = dashboardSummaryTotals.totalExpenses),
                        onSignOut = { currentScreen = AppScreen.DASHBOARD },
                        onResetData = { viewModel.resetAndReseedData() },
                        onLaunchBiometricPrompt = onLaunchBiometricPrompt
                    )
                    AppScreen.INVOICE_VIEW -> InvoiceScreen(
                        sale = selectedInvoiceForView,
                        onBackClick = { currentScreen = previousScreen },
                        onNewSaleClick = {
                            currentScreen = AppScreen.NEW_SALE
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerNavMenuItem(
    screen: AppScreen,
    currentScreen: AppScreen,
    badge: String? = null,
    badgeColor: Color = BrandBluePrimary,
    onClick: () -> Unit
) {
    val isSelected = currentScreen == screen
    NavigationDrawerItem(
        icon = { Icon(screen.icon, contentDescription = screen.title) },
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(screen.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BrandBluePrimaryContainer,
            selectedIconColor = BrandBlueOnPrimaryContainer,
            selectedTextColor = BrandBlueOnPrimaryContainer
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
