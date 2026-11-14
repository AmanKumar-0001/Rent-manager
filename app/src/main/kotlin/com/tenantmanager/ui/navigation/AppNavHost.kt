package com.tenantmanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tenantmanager.TenantApplication
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.ui.dashboard.DashboardScreen
import com.tenantmanager.ui.dashboard.DashboardViewModel
import com.tenantmanager.ui.payments.PaymentsListScreen
import com.tenantmanager.ui.payments.PaymentsViewModel
import com.tenantmanager.ui.payments.RecordPaymentDialog
import com.tenantmanager.ui.properties.PropertiesScreen
import com.tenantmanager.ui.properties.PropertiesViewModel
import com.tenantmanager.ui.settings.SettingsScreen
import com.tenantmanager.ui.settings.SettingsViewModel
import com.tenantmanager.ui.tenants.AddEditTenantScreen
import com.tenantmanager.ui.tenants.AddEditTenantViewModel
import com.tenantmanager.ui.tenants.TenantDetailScreen
import com.tenantmanager.ui.tenants.TenantDetailViewModel
import com.tenantmanager.ui.tenants.TenantsListScreen
import com.tenantmanager.ui.tenants.TenantsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BottomNavItem(
    val title: String,
    val destination: Destination,
    val icon: ImageVector
)

@Composable
fun AppNavHost(
    app: TenantApplication,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val container = app.container
    val coroutineScope = rememberCoroutineScope()
    var activePaymentLedger by remember { mutableStateOf<TenantLedger?>(null) }
    val settingsState by container.settingsRepository.getSettings().collectAsState(initial = com.tenantmanager.domain.repository.AppSettings())

    fun launchRecordPayment(tenantId: String) {
        coroutineScope.launch {
            val tenant = container.tenantRepository.getTenantById(tenantId)
            if (tenant != null) {
                val payments = container.paymentRepository.getPaymentsForTenant(tenantId).first()
                val ledger = RentCalculator.calculateLedger(
                    tenant = tenant,
                    payments = payments,
                    asOfDate = LocalDate.now(),
                    graceDays = settingsState.graceDays
                )
                activePaymentLedger = ledger
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Destination.Dashboard, Icons.Default.Dashboard),
        BottomNavItem("Tenants", Destination.Tenants, Icons.Default.People),
        BottomNavItem("Payments", Destination.Payments, Icons.Default.Payments),
        BottomNavItem("Settings", Destination.Settings, Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = bottomNavItems.any { it.destination.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.destination.route,
                            onClick = {
                                navController.navigate(item.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable(Destination.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(
                        container.tenantRepository,
                        container.paymentRepository,
                        container.settingsRepository
                    )
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTenantDetail = { tenantId ->
                        navController.navigate(Destination.TenantDetail.createRoute(tenantId))
                    },
                    onNavigateToAddTenant = {
                        navController.navigate(Destination.AddEditTenant.createRoute())
                    },
                    onRecordPayment = ::launchRecordPayment
                )
            }

            // Tenants List
            composable(Destination.Tenants.route) {
                val viewModel: TenantsViewModel = viewModel(
                    factory = TenantsViewModel.provideFactory(
                        container.tenantRepository,
                        container.paymentRepository,
                        container.settingsRepository
                    )
                )
                TenantsListScreen(
                    viewModel = viewModel,
                    onNavigateToTenantDetail = { tenantId ->
                        navController.navigate(Destination.TenantDetail.createRoute(tenantId))
                    },
                    onNavigateToAddTenant = {
                        navController.navigate(Destination.AddEditTenant.createRoute())
                    },
                    onNavigateToProperties = {
                        navController.navigate(Destination.Properties.route)
                    },
                    onRecordPayment = ::launchRecordPayment
                )
            }

            // Payments List
            composable(Destination.Payments.route) {
                val viewModel: PaymentsViewModel = viewModel(
                    factory = PaymentsViewModel.provideFactory(
                        container.paymentRepository,
                        container.tenantRepository,
                        container.settingsRepository
                    )
                )
                PaymentsListScreen(
                    viewModel = viewModel,
                    onNavigateToTenantDetail = { tenantId ->
                        navController.navigate(Destination.TenantDetail.createRoute(tenantId))
                    }
                )
            }

            // Settings
            composable(Destination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        settingsRepository = container.settingsRepository,
                        tenantRepository = container.tenantRepository,
                        propertyRepository = container.propertyRepository,
                        paymentRepository = container.paymentRepository,
                        syncRepository = container.syncRepository,
                        remoteMirror = container.remoteMirror,
                        googleAuthManager = container.googleAuthManager,
                        backupExporter = container.backupExporter,
                        backupImporter = container.backupImporter
                    )
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToProperties = {
                        navController.navigate(Destination.Properties.route)
                    }
                )
            }

            // Properties
            composable(Destination.Properties.route) {
                val viewModel: PropertiesViewModel = viewModel(
                    factory = PropertiesViewModel.provideFactory(
                        container.propertyRepository,
                        container.tenantRepository,
                        container.settingsRepository
                    )
                )
                PropertiesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Tenant Detail
            composable(
                route = Destination.TenantDetail.route,
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tenantId = backStackEntry.arguments?.getString("tenantId") ?: return@composable
                val viewModel: TenantDetailViewModel = viewModel(
                    factory = TenantDetailViewModel.provideFactory(
                        tenantId,
                        container.tenantRepository,
                        container.propertyRepository,
                        container.paymentRepository,
                        container.settingsRepository
                    )
                )
                TenantDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Destination.AddEditTenant.createRoute(id))
                    },
                    onRecordPayment = ::launchRecordPayment
                )
            }

            // Add/Edit Tenant
            composable(
                route = Destination.AddEditTenant.route,
                arguments = listOf(
                    navArgument("tenantId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val tenantId = backStackEntry.arguments?.getString("tenantId")
                val viewModel: AddEditTenantViewModel = viewModel(
                    factory = AddEditTenantViewModel.provideFactory(
                        tenantId,
                        container.tenantRepository,
                        container.propertyRepository
                    )
                )
                AddEditTenantScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Modal Record Payment Dialog
    if (activePaymentLedger != null) {
        RecordPaymentDialog(
            ledger = activePaymentLedger!!,
            currencySymbol = settingsState.currencySymbol,
            onConfirmPayment = { payment ->
                coroutineScope.launch {
                    container.paymentRepository.insertPayment(payment)
                    activePaymentLedger = null
                }
            },
            onDismiss = { activePaymentLedger = null }
        )
    }
}

