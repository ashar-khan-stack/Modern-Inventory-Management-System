package com.example

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
    SALES("Sales / POS", Icons.Default.ReceiptLong),
    PEOPLE("People", Icons.Default.People),
    EXPENSES("Expenses", Icons.Default.Receipt),
    REPORTS("Reports & P&L", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings),
    INVOICE_VIEW("Invoice / Receipt", Icons.Default.Receipt)
}

enum class PendingPermission {
    NONE,
    STORAGE,
    NOTIFICATION
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

            LaunchedEffect(Unit) {
                if (permissionManager.shouldShowStorageRationale()) {
                    pendingPermission = PendingPermission.STORAGE
                } else if (permissionManager.shouldShowNotificationRationale()) {
                    pendingPermission = PendingPermission.NOTIFICATION
                }
            }

            if (pendingPermission == PendingPermission.STORAGE) {
                PermissionRationaleDialog(
                    title = "Storage Access Required",
                    description = "This permission allows the app to save and manage exported files such as invoices, reports, and backups.",
                    onConfirm = {
                        permissionManager.requestStoragePermission { isGranted ->
                            if (permissionManager.shouldShowNotificationRationale()) {
                                pendingPermission = PendingPermission.NOTIFICATION
                            } else {
                                pendingPermission = PendingPermission.NONE
                            }
                        }
                    },
                    onDismiss = {
                        sharedPrefs.edit().putBoolean("has_requested_storage", true).apply()
                        if (permissionManager.shouldShowNotificationRationale()) {
                            pendingPermission = PendingPermission.NOTIFICATION
                        } else {
                            pendingPermission = PendingPermission.NONE
                        }
                    }
                )
            }

            if (pendingPermission == PendingPermission.NOTIFICATION) {
                PermissionRationaleDialog(
                    title = "Notification Permission Required",
                    description = "Allow notifications so the app can send important reminders, alerts, and payroll updates.",
                    onConfirm = {
                        permissionManager.requestNotificationPermission { isGranted ->
                            pendingPermission = PendingPermission.NONE
                        }
                    },
                    onDismiss = {
                        sharedPrefs.edit().putBoolean("has_requested_notification", true).apply()
                        pendingPermission = PendingPermission.NONE
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

    // State Collection from ViewModel
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val salaryPayments by viewModel.salaryPayments.collectAsStateWithLifecycle()
    val dashboardSummaryTotals by viewModel.dashboardSummaryTotals.collectAsStateWithLifecycle()

    val posCart by viewModel.posCart.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()

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
                        screen = AppScreen.PEOPLE,
                        currentScreen = currentScreen,
                        onClick = {
                            currentScreen = AppScreen.PEOPLE
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
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        sales = sales,
                        expenses = expenses,
                        customers = customers,
                        salaryPayments = salaryPayments,
                        summaryTotals = dashboardSummaryTotals,
                        currentSession = currentSession,
                        onNavigateToSales = { currentScreen = AppScreen.SALES },
                        onNavigateToExpenses = { currentScreen = AppScreen.EXPENSES },
                        onSelectInvoice = { sale ->
                            previousScreen = AppScreen.DASHBOARD
                            selectedInvoiceForView = sale
                            currentScreen = AppScreen.INVOICE_VIEW
                        }
                    )
                    AppScreen.SALES -> SalesScreen(
                        products = emptyList(),
                        customers = customers,
                        pastSales = sales,
                        cartItems = posCart,
                        selectedCustomer = selectedCustomer,
                        onCustomerSelected = { viewModel.selectCustomer(it) },
                        onAddToCart = { },
                        onUpdateCartQty = { id, qty -> viewModel.updatePosCartItemQuantity(id, qty) },
                        onRemoveFromCart = { id -> viewModel.removeFromPosCart(id) },
                        onClearCart = { viewModel.clearPosCart() },
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
                        onAddCustomer = { viewModel.saveCustomer(it) }
                    )
                    AppScreen.PEOPLE -> PeopleScreen(
                        customers = customers,
                        employees = employees,
                        salaryPayments = salaryPayments,
                        sales = sales,
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onDeleteCustomer = { viewModel.deleteCustomer(it) },
                        onSettlePayment = { id, amt -> viewModel.settleCustomerPayment(id, amt) },
                        onSaveEmployee = { viewModel.saveEmployee(it) },
                        onDeleteEmployee = { viewModel.deleteEmployee(it) },
                        onDisburseSalary = { viewModel.disburseSalary(it) },
                        onUpdateSalary = { viewModel.updateSalary(it) },
                        onDeleteSalary = { viewModel.deleteSalary(it) }
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
                    AppScreen.SETTINGS -> SettingsScreen(
                        currentSession = currentSession,
                        authRepository = authRepository,
                        themePreferenceManager = themePreferenceManager,
                        businessProfileManager = businessProfileManager,
                        dashboardTotals = dashboardSummaryTotals,
                        onSignOut = { currentScreen = AppScreen.DASHBOARD },
                        onResetData = { viewModel.resetAndReseedData() },
                        onLaunchBiometricPrompt = onLaunchBiometricPrompt
                    )
                    AppScreen.INVOICE_VIEW -> InvoiceScreen(
                        sale = selectedInvoiceForView,
                        onBackClick = { currentScreen = previousScreen },
                        onNewSaleClick = {
                            viewModel.clearPosCart()
                            currentScreen = AppScreen.SALES
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
