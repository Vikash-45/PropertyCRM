package com.propertycrm.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propertycrm.app.R
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.ui.components.CallOutcomeDialog
import com.propertycrm.app.ui.components.DoNotCallBadge
import com.propertycrm.app.ui.components.EmptyStateCard
import com.propertycrm.app.ui.components.MarkCompletedDialog
import com.propertycrm.app.ui.components.RescheduleDialog
import com.propertycrm.app.ui.components.StageChip
import com.propertycrm.app.ui.components.StatusBadge
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkBorder
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusCompleted
import com.propertycrm.app.ui.theme.StatusCompletedBg
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.StatusOverdueBg
import com.propertycrm.app.ui.theme.StatusToday
import com.propertycrm.app.ui.theme.StatusTodayBg
import com.propertycrm.app.ui.theme.StatusUpcoming
import com.propertycrm.app.ui.theme.StatusUpcomingBg
import com.propertycrm.app.ui.theme.TextMuted
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToFollowUpsFilter: (String) -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var activeRescheduleItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeCompleteItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeCallOutcomeItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var dncPromptItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCustomer,
                containerColor = PrimaryBlue,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.add_customer), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real Estate Dealings & Customer Follow-up CRM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Stat Cards Grid: Distinct Counts (Item 7)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Top row: Today Due & Overdue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCardDetailed(
                            title = stringResource(R.string.today_pending),
                            mainCount = uiState.uniqueTodayCustomersCount,
                            mainLabel = "Customers",
                            subCount = uiState.totalTodayTasksCount,
                            subLabel = "Calls",
                            accentColor = StatusToday,
                            bgColor = StatusTodayBg,
                            icon = Icons.Default.NotificationImportant,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToFollowUpsFilter("TODAY") }
                        )
                        StatCardDetailed(
                            title = stringResource(R.string.overdue),
                            mainCount = uiState.uniqueOverdueCustomersCount,
                            mainLabel = "Customers",
                            subCount = uiState.totalOverdueTasksCount,
                            subLabel = "Calls",
                            accentColor = StatusOverdue,
                            bgColor = StatusOverdueBg,
                            icon = Icons.Default.EventBusy,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToFollowUpsFilter("OVERDUE") }
                        )
                    }

                    // Middle row: Upcoming Tasks, Site Visits, Completed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = stringResource(R.string.upcoming),
                            count = uiState.upcomingTasksCount,
                            accentColor = StatusUpcoming,
                            bgColor = StatusUpcomingBg,
                            icon = Icons.Default.HourglassTop,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToFollowUpsFilter("UPCOMING") }
                        )
                        StatCard(
                            title = stringResource(R.string.upcoming_visits),
                            count = uiState.upcomingSiteVisitsCount,
                            accentColor = PrimaryBlue,
                            bgColor = DarkSurfaceVariant,
                            icon = Icons.Default.EventAvailable,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCustomers
                        )
                        StatCard(
                            title = stringResource(R.string.completed),
                            count = uiState.completedTasksCount,
                            accentColor = StatusCompleted,
                            bgColor = StatusCompletedBg,
                            icon = Icons.Default.Verified,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToFollowUpsFilter("COMPLETED") }
                        )
                    }
                }
            }

            // Section: Today's Follow-ups
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.todays_followups),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (uiState.todayFollowUps.isNotEmpty()) {
                        Text(
                            text = "View All (${uiState.totalTodayTasksCount} calls)",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToFollowUpsFilter("TODAY") }
                        )
                    }
                }
            }

            if (uiState.todayFollowUps.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.no_followups_today),
                        message = "You are all caught up for today! Add a new customer or review upcoming follow-ups.",
                        icon = Icons.Default.Check,
                        actionLabel = stringResource(R.string.add_customer),
                        onActionClick = onNavigateToAddCustomer
                    )
                }
            } else {
                items(uiState.todayFollowUps) { item ->
                    DashboardFollowUpCard(
                        item = item,
                        onCallClick = {
                            if (item.customer.isDoNotCall) {
                                dncPromptItem = item
                            } else {
                                PhoneUtils.dialCustomer(context, item.customer.phone)
                                // Offer to log call outcome right away
                                activeCallOutcomeItem = item
                            }
                        },
                        onRecordOutcomeClick = { activeCallOutcomeItem = item },
                        onRescheduleClick = { activeRescheduleItem = item },
                        onClick = { onNavigateToCustomerDetail(item.customer.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Do Not Call override prompt
    dncPromptItem?.let { item ->
        AlertDialog(
            onDismissRequest = { dncPromptItem = null },
            title = { Text(text = "Do Not Call Customer", color = StatusOverdue) },
            text = { Text(stringResource(R.string.dnc_call_prompt), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        PhoneUtils.dialCustomer(context, item.customer.phone)
                        dncPromptItem = null
                        activeCallOutcomeItem = item
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                ) {
                    Text("Call Anyway", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dncPromptItem = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Call Outcome Dialog
    activeCallOutcomeItem?.let { item ->
        CallOutcomeDialog(
            customerName = item.customer.name,
            onDismiss = { activeCallOutcomeItem = null },
            onConfirm = { outcome, notes, nextDate, nextTime ->
                viewModel.recordCallOutcome(item.customer.id, outcome, notes, nextDate, nextTime)
                activeCallOutcomeItem = null
            }
        )
    }

    // Reschedule Dialog
    activeRescheduleItem?.let { item ->
        RescheduleDialog(
            initialDateMillis = item.followUp.followUpDate,
            onDismiss = { activeRescheduleItem = null },
            onConfirm = { newDate, newTime, reason ->
                viewModel.rescheduleFollowUp(item.followUp.id, newDate, newTime, reason)
                activeRescheduleItem = null
            }
        )
    }

    // Complete Dialog
    activeCompleteItem?.let { item ->
        MarkCompletedDialog(
            customerName = item.customer.name,
            onDismiss = { activeCompleteItem = null },
            onConfirm = { notes, _ ->
                viewModel.markFollowUpCompleted(item.followUp.id, notes)
                activeCompleteItem = null
            }
        )
    }
}

@Composable
fun StatCardDetailed(
    title: String,
    mainCount: Int,
    mainLabel: String,
    subCount: Int,
    subLabel: String,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mainCount.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = mainLabel,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Text(
                text = "$subCount $subLabel • $title",
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DashboardFollowUpCard(
    item: FollowUpWithCustomer,
    onCallClick: () -> Unit,
    onRecordOutcomeClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Customer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = item.customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryBlue
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.customer.isDoNotCall) {
                        DoNotCallBadge()
                    }
                    StatusBadge(status = item.followUp.status, dateMillis = item.followUp.followUpDate)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chips: Stage & Property Type & Requirement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageChip(stage = item.customer.stage)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.customer.requirementType.getDisplayName()} ${item.customer.propertyType.getDisplayName()}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!item.customer.budget.isNullOrBlank()) {
                    Text(
                        text = "• ₹${item.customer.budget}",
                        color = StatusCompleted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (item.customer.inquiryReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Inquiry: ${item.customer.inquiryReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            if (!item.followUp.followUpTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = item.followUp.followUpTime, fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.customer.isDoNotCall) StatusOverdue else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (item.customer.isDoNotCall) "DNC" else "Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Outcome / Done Button
                Button(
                    onClick = onRecordOutcomeClick,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Outcome", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Reschedule Button
                OutlinedButton(
                    onClick = onRescheduleClick,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Reschedule", fontSize = 11.sp)
                }
            }
        }
    }
}
