package com.propertycrm.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Customers : Screen("customers", "Customers", Icons.Default.People)
    object AICopilot : Screen("ai_copilot", "AI Copilot", Icons.Default.SmartToy)
    object FollowUps : Screen("follow_ups?filter={filter}", "Follow-ups", Icons.AutoMirrored.Filled.Assignment) {
        fun createRoute(filter: String = "ALL"): String = "follow_ups?filter=$filter"
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Sub screens
    object AddCustomer : Screen("add_customer", "Add Customer")
    object EditCustomer : Screen("edit_customer/{customerId}", "Edit Customer") {
        fun createRoute(customerId: Long): String = "edit_customer/$customerId"
    }
    object CustomerDetail : Screen("customer_detail/{customerId}", "Customer Details") {
        fun createRoute(customerId: Long): String = "customer_detail/$customerId"
    }
    object DailySummary : Screen("daily_summary", "Daily Reminder Summary")

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Customers, AICopilot, FollowUps, Settings)
    }
}
