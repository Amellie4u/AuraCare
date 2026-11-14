package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SeizureLog
import com.example.reports.ClinicalPdfGenerator
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveSeizureTimerCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateTime
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.viewmodel.SeizureTimerState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeizuresScreen(
    seizures: List<SeizureLog>,
    timerState: SeizureTimerState,
    onStartTimer: () -> Unit,
    onStopAndLogTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onOpenEmergencyDialog: () -> Unit,
    onAddSeizureClick: () -> Unit,
    onEditSeizureClick: (SeizureLog) -> Unit,
    onDeleteSeizureClick: (String) -> Unit,
    onOpenReportDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                LiveSeizureTimerCard(
                    timerState = timerState,
                    onStart = onStartTimer,
                    onStopAndLog = onStopAndLogTimer,
                    onReset = onResetTimer,
                    onEmergencyClick = onOpenEmergencyDialog
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddSeizureClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_seizure_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Seizure Details")
                }
                OutlinedButton(
                    onClick = onOpenReportDialog,
                    modifier = Modifier.testTag("seizures_export_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF / Email")
                }
            }
        }

        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                SectionHeader(
                    title = "Recorded Events (${seizures.size})",
                    subtitle = "Duration, triggers, medication side effects & post-ictal notes"
                )
            }
        }

        if (seizures.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.Timer,
                        title = "No seizures recorded yet",
                        description = "Use the live Seizure Timer above or tap Log Seizure Details to capture duration, triggers, and side effects for your medical review.",
                        buttonLabel = "Log First Event",
                        onButtonClick = onAddSeizureClick
                    )
                }
            }
        } else {
            items(seizures, key = { it.id }) { seizure ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = seizure.seizureType,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = formatDateTime(seizure.occurredAtMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${ClinicalPdfGenerator.formatDuration(seizure.durationSeconds)} · Sev ${seizure.severity}/5",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = RobotoMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(onClick = { onEditSeizureClick(seizure) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit seizure")
                                }
                                IconButton(onClick = { onDeleteSeizureClick(seizure.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete seizure", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (seizure.triggers.isNotEmpty()) {
                            Text("Triggers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                seizure.triggers.forEach { trigger ->
                                    AssistChip(onClick = {}, label = { Text(trigger) })
                                }
                            }
                        }

                        if (seizure.medicationSideEffects.isNotEmpty()) {
                            Text("Medication Side Effects", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                seizure.medicationSideEffects.forEach { eff ->
                                    AssistChip(onClick = {}, label = { Text(eff) })
                                }
                            }
                        }

                        if (seizure.symptomsAndAura.isNotBlank()) {
                            Text(
                                text = "Aura / Symptoms: ${seizure.symptomsAndAura}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (seizure.postIctalNotes.isNotBlank()) {
                            Text(
                                text = "Post-Ictal Notes: ${seizure.postIctalNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
