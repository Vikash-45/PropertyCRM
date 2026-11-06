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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propertycrm.app.R
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.PropertyType
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkBorder
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.StatusUpcoming
import com.propertycrm.app.ui.theme.StatusUpcomingBg
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditCustomerScreen(
    viewModel: AddEditCustomerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showFirstVisitDatePicker by remember { mutableStateOf(false) }
    var showFollowUpDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AddEditEvent.Saved -> onNavigateBack()
                is AddEditEvent.Error -> { /* Handled via validationError state */ }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) stringResource(R.string.edit_customer) else stringResource(R.string.add_new_customer),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveCustomer() },
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.save), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Validation error banner
            if (uiState.validationError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusOverdue.copy(alpha = 0.15f))
                        .border(1.dp, StatusOverdue, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(text = uiState.validationError!!, color = StatusOverdue, fontSize = 13.sp)
                }
            }

            // Duplicate phone warning banner (Requirement: Warn about duplicate phone without blocking)
            if (uiState.isDuplicatePhone) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusUpcomingBg)
                        .border(1.dp, StatusUpcoming, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusUpcoming)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.duplicate_phone_warning),
                            color = StatusUpcoming,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Customer Name (Required)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text(stringResource(R.string.customer_name)) },
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors(),
                singleLine = true
            )

            // Phone Number (Required)
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { viewModel.onPhoneChange(it) },
                label = { Text(stringResource(R.string.customer_phone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors(),
                singleLine = true
            )

            // Inquiry Reason (Required)
            OutlinedTextField(
                value = uiState.inquiryReason,
                onValueChange = { viewModel.onInquiryReasonChange(it) },
                label = { Text(stringResource(R.string.inquiry_reason)) },
                placeholder = { Text("e.g. Looking for 3BHK flat near Metro") },
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors()
            )

            // Property Type
            Text(
                text = stringResource(R.string.property_type),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PropertyType.values().forEach { type ->
                    val selected = uiState.propertyType == type
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onPropertyTypeChange(type) },
                        label = { Text(type.getDisplayName()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Customer Stage
            Text(
                text = stringResource(R.string.stage),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerStage.values().forEach { stg ->
                    val selected = uiState.stage == stg
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onStageChange(stg) },
                        label = { Text(stg.getDisplayName()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Preferred Location & Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.preferredLocation,
                    onValueChange = { viewModel.onPreferredLocationChange(it) },
                    label = { Text(stringResource(R.string.preferred_location)) },
                    modifier = Modifier.weight(1f),
                    colors = getDarkTextFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.budget,
                    onValueChange = { viewModel.onBudgetChange(it) },
                    label = { Text(stringResource(R.string.budget)) },
                    placeholder = { Text("e.g. 50L - 70L") },
                    modifier = Modifier.weight(1f),
                    colors = getDarkTextFieldColors(),
                    singleLine = true
                )
            }

            // Lead Source
            OutlinedTextField(
                value = uiState.leadSource,
                onValueChange = { viewModel.onLeadSourceChange(it) },
                label = { Text(stringResource(R.string.lead_source)) },
                placeholder = { Text("e.g. Walk-in, 99acres, Referral") },
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors(),
                singleLine = true
            )

            // First Visit Date
            OutlinedButton(
                onClick = { showFirstVisitDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.firstVisitDate != null) {
                        "First Visit: ${DateUtils.formatDate(uiState.firstVisitDate)}"
                    } else {
                        "Set First Visit Date (Optional)"
                    }
                )
            }

            // Initial Follow-up (Creation Mode only)
            if (!uiState.isEditMode) {
                Text(
                    text = "Scheduled Follow-up",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFollowUpDatePicker = true },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatDate(uiState.initialFollowUpDate),
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = uiState.initialFollowUpTime,
                        onValueChange = { viewModel.onInitialFollowUpTimeChange(it) },
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f),
                        colors = getDarkTextFieldColors(),
                        singleLine = true
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text("Additional customer preferences, requirements...") },
                modifier = Modifier.fillMaxWidth(),
                colors = getDarkTextFieldColors(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // First Visit Date Picker
    if (showFirstVisitDatePicker) {
        val cal = Calendar.getInstance()
        if (uiState.firstVisitDate != null) cal.timeInMillis = uiState.firstVisitDate!!
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, day, 0, 0, 0)
                viewModel.onFirstVisitDateChange(newCal.timeInMillis)
                showFirstVisitDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showFirstVisitDatePicker = false }
            show()
        }
    }

    // Follow-up Date Picker
    if (showFollowUpDatePicker) {
        val cal = Calendar.getInstance()
        if (uiState.initialFollowUpDate != null) cal.timeInMillis = uiState.initialFollowUpDate!!
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, day, 0, 0, 0)
                viewModel.onInitialFollowUpDateChange(newCal.timeInMillis)
                showFollowUpDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showFollowUpDatePicker = false }
            show()
        }
    }
}

@Composable
fun getDarkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    cursorColor = PrimaryBlue
)
