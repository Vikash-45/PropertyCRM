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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhoneForwarded
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.ui.components.AddFollowUpDialog
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditCustomer: (Long) -> Unit
) {
    val data by viewModel.customerData.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val siteVisits by viewModel.siteVisits.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Timeline, 1: Follow-ups, 2: Site Visits
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDncConfirm by remember { mutableStateOf(false) }
    var showCallOutcomeDialog by remember { mutableStateOf(false) }
    var showAddFollowUpDialog by remember { mutableStateOf(false) }
    var showScheduleVisitDialog by remember { mutableStateOf(false) }
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
                        IconButton(onClick = {
                            if (customer.isDoNotCall) {
                                showDncConfirm = true
                            } else {
                                PhoneUtils.dialCustomer(context, customer.phone)
                                showCallOutcomeDialog = true
                            }
                        }) {
                            Icon(
                                imageVector = if (customer.isDoNotCall) Icons.Default.PhoneDisabled else Icons.Default.Call,
                                contentDescription = "Call",
                                tint = if (customer.isDoNotCall) StatusOverdue else PrimaryBlue
                            )
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
                                text = { Text(if (customer.isDoNotCall) "Remove DNC (Enable Calls)" else "Mark Do Not Call (DNC)", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (customer.isDoNotCall) Icons.Default.Call else Icons.Default.PhoneDisabled,
                                        contentDescription = null,
                                        tint = if (customer.isDoNotCall) StatusCompleted else StatusOverdue
                                    )
                                },
                                onClick = {
                                    viewModel.toggleDoNotCall()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (customer.isArchived) "Unarchive Customer" else "Archive Customer", color = TextPrimary) },
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (customer.isDoNotCall) DoNotCallBadge()
                                    StageChip(stage = customer.stage)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Property & Deal Details
                            DetailRow("Requirement", "${customer.requirementType.getDisplayName()} ${customer.propertyType.getDisplayName()}")
                            DetailRow("Inquiry Need", customer.inquiryReason)
                            if (customer.preferredLocation.isNotBlank()) DetailRow("Location", customer.preferredLocation)
                            if (!customer.preferredSize.isNullOrBlank()) DetailRow("Size", customer.preferredSize)
                            if (!customer.budget.isNullOrBlank()) DetailRow("Budget", "₹${customer.budget}")
                            if (!customer.preferredContactTime.isNullOrBlank()) DetailRow("Contact Time", customer.preferredContactTime)
                            DetailRow("Next Action", customer.nextAction.getDisplayName())
                            if (!customer.assignedPerson.isNullOrBlank()) DetailRow("Assigned Agent", customer.assignedPerson)
                            if (!customer.leadSource.isNullOrBlank()) DetailRow("Lead Source", customer.leadSource)
                            if (customer.firstVisitDate != null) DetailRow("First Visit", DateUtils.formatDate(customer.firstVisitDate))
                            if (customer.notes.isNotBlank()) DetailRow("Notes", customer.notes)
                            DetailRow("Registered", DateUtils.formatDate(customer.createdAt))

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (customer.isDoNotCall) showDncConfirm = true
                                        else {
                                            PhoneUtils.dialCustomer(context, customer.phone)
                                            showCallOutcomeDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (customer.isDoNotCall) StatusOverdue else PrimaryBlue
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (customer.isDoNotCall) "DNC" else "Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { showCallOutcomeDialog = true },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log Call", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { showScheduleVisitDialog = true },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Visit", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Stage Selector Chips
                item {
                    Text(
                        text = "Update Customer Stage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CustomerStage.values().forEach { stg ->
                            val isSelected = customer.stage == stg
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateStage(stg) },
                                label = { Text(stg.getDisplayName(), fontSize = 12.sp) },
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

                // Tabs: Timeline / Follow-ups / Site Visits
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkBackground,
                        contentColor = PrimaryBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = PrimaryBlue
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Timeline (${interactions.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Follow-ups (${followUps.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Site Visits (${siteVisits.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Chronological Interaction Timeline (Item 3)
                        if (interactions.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Interaction History",
                                    message = "Calls, notes, site visits, and stage updates will appear here chronologically.",
                                    icon = Icons.Default.History
                                )
                            }
                        } else {
                            items(interactions) { interaction ->
                                InteractionItemCard(interaction = interaction)
                            }
                        }
                    }
                    1 -> {
                        // Follow-up Records
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showAddFollowUpDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ New Follow-up", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        if (followUps.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = stringResource(R.string.no_followup_history),
                                    message = "No follow-up calls scheduled.",
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
                    }
                    2 -> {
                        // Site Visits (Item 1)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showScheduleVisitDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Schedule Visit", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        if (siteVisits.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No Site Visits Scheduled",
                                    message = "Record appointments for property inspections and site visits here.",
                                    icon = Icons.Default.EventAvailable,
                                    actionLabel = "Schedule Visit",
                                    onActionClick = { showScheduleVisitDialog = true }
                                )
                            }
                        } else {
                            items(siteVisits) { visit ->
                                SiteVisitCard(
                                    visit = visit,
                                    onStatusChange = { newStatus -> viewModel.updateSiteVisitStatus(visit.id, newStatus) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // Call Outcome Dialog
    if (showCallOutcomeDialog) {
        CallOutcomeDialog(
            customerName = customer?.name ?: "Customer",
            onDismiss = { showCallOutcomeDialog = false },
            onConfirm = { outcome, notes, nextDate, nextTime ->
                viewModel.recordCallOutcome(outcome, notes, nextDate, nextTime)
                showCallOutcomeDialog = false
            }
        )
    }

    // Schedule Site Visit Dialog
    if (showScheduleVisitDialog) {
        ScheduleSiteVisitDialog(
            defaultLocation = customer?.preferredLocation ?: "",
            onDismiss = { showScheduleVisitDialog = false },
            onConfirm = { loc, date, time, notes ->
                viewModel.scheduleSiteVisit(loc, date, time, notes)
                showScheduleVisitDialog = false
            }
        )
    }

    // DNC Confirmation Override
    if (showDncConfirm) {
        AlertDialog(
            onDismissRequest = { showDncConfirm = false },
            title = { Text("Customer on Do Not Call list", color = StatusOverdue) },
            text = { Text(stringResource(R.string.dnc_call_prompt), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        PhoneUtils.dialCustomer(context, customer!!.phone)
                        showDncConfirm = false
                        showCallOutcomeDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                ) {
                    Text("Call Anyway", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDncConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
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
fun InteractionItemCard(interaction: CustomerInteraction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (interaction.type) {
                            InteractionType.CALL -> PrimaryBlue.copy(alpha = 0.2f)
                            InteractionType.SITE_VISIT -> StatusUpcoming.copy(alpha = 0.2f)
                            InteractionType.DO_NOT_CALL_TOGGLED -> StatusOverdue.copy(alpha = 0.2f)
                            else -> DarkSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (interaction.type) {
                        InteractionType.CALL -> Icons.Default.Call
                        InteractionType.SITE_VISIT -> Icons.Default.EventAvailable
                        InteractionType.DO_NOT_CALL_TOGGLED -> Icons.Default.PhoneDisabled
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    tint = when (interaction.type) {
                        InteractionType.CALL -> PrimaryBlue
                        InteractionType.SITE_VISIT -> StatusUpcoming
                        InteractionType.DO_NOT_CALL_TOGGLED -> StatusOverdue
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = interaction.type.getDisplayName() + if (interaction.callOutcome != null) " (${interaction.callOutcome.getDisplayName()})" else "",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = DateUtils.formatDate(interaction.timestamp),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (interaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = interaction.notes, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SiteVisitCard(
    visit: SiteVisit,
    onStatusChange: (AppointmentStatus) -> Unit
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
                Text(
                    text = visit.propertyLocation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (visit.status) {
                                AppointmentStatus.COMPLETED -> StatusCompletedBg
                                AppointmentStatus.CANCELLED -> StatusOverdueBg
                                AppointmentStatus.RESCHEDULED -> DarkSurfaceVariant
                                AppointmentStatus.SCHEDULED -> StatusUpcomingBg
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = visit.status.getDisplayName(),
                        color = when (visit.status) {
                            AppointmentStatus.COMPLETED -> StatusCompleted
                            AppointmentStatus.CANCELLED -> StatusOverdue
                            AppointmentStatus.RESCHEDULED -> TextMuted
                            AppointmentStatus.SCHEDULED -> StatusUpcoming
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Date: ${DateUtils.formatDate(visit.visitDate)}", color = TextPrimary, fontSize = 13.sp)
                if (!visit.visitTime.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "• ${visit.visitTime}", color = TextSecondary, fontSize = 12.sp)
                }
            }

            if (visit.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = visit.notes, color = TextSecondary, fontSize = 12.sp)
            }

            if (visit.status == AppointmentStatus.SCHEDULED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onStatusChange(AppointmentStatus.CANCELLED) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onStatusChange(AppointmentStatus.COMPLETED) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)
                    ) {
                        Text("Completed", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSiteVisitDialog(
    defaultLocation: String,
    onDismiss: () -> Unit,
    onConfirm: (location: String, dateMillis: Long, time: String?, notes: String) -> Unit
) {
    var location by remember { mutableStateOf(defaultLocation) }
    var selectedDate by remember { mutableStateOf(DateUtils.getStartOfDay() + 86400000L) }
    var selectedTime by remember { mutableStateOf("04:00 PM") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Site Visit", color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Property / Site Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Visit Notes (Optional)") },
                    placeholder = { Text("e.g. Show 3BHK flat layout, meeting at site gate") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (location.isNotBlank()) {
                        onConfirm(location, selectedDate, selectedTime.takeIf { it.isNotBlank() }, notes)
                    }
                },
                enabled = location.isNotBlank(),
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
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val context = LocalContext.current
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
