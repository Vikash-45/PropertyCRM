package com.propertycrm.app.ui.customer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.propertycrm.app.ai.DocumentImportResult
import com.propertycrm.app.ai.SmartDocumentImporter
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.data.model.RequirementType
import com.propertycrm.app.ui.components.DocumentImportDialog
import com.propertycrm.app.ui.components.DoNotCallBadge
import com.propertycrm.app.ui.components.EmptyStateCard
import com.propertycrm.app.ui.components.PropertyTypeBadge
import com.propertycrm.app.ui.components.RequirementTypeChip
import com.propertycrm.app.ui.components.StageChip
import com.propertycrm.app.ui.components.WhatsAppTemplateDialog
import kotlinx.coroutines.launch
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkBorder
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusCompleted
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.ui.theme.getDarkTextFieldColors
import com.propertycrm.app.util.DateUtils
import com.propertycrm.app.util.PhoneUtils

@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel,
    smartDocumentImporter: SmartDocumentImporter? = null,
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToCustomerDetail: (Long) -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val selectedPropertyType by viewModel.selectedPropertyType.collectAsState()
    val selectedRequirementType by viewModel.selectedRequirementType.collectAsState()
    val selectedStage by viewModel.selectedStage.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var customerForWhatsAppTemplates by remember { mutableStateOf<Customer?>(null) }

    var importResultDialog by remember { mutableStateOf<DocumentImportResult?>(null) }
    var isDocumentParsing by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && smartDocumentImporter != null) {
            coroutineScope.launch {
                isDocumentParsing = true
                Toast.makeText(context, "Scanning document for client leads...", Toast.LENGTH_SHORT).show()
                val result = smartDocumentImporter.parseDocument(uri)
                isDocumentParsing = false
                if (result.isSuccess) {
                    val importRes = result.getOrNull()
                    if (importRes != null && importRes.candidates.isNotEmpty()) {
                        importResultDialog = importRes
                    } else {
                        Toast.makeText(context, "No leads detected in document.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Document parse error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCustomer,
                containerColor = PrimaryBlue,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.add_customer), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.customers),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stats.totalActive} Total Clients Registered",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Import Document / Leads button (PDF, text, JSON, CSV, Image)
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = !isDocumentParsing,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isDocumentParsing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PrimaryBlue, strokeWidth = 1.5.dp)
                        } else {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Import Leads", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (viewModel.hasActiveFilters()) {
                        Button(
                            onClick = { viewModel.clearFilters() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = StatusOverdue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Reset Filters", color = StatusOverdue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Metrics Summary Cards Row (Total, Buy, Sell, Rent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatCard(
                    title = "Buy",
                    count = stats.buyCount,
                    isSelected = selectedRequirementType == RequirementType.BUY,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setSelectedRequirementType(
                            if (selectedRequirementType == RequirementType.BUY) null else RequirementType.BUY
                        )
                    }
                )
                SummaryStatCard(
                    title = "Sell",
                    count = stats.sellCount,
                    isSelected = selectedRequirementType == RequirementType.SELL,
                    accentColor = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setSelectedRequirementType(
                            if (selectedRequirementType == RequirementType.SELL) null else RequirementType.SELL
                        )
                    }
                )
                SummaryStatCard(
                    title = "Rent",
                    count = stats.rentCount,
                    isSelected = selectedRequirementType == RequirementType.RENT,
                    accentColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.setSelectedRequirementType(
                            if (selectedRequirementType == RequirementType.RENT) null else RequirementType.RENT
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search by name, phone, location, budget...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Property Category / Type Filter Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All Categories" chip
                FilterChip(
                    selected = selectedPropertyType == null,
                    onClick = { viewModel.setSelectedPropertyType(null) },
                    label = { Text("All Categories (${stats.totalActive})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    )
                )

                // Category types: Plot, Flat, House, Land, Commercial, Other
                PropertyType.values().forEach { type ->
                    val isSelected = selectedPropertyType == type
                    val count = stats.categoryCounts[type] ?: 0
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setSelectedPropertyType(if (isSelected) null else type)
                        },
                        label = {
                            Text(
                                text = "${type.getDisplayName()} ($count)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Customer Stage / Status Filter Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Stage:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                FilterChip(
                    selected = selectedStage == null,
                    onClick = { viewModel.setSelectedStage(null) },
                    label = { Text("All Stages", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    )
                )

                CustomerStage.values().forEach { stage ->
                    val isSelected = selectedStage == stage
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setSelectedStage(if (isSelected) null else stage)
                        },
                        label = { Text(stage.getDisplayName(), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tabs: Active Customers vs Archived
            TabRow(
                selectedTabIndex = if (showArchived) 1 else 0,
                containerColor = DarkBackground,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (showArchived) 1 else 0]),
                        color = PrimaryBlue
                    )
                }
            ) {
                Tab(
                    selected = !showArchived,
                    onClick = { viewModel.setShowArchived(false) },
                    text = { Text("Active Customers (${customers.size})", color = if (!showArchived) PrimaryBlue else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = showArchived,
                    onClick = { viewModel.setShowArchived(true) },
                    text = { Text("Archived", color = if (showArchived) PrimaryBlue else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (customers.isEmpty()) {
                EmptyStateCard(
                    title = if (viewModel.hasActiveFilters()) "No Matching Customers" else "No Customers Yet",
                    message = if (viewModel.hasActiveFilters()) "Try clearing your filters or search keywords." else "Start by registering your first property client.",
                    icon = Icons.Default.People,
                    actionLabel = if (!viewModel.hasActiveFilters()) stringResource(R.string.add_customer) else "Reset Filters",
                    onActionClick = {
                        if (viewModel.hasActiveFilters()) viewModel.clearFilters()
                        else onNavigateToAddCustomer()
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerListItemCard(
                            customer = customer,
                            onCallClick = { PhoneUtils.dialCustomer(context, customer.phone) },
                            onWhatsAppClick = { PhoneUtils.openWhatsApp(context, customer.phone) },
                            onTemplateClick = { customerForWhatsAppTemplates = customer },
                            onClick = { onNavigateToCustomerDetail(customer.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Quick WhatsApp Message Templates Dialog
    customerForWhatsAppTemplates?.let { customer ->
        WhatsAppTemplateDialog(
            customer = customer,
            onDismiss = { customerForWhatsAppTemplates = null },
            onSend = { customMessage ->
                customerForWhatsAppTemplates = null
                PhoneUtils.openWhatsApp(context, customer.phone, customMessage)
            }
        )
    }

    // Smart Document Import Leads Preview Dialog
    importResultDialog?.let { result ->
        DocumentImportDialog(
            importResult = result,
            onDismiss = { importResultDialog = null },
            onConfirmImport = { candidates ->
                importResultDialog = null
                if (smartDocumentImporter != null) {
                    coroutineScope.launch {
                        val count = smartDocumentImporter.commitCandidatesToDatabase(candidates, result.sourceName)
                        Toast.makeText(context, "Successfully imported $count client leads!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CustomerListItemCard(
    customer: Customer,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onTemplateClick: () -> Unit,
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
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Badges (Requirement Type + Property Category + Stage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RequirementTypeChip(requirementType = customer.requirementType)
                    PropertyTypeBadge(propertyType = customer.propertyType)
                    if (customer.isDoNotCall) {
                        DoNotCallBadge()
                    }
                }
                StageChip(stage = customer.stage)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name & Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Budget Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customer.preferredLocation.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = customer.preferredLocation,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                if (!customer.budget.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF352B10))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "₹${customer.budget}",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!customer.leadSource.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = customer.leadSource,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Inquiry Need / Requirement Note
            if (customer.inquiryReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = customer.inquiryReason,
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions Bar: Added Date + Quick Communication Suite (Call, WhatsApp, Templates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added: ${DateUtils.formatDate(customer.createdAt)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick WhatsApp Direct Button
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF25D366))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "WhatsApp",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Quick WhatsApp Templates Button
                    IconButton(
                        onClick = onTemplateClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp Templates",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Direct Phone Call Button
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
