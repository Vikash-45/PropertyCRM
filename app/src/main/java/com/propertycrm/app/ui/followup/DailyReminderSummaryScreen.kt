package com.propertycrm.app.ui.followup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import com.propertycrm.app.ui.components.CallOutcomeDialog
import com.propertycrm.app.ui.components.MarkCompletedDialog
import com.propertycrm.app.ui.components.RescheduleDialog
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.StatusToday
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.PhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReminderSummaryScreen(
    followUpRepository: FollowUpRepository,
    customerRepository: CustomerRepository,
    onNavigateBack: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val todayFollowUps by followUpRepository.getTodayFollowUps().collectAsState(initial = emptyList())
    val overdueFollowUps by followUpRepository.getOverdueFollowUps().collectAsState(initial = emptyList())
    val context = LocalContext.current

    var activeRescheduleItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeCompleteItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeCallOutcomeItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "Daily Reminder Follow-ups", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SECTION A: Due Today
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECTION A: Due Today (${todayFollowUps.size} tasks)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusToday
                    )
                }
            }

            if (todayFollowUps.isEmpty()) {
                item {
                    Text(text = "No pending follow-ups due today.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                items(todayFollowUps) { item ->
                    FollowUpCardItem(
                        item = item,
                        onCallClick = {
                            PhoneUtils.dialCustomer(context, item.customer.phone)
                            activeCallOutcomeItem = item
                        },
                        onCompleteClick = { activeCompleteItem = item },
                        onRescheduleClick = { activeRescheduleItem = item },
                        onAddFollowUpClick = {},
                        onOpenCustomerDetails = { onNavigateToCustomerDetail(item.customer.id) }
                    )
                }
            }

            // SECTION B: Overdue
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECTION B: Overdue (${overdueFollowUps.size} tasks)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusOverdue
                    )
                }
            }

            if (overdueFollowUps.isEmpty()) {
                item {
                    Text(text = "No overdue follow-ups.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                items(overdueFollowUps) { item ->
                    FollowUpCardItem(
                        item = item,
                        onCallClick = {
                            PhoneUtils.dialCustomer(context, item.customer.phone)
                            activeCallOutcomeItem = item
                        },
                        onCompleteClick = { activeCompleteItem = item },
                        onRescheduleClick = { activeRescheduleItem = item },
                        onAddFollowUpClick = {},
                        onOpenCustomerDetails = { onNavigateToCustomerDetail(item.customer.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Call Outcome Dialog
    activeCallOutcomeItem?.let { item ->
        CallOutcomeDialog(
            customerName = item.customer.name,
            onDismiss = { activeCallOutcomeItem = null },
            onConfirm = { outcome, notes, nextDate, nextTime ->
                CoroutineScope(Dispatchers.IO).launch {
                    customerRepository.recordCallOutcome(item.customer.id, outcome, notes, nextDate, nextTime)
                }
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
                CoroutineScope(Dispatchers.IO).launch {
                    followUpRepository.rescheduleFollowUp(item.followUp.id, newDate, newTime, reason)
                }
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
                CoroutineScope(Dispatchers.IO).launch {
                    followUpRepository.markCompleted(item.followUp.id, notes)
                }
                activeCompleteItem = null
            }
        )
    }
}
