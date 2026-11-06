package com.propertycrm.app.ui.customer

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
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
import com.propertycrm.app.ui.theme.StatusToday
import com.propertycrm.app.ui.theme.StatusTodayBg
import com.propertycrm.app.ui.theme.StatusUpcoming
import com.propertycrm.app.ui.theme.StatusUpcomingBg
import com.propertycrm.app.ui.theme.TextMuted
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditCustomer: (Long) -> Unit
) {
    val data by viewModel.customerData.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddFollowUpDialog by remember { mutableStateOf(false) }
    var activeRescheduleFollowUp by remember { mutableStateOf<FollowUp?>(null) }
    var activeCompleteFollowUp by remember { mutableStateOf<FollowUp?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CustomerDetailEvent.CustomerDeleted -> onNavigateBack()
            }
        }
    }

    val customer = data?.customer
    val followUps = data?.followUps ?: emptyList()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.customer_details), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (customer != null) {
                        IconButton(onClick = { PhoneUtils.dialCustomer(context, customer.phone) }) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = PrimaryBlue)
                        }
                        IconButton(onClick = { onNavigateToEditCustomer(customer.id) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary)
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (customer.isArchived) "Unarchive" else "Archive", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (customer.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                onClick = {
                                    viewModel.toggleArchive()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Customer", color = StatusOverdue) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StatusOverdue)
                                },
                                onClick = {
                                    showDeleteConfirm = true
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading customer...", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Profile Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PrimaryBlue
                                    )
                                }
                                StageChip(stage = customer.stage)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Property Details
                            DetailRow("Inquiry Need", customer.inquiryReason)
                            DetailRow("Property Type", customer.propertyType.getDisplayName())
                            if (customer.preferredLocation.isNotBlank()) {
                                DetailRow("Location", customer.preferredLocation)
                            }
                            if (!customer.budget.isNullOrBlank()) {
                                DetailRow("Budget", customer.budget)
                            }
                            if (!customer.leadSource.isNullOrBlank()) {
                                DetailRow("Lead Source", customer.leadSource)
                            }
                            if (customer.firstVisitDate != null) {
                                DetailRow("First Visit", DateUtils.formatDate(customer.firstVisitDate))
                            }
                            if (customer.notes.isNotBlank()) {
                                DetailRow("Notes", customer.notes)
                            }
                            DetailRow("Created On", DateUtils.formatDate(customer.createdAt))

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Call button
                            Button(
                                onClick = { PhoneUtils.dialCustomer(context, customer.phone) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call Customer Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Stage Changer Section
                item {
                    Text(
                        text = "Change Customer Stage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomerStage.values().forEach { stg ->
                            val isSelected = customer.stage == stg
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateStage(stg) },
                                label = { Text(stg.getDisplayName()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (stg) {
                                        CustomerStage.WON -> StatusCompleted
                                        CustomerStage.LOST -> StatusOverdue
                                        else -> PrimaryBlue
                                    },
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Follow-up History Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.followup_history),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Button(
                            onClick = { showAddFollowUpDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Add Follow-up", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (followUps.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(R.string.no_followup_history),
                            message = "No follow-ups recorded yet. Schedule the first follow-up call with this customer.",
                            actionLabel = "Add Follow-up",
                            onActionClick = { showAddFollowUpDialog = true }
                        )
                    }
                } else {
                    items(followUps) { fu ->
                        FollowUpTimelineCard(
                            followUp = fu,
                            onComplete = { activeCompleteFollowUp = fu },
                            onReschedule = { activeRescheduleFollowUp = fu }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Customer?", color = TextPrimary) },
            text = { Text(stringResource(R.string.confirm_delete_customer), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Follow-up Dialog
    if (showAddFollowUpDialog) {
        AddFollowUpDialog(
            onDismiss = { showAddFollowUpDialog = false },
            onConfirm = { date, time, notes ->
                viewModel.addFollowUp(date, time, notes)
                showAddFollowUpDialog = false
            }
        )
    }

    // Reschedule Dialog
    activeRescheduleFollowUp?.let { fu ->
        RescheduleDialog(
            initialDateMillis = fu.followUpDate,
            onDismiss = { activeRescheduleFollowUp = null },
            onConfirm = { newDate, newTime, reason ->
                viewModel.rescheduleFollowUp(fu.id, newDate, newTime, reason)
                activeRescheduleFollowUp = null
            }
        )
    }

    // Mark Completed Dialog
    activeCompleteFollowUp?.let { fu ->
        MarkCompletedDialog(
            customerName = customer?.name ?: "Customer",
            onDismiss = { activeCompleteFollowUp = null },
            onConfirm = { notes, _ ->
                viewModel.markFollowUpCompleted(fu.id, notes)
                activeCompleteFollowUp = null
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FollowUpTimelineCard(
    followUp: FollowUp,
    onComplete: () -> Unit,
    onReschedule: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateUtils.formatDate(followUp.followUpDate),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (!followUp.followUpTime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•  ${followUp.followUpTime}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                StatusBadge(status = followUp.status, dateMillis = followUp.followUpDate)
            }

            if (followUp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = followUp.notes, color = TextSecondary, fontSize = 13.sp)
            }

            if (followUp.completedAt != null && followUp.status == FollowUpStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Completed on: ${DateUtils.formatDate(followUp.completedAt)}",
                    color = StatusCompleted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Actions for Pending follow-ups
            if (followUp.status == FollowUpStatus.PENDING) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReschedule,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Reschedule", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFollowUpDialog(
    onDismiss: () -> Unit,
    onConfirm: (dateMillis: Long, time: String?, notes: String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(DateUtils.getStartOfDay()) }
    var selectedTime by remember { mutableStateOf("11:00 AM") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Follow-up", color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Date: ${DateUtils.formatDate(selectedDate)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Time") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getDarkTextFieldColors(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("e.g. Discuss revised quote, site visit arrangement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getDarkTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, selectedTime.takeIf { it.isNotBlank() }, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Schedule", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDate
        val context = LocalContext.current
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, day, 0, 0, 0)
                selectedDate = newCal.timeInMillis
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }
}
