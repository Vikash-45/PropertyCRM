package com.propertycrm.app.ui.customer

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.propertycrm.app.ai.CallRecordingManager
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.CallOutcome
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.notification.ReminderScheduler
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.propertycrm.app.ui.components.WhatsAppTemplateDialog
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
    callRecordingManager: CallRecordingManager? = null,
    reminderScheduler: ReminderScheduler? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEditCustomer: (Long) -> Unit
) {
    val data by viewModel.customerData.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val siteVisits by viewModel.siteVisits.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Timeline, 1: Follow-ups, 2: Site Visits
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDncConfirm by remember { mutableStateOf(false) }
    var showCallOutcomeDialog by remember { mutableStateOf(false) }
    var showAddFollowUpDialog by remember { mutableStateOf(false) }
    var showScheduleVisitDialog by remember { mutableStateOf(false) }
    var showWhatsAppTemplates by remember { mutableStateOf(false) }
    var showAudioSummarizerDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Secondary Communication Row: WhatsApp & SMS & Templates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { PhoneUtils.openWhatsApp(context, customer.phone) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showWhatsAppTemplates = true },
                                    modifier = Modifier.weight(1.1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Templates", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { PhoneUtils.sendSms(context, customer.phone) },
                                    modifier = Modifier.weight(0.9f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // AI Audio & Call Summarizer Button + Visit Alert Customization
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAudioSummarizerDialog = true },
                                    modifier = Modifier.weight(1.3f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Record / AI Call Summary", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showNotificationSettingsDialog = true },
                                    modifier = Modifier.weight(0.9f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = if (customer.notificationsEnabled) PrimaryBlue else TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alert (${customer.reminderLeadTimeMinutes}m)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                            TabRowDefaults.SecondaryIndicator(
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
                viewModel.scheduleSiteVisit(loc, date, time, notes) { visit ->
                    if (reminderScheduler != null && customer != null && customer.notificationsEnabled) {
                        reminderScheduler.schedulePriorSiteVisitAlert(customer, visit)
                    }
                }
                showScheduleVisitDialog = false
            }
        )
    }

    // AI Audio & Call Summarizer Dialog
    if (showAudioSummarizerDialog && customer != null) {
        AudioCallSummarizerDialog(
            customer = customer,
            callRecordingManager = callRecordingManager,
            onDismiss = { showAudioSummarizerDialog = false },
            onSummarySaved = { notes, audioFile, summaryText ->
                viewModel.saveAiCallSummary(notes, audioFile?.absolutePath, summaryText)
                showAudioSummarizerDialog = false
                Toast.makeText(context, "AI Call Summary added to customer timeline!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Customer Prior Notification Settings Dialog
    if (showNotificationSettingsDialog && customer != null) {
        CustomerNotificationSettingsDialog(
            customer = customer,
            onDismiss = { showNotificationSettingsDialog = false },
            onSave = { leadTime, enabled, notes ->
                viewModel.updateNotificationSettings(leadTime, enabled, notes)
                // Reschedule pending site visits for this customer
                if (reminderScheduler != null) {
                    siteVisits.filter { it.status == AppointmentStatus.SCHEDULED }.forEach { visit ->
                        if (enabled) {
                            reminderScheduler.schedulePriorSiteVisitAlert(
                                customer.copy(reminderLeadTimeMinutes = leadTime, notificationsEnabled = enabled, customNotificationNotes = notes),
                                visit
                            )
                        } else {
                            reminderScheduler.cancelSiteVisitPriorAlert(visit.id)
                        }
                    }
                }
                showNotificationSettingsDialog = false
                Toast.makeText(context, "Notification preferences updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // WhatsApp Message Templates Dialog
    if (showWhatsAppTemplates && customer != null) {
        WhatsAppTemplateDialog(
            customer = customer,
            onDismiss = { showWhatsAppTemplates = false },
            onSend = { customMessage ->
                showWhatsAppTemplates = false
                PhoneUtils.openWhatsApp(context, customer.phone, customMessage)
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
                StatusBadge(status = followUp.status, dateMillis = followUp.followUpDate)
                Text(
                    text = "${DateUtils.formatDate(followUp.followUpDate)}${if (!followUp.followUpTime.isNullOrBlank()) " at ${followUp.followUpTime}" else ""}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (followUp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = followUp.notes, color = TextPrimary, fontSize = 13.sp)
            }

            if (followUp.status == FollowUpStatus.PENDING) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReschedule) {
                        Text(text = "Reschedule", color = PrimaryBlue, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Complete", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionItemCard(interaction: CustomerInteraction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (interaction.type == InteractionType.AI_CALL_SUMMARY) Color(0xFF132238) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (interaction.type == InteractionType.AI_CALL_SUMMARY) PrimaryBlue.copy(alpha = 0.5f) else DarkBorder
            )
        )
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
                            InteractionType.AI_CALL_SUMMARY -> PrimaryBlue.copy(alpha = 0.25f)
                            InteractionType.DOCUMENT_IMPORT -> Color(0xFFF59E0B).copy(alpha = 0.2f)
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
                        InteractionType.AI_CALL_SUMMARY -> Icons.Default.AutoAwesome
                        InteractionType.DOCUMENT_IMPORT -> Icons.Default.Edit
                        InteractionType.CALL -> Icons.Default.Call
                        InteractionType.SITE_VISIT -> Icons.Default.EventAvailable
                        InteractionType.DO_NOT_CALL_TOGGLED -> Icons.Default.PhoneDisabled
                        else -> Icons.Default.History
                    },
                    contentDescription = null,
                    tint = when (interaction.type) {
                        InteractionType.AI_CALL_SUMMARY -> PrimaryBlue
                        InteractionType.DOCUMENT_IMPORT -> Color(0xFFF59E0B)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = interaction.type.getDisplayName() + if (interaction.callOutcome != null) " (${interaction.callOutcome.getDisplayName()})" else "",
                            fontWeight = FontWeight.Bold,
                            color = if (interaction.type == InteractionType.AI_CALL_SUMMARY) PrimaryBlue else TextPrimary,
                            fontSize = 13.sp
                        )
                        if (interaction.audioFilePath != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Audio Recorded", tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        }
                    }
                    Text(
                        text = DateUtils.formatDate(interaction.timestamp),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (interaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = interaction.notes,
                        color = if (interaction.type == InteractionType.AI_CALL_SUMMARY) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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

@Composable
fun AudioCallSummarizerDialog(
    customer: com.propertycrm.app.data.model.Customer,
    callRecordingManager: CallRecordingManager?,
    onDismiss: () -> Unit,
    onSummarySaved: (notes: String, audioFile: File?, summaryText: String?) -> Unit
) {
    var isRecording by remember { mutableStateOf(callRecordingManager?.isRecording() == true) }
    var notesText by remember { mutableStateOf("") }
    var isSummarizing by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (isRecording) {
                audioFile = callRecordingManager?.stopRecording()
                isRecording = false
            }
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) StatusOverdue.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecording) StatusOverdue else PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isRecording) "Recording Audio..." else "Call & Voice Note",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${customer.name} • ${customer.phone}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Audio recording control pill
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isRecording) "Recording in progress" else if (audioFile != null) "Audio Captured" else "Audio Capture",
                                color = if (isRecording) StatusOverdue else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isRecording) "Tap to finish recording" else if (audioFile != null) audioFile!!.name else "Optional microphone audio record",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (isRecording) {
                                    audioFile = callRecordingManager?.stopRecording()
                                    isRecording = false
                                    Toast.makeText(context, "Audio recorded successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val started = callRecordingManager?.startRecording(customer.id, customer.name, customer.phone) ?: false
                                    if (started) isRecording = true
                                    else Toast.makeText(context, "Failed to initialize microphone.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) StatusOverdue else PrimaryBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecording) "Stop" else "Record", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Call Notes / Transcript / Key Points", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Client requirements, budget discussions, floor preference, objections...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = getDarkTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isRecording) {
                        audioFile = callRecordingManager?.stopRecording()
                        isRecording = false
                    }
                    if (callRecordingManager != null) {
                        coroutineScope.launch {
                            isSummarizing = true
                            val result = callRecordingManager.autoSummarizeAndImport(
                                customerId = customer.id,
                                customerName = customer.name,
                                customerPhone = customer.phone,
                                audioFile = audioFile,
                                callNotesOrTranscript = notesText
                            )
                            isSummarizing = false
                            if (result.isSuccess) {
                                val item = result.getOrNull()
                                onSummarySaved(item?.notes ?: notesText, audioFile, item?.aiSummary)
                            } else {
                                onSummarySaved(notesText, audioFile, null)
                            }
                        }
                    } else {
                        onSummarySaved(notesText, audioFile, null)
                    }
                },
                enabled = !isSummarizing && (notesText.isNotBlank() || audioFile != null || isRecording),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isSummarizing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Summarizing...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Summarize & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecording) {
                    callRecordingManager?.stopRecording()
                    isRecording = false
                }
                onDismiss()
            }) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CustomerNotificationSettingsDialog(
    customer: com.propertycrm.app.data.model.Customer,
    onDismiss: () -> Unit,
    onSave: (leadTimeMinutes: Int, enabled: Boolean, notes: String?) -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(customer.notificationsEnabled) }
    var selectedLeadTime by remember { mutableStateOf(customer.reminderLeadTimeMinutes) }
    var customNotes by remember { mutableStateOf(customer.customNotificationNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Prior Visit Notifications",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Per-customer alert preferences",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Enable toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Enable Site Visit Alerts", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryBlue)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Alert Lead Time Prior to Visit", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 1440 to "1d").forEach { (mins, label) ->
                        val isSel = selectedLeadTime == mins
                        Button(
                            onClick = { selectedLeadTime = mins },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) PrimaryBlue else DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Custom Reminder Notes", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    placeholder = { Text("e.g. Bring key for top floor, verify buyer ID proof") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getDarkTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedLeadTime, notificationsEnabled, customNotes.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Preferences", color = Color.Black, fontWeight = FontWeight.Bold)
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
}
