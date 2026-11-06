package com.propertycrm.app.ui.followup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propertycrm.app.R
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.ui.components.AddFollowUpDialog
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
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.StatusToday
import com.propertycrm.app.ui.theme.StatusUpcoming
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils

@Composable
fun FollowUpScreen(
    viewModel: FollowUpViewModel,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val followUps by viewModel.followUps.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val context = LocalContext.current

    var activeRescheduleItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeCompleteItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }
    var activeAddFollowUpItem by remember { mutableStateOf<FollowUpWithCustomer?>(null) }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.follow_ups),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FollowUpFilterTab.values().forEach { tab ->
                    val isSelected = selectedFilter == tab
                    val label = when (tab) {
                        FollowUpFilterTab.TODAY -> stringResource(R.string.filter_today)
                        FollowUpFilterTab.OVERDUE -> stringResource(R.string.filter_overdue)
                        FollowUpFilterTab.UPCOMING -> stringResource(R.string.filter_upcoming)
                        FollowUpFilterTab.COMPLETED -> stringResource(R.string.filter_completed)
                        FollowUpFilterTab.ALL -> stringResource(R.string.filter_all)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(tab) },
                        label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (tab) {
                                FollowUpFilterTab.OVERDUE -> StatusOverdue
                                FollowUpFilterTab.COMPLETED -> StatusCompleted
                                FollowUpFilterTab.TODAY -> StatusToday
                                FollowUpFilterTab.UPCOMING -> StatusUpcoming
                                else -> PrimaryBlue
                            },
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (followUps.isEmpty()) {
                val emptyMsg = when (selectedFilter) {
                    FollowUpFilterTab.TODAY -> "No follow-ups due today."
                    FollowUpFilterTab.OVERDUE -> "No overdue follow-ups. You're right on schedule!"
                    FollowUpFilterTab.UPCOMING -> "No upcoming follow-ups scheduled."
                    FollowUpFilterTab.COMPLETED -> "No completed follow-ups yet."
                    FollowUpFilterTab.ALL -> "No follow-up records found."
                }
                EmptyStateCard(
                    title = "All Clean",
                    message = emptyMsg,
                    icon = Icons.Default.FilterList
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(followUps) { item ->
                        FollowUpCardItem(
                            item = item,
                            onCallClick = { PhoneUtils.dialCustomer(context, item.customer.phone) },
                            onCompleteClick = { activeCompleteItem = item },
                            onRescheduleClick = { activeRescheduleItem = item },
                            onAddFollowUpClick = { activeAddFollowUpItem = item },
                            onOpenCustomerDetails = { onNavigateToCustomerDetail(item.customer.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Reschedule Dialog
    activeRescheduleItem?.let { item ->
        RescheduleDialog(
            initialDateMillis = item.followUp.followUpDate,
            onDismiss = { activeRescheduleItem = null },
            onConfirm = { newDate, newTime, reason ->
                viewModel.reschedule(item.followUp.id, newDate, newTime, reason)
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
                viewModel.markCompleted(item.followUp.id, notes)
                activeCompleteItem = null
            }
        )
    }

    // Add Follow-up Dialog
    activeAddFollowUpItem?.let { item ->
        AddFollowUpDialog(
            onDismiss = { activeAddFollowUpItem = null },
            onConfirm = { date, time, notes ->
                viewModel.addFollowUp(item.customer.id, date, time, notes)
                activeAddFollowUpItem = null
            }
        )
    }
}

@Composable
fun FollowUpCardItem(
    item: FollowUpWithCustomer,
    onCallClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onAddFollowUpClick: () -> Unit,
    onOpenCustomerDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCustomerDetails() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, Phone & Status
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
                StatusBadge(status = item.followUp.status, dateMillis = item.followUp.followUpDate)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Due: ${DateUtils.formatDate(item.followUp.followUpDate)}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!item.followUp.followUpTime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• ${item.followUp.followUpTime}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                StageChip(stage = item.customer.stage)
            }

            // Inquiry details
            if (item.customer.inquiryReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inquiry: ${item.customer.inquiryReason} (${item.customer.propertyType.getDisplayName()})",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Follow-up notes
            if (item.followUp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${item.followUp.notes}",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Call, Done, Reschedule, Add More, Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action 1: Call Customer (ACTION_DIAL)
                Button(
                    onClick = onCallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Action 2: Mark completed (if pending)
                if (item.followUp.status == FollowUpStatus.PENDING) {
                    Button(
                        onClick = onCompleteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Action 3: Reschedule
                    OutlinedButton(
                        onClick = onRescheduleClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Reschedule", fontSize = 11.sp)
                    }
                } else {
                    // Action 4: Add another follow-up
                    Button(
                        onClick = onAddFollowUpClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Follow-up", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 5: Details
                OutlinedButton(
                    onClick = onOpenCustomerDetails,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Details", fontSize = 11.sp)
                }
            }
        }
    }
}
