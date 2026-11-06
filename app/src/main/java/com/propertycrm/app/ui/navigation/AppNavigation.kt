package com.propertycrm.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.propertycrm.app.PropertyCrmApp
import com.propertycrm.app.ui.customer.AddEditCustomerScreen
import com.propertycrm.app.ui.customer.AddEditCustomerViewModel
import com.propertycrm.app.ui.customer.CustomerDetailScreen
import com.propertycrm.app.ui.customer.CustomerDetailViewModel
import com.propertycrm.app.ui.customer.CustomerListScreen
import com.propertycrm.app.ui.customer.CustomerListViewModel
import com.propertycrm.app.ui.dashboard.DashboardScreen
import com.propertycrm.app.ui.dashboard.DashboardViewModel
import com.propertycrm.app.ui.followup.DailyReminderSummaryScreen
import com.propertycrm.app.ui.followup.FollowUpScreen
import com.propertycrm.app.ui.followup.FollowUpViewModel
import com.propertycrm.app.ui.settings.SettingsScreen
import com.propertycrm.app.ui.settings.SettingsViewModel
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.TextMuted

@Composable
fun AppNavigation(
    navController: NavHostController,
    appContainer: PropertyCrmApp
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom navigation on primary 4 screens
    val isBottomBarVisible = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Customers.route,
        Screen.FollowUps.route,
        Screen.Settings.route
    )

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = PrimaryBlue,
                    tonalElevation = androidx.compose.ui.unit.dp(4)
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = when (screen) {
                            Screen.FollowUps -> currentRoute?.startsWith("follow_ups") == true
                            else -> currentRoute == screen.route
                        }

                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(text = screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = PrimaryBlue,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Screen 1: Dashboard
            composable(Screen.Dashboard.route) {
                val vm = DashboardViewModel(
                    customerRepository = appContainer.customerRepository,
                    followUpRepository = appContainer.followUpRepository
                )
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToAddCustomer = { navController.navigate(Screen.AddCustomer.route) },
                    onNavigateToFollowUpsFilter = { filter ->
                        navController.navigate(Screen.FollowUps.createRoute(filter))
                    },
                    onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                    onNavigateToCustomerDetail = { id ->
                        navController.navigate(Screen.CustomerDetail.createRoute(id))
                    }
                )
            }

            // Screen 2: Customers List
            composable(Screen.Customers.route) {
                val vm = CustomerListViewModel(
                    customerRepository = appContainer.customerRepository
                )
                CustomerListScreen(
                    viewModel = vm,
                    onNavigateToAddCustomer = { navController.navigate(Screen.AddCustomer.route) },
                    onNavigateToCustomerDetail = { id ->
                        navController.navigate(Screen.CustomerDetail.createRoute(id))
                    }
                )
            }

            // Screen 3: Follow-ups Management
            composable(
                route = Screen.FollowUps.route,
                arguments = listOf(
                    navArgument("filter") {
                        type = NavType.StringType
                        defaultValue = "ALL"
                    }
                )
            ) { backStackEntry ->
                val filterArg = backStackEntry.arguments?.getString("filter") ?: "ALL"
                val vm = FollowUpViewModel(
                    followUpRepository = appContainer.followUpRepository,
                    initialFilter = filterArg
                )
                FollowUpScreen(
                    viewModel = vm,
                    onNavigateToCustomerDetail = { id ->
                        navController.navigate(Screen.CustomerDetail.createRoute(id))
                    }
                )
            }

            // Screen 4: Combined Daily Reminder Summary (Item 9)
            composable(Screen.DailySummary.route) {
                DailyReminderSummaryScreen(
                    followUpRepository = appContainer.followUpRepository,
                    customerRepository = appContainer.customerRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCustomerDetail = { id ->
                        navController.navigate(Screen.CustomerDetail.createRoute(id))
                    }
                )
            }

            // Screen 5: Customer Details
            composable(
                route = Screen.CustomerDetail.route,
                arguments = listOf(
                    navArgument("customerId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                val vm = CustomerDetailViewModel(
                    customerId = customerId,
                    customerRepository = appContainer.customerRepository,
                    followUpRepository = appContainer.followUpRepository
                )
                CustomerDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditCustomer = { id ->
                        navController.navigate(Screen.EditCustomer.createRoute(id))
                    }
                )
            }

            // Screen 6: Add Customer
            composable(Screen.AddCustomer.route) {
                val vm = AddEditCustomerViewModel(
                    customerRepository = appContainer.customerRepository
                )
                AddEditCustomerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 7: Edit Customer
            composable(
                route = Screen.EditCustomer.route,
                arguments = listOf(
                    navArgument("customerId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                val vm = AddEditCustomerViewModel(
                    customerRepository = appContainer.customerRepository,
                    existingCustomerId = customerId
                )
                AddEditCustomerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 8: Settings
            composable(Screen.Settings.route) {
                val vm = SettingsViewModel(
                    preferenceManager = appContainer.preferenceManager,
                    backupRepository = appContainer.backupRepository,
                    reminderScheduler = appContainer.reminderScheduler,
                    reminderNotificationManager = appContainer.reminderNotificationManager
                )
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
