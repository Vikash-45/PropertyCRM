package com.propertycrm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.propertycrm.app.ai.DocumentImportResult
import com.propertycrm.app.ai.ExtractedCustomerCandidate
import com.propertycrm.app.ui.theme.DarkBorder
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusCompleted
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary

@Composable
fun DocumentImportDialog(
    importResult: DocumentImportResult,
    onDismiss: () -> Unit,
    onConfirmImport: (candidates: List<ExtractedCustomerCandidate>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF13283E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (importResult.extractedUsingAi) Icons.Default.AutoAwesome else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (importResult.extractedUsingAi) PrimaryBlue else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Document Leads Detected",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = importResult.sourceName,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = importResult.infoMessage,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Extracted Customer Candidates (${importResult.candidates.size}):",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(importResult.candidates) { candidate ->
                        CandidateCard(candidate)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmImport(importResult.candidates) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Import All (${importResult.candidates.size})",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CandidateCard(candidate: ExtractedCustomerCandidate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B26)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RequirementTypeChip(requirementType = candidate.requirementType)
                    PropertyTypeBadge(propertyType = candidate.propertyType)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = candidate.phone,
                color = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (candidate.preferredLocation.isNotBlank() || !candidate.budget.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (candidate.preferredLocation.isNotBlank()) {
                        Text(
                            text = "📍 ${candidate.preferredLocation}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (!candidate.budget.isNullOrBlank()) {
                        Text(
                            text = "• ₹${candidate.budget}",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (candidate.inquiryReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = candidate.inquiryReason,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}
