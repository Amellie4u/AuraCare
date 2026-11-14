package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyLog
import com.example.data.model.DoseLog
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.example.reports.ClinicalPdfGenerator
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LiveSeizureTimerCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateTime
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.viewmodel.SeizureTimerState
import com.example.util.CareIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    profile: PatientProfile?,
    seizures: List<SeizureLog>,
    medications: List<MedicationEntry>,
    doseLogs: List<DoseLog>,
    dailyLogs: List<DailyLog>,
    appointments: List<MedicalAppointment>,
    records: List<MedicalRecord>,
    timerState: SeizureTimerState,
    onStartTimer: () -> Unit,
    onStopAndLogTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onOpenEmergencyDialog: () -> Unit,
    onOpenSeizureEditor: () -> Unit,
    onOpenReportDialog: () -> Unit,
    onToggleDose: (MedicationEntry, String, Boolean) -> Unit,
    onMarkAppointmentLinked: (String) -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val currentMeds = remember(medications) { medications.filter { it.status == "current" } }
    val upcomingAppointment = remember(appointments) {
        appointments.firstOrNull { it.appointmentMillis >= System.currentTimeMillis() - 3600_000L }
            ?: appointments.lastOrNull()
    }
    val topTriggers = remember(seizures) {
        seizures.flatMap { it.triggers }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(3)
    }
    val topSideEffects = remember(seizures, medications) {
        (seizures.flatMap { it.medicationSideEffects } + medications.flatMap { it.sideEffectsNoted })
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                Text(
                    text = "Welcome back, ${profile?.fullName?.ifBlank { "Care Member" } ?: "Care Member"}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = buildString {
                        append(profile?.epilepsyType?.ifBlank { "Personal Seizure & Wellness Care" } ?: "Personal Seizure & Wellness Care")
                        if (!profile?.dietaryPreference.isNullOrBlank()) {
                            append(" · ${profile?.dietaryPreference} plan")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
            Column(
                modifier = Modifier.widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "Quick Actions")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.Add,
                        label = "Log Seizure",
                        onClick = onOpenSeizureEditor,
                        modifier = Modifier.testTag("quick_log_seizure")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.PictureAsPdf,
                        label = "PDF & Email",
                        onClick = onOpenReportDialog,
                        modifier = Modifier.testTag("quick_pdf_report")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Restaurant,
                        label = "Food & Recipes",
                        onClick = { onNavigateToTab("daily") }
                    )
                    QuickActionTile(
                        icon = Icons.Filled.FolderShared,
                        label = "Records (${records.size})",
                        onClick = { onNavigateToTab("care") }
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(
                    title = "Today's Scheduled Doses",
                    actionLabel = "All Meds",
                    onActionClick = { onNavigateToTab("meds") }
                )
                val scheduledPairs = currentMeds.flatMap { med ->
                    med.scheduleTimes.map { time -> med to time }
                }.sortedBy { it.second }

                if (scheduledPairs.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Filled.Medication,
                        title = "No active medication schedule",
                        description = "Add your current medications and scheduled dose times to enable reminders.",
                        buttonLabel = "Add Medication",
                        onButtonClick = { onNavigateToTab("meds") }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            scheduledPairs.forEach { (med, time) ->
                                val taken = doseLogs.any {
                                    it.medicationId == med.id &&
                                        it.dateKey == todayKey &&
                                        it.scheduledTime == time &&
                                        it.taken
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleDose(med, time, !taken) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (taken) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = if (taken) "Taken" else "Pending",
                                            tint = if (taken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "${med.name} · ${med.dosage}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = if (med.reminderEnabled) "Alert at $time" else "Scheduled $time",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (taken) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (taken) "Taken" else "Mark",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(
                    title = "Medical Overview",
                    actionLabel = "Seizures",
                    onActionClick = { onNavigateToTab("seizures") }
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val avgDuration = if (seizures.isNotEmpty()) {
                            ClinicalPdfGenerator.formatDuration(seizures.map { it.durationSeconds }.average().toLong())
                        } else "—"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricTile("Logged Seizures", seizures.size.toString(), Modifier.weight(1f))
                            MetricTile("Avg Duration", avgDuration, Modifier.weight(1f))
                            MetricTile("Daily Logs", dailyLogs.size.toString(), Modifier.weight(1f))
                        }
                        if (topTriggers.isNotEmpty()) {
                            Text("Frequent Triggers", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                topTriggers.forEach { (trigger, count) ->
                                    AssistChip(onClick = {}, label = { Text("$trigger ($count)") })
                                }
                            }
                        }
                        if (topSideEffects.isNotEmpty()) {
                            Text("Reported Side Effects", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                topSideEffects.forEach { (eff, count) ->
                                    AssistChip(onClick = {}, label = { Text("$eff ($count)") })
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(
                    title = "Next Appointment",
                    actionLabel = "Manage",
                    onActionClick = { onNavigateToTab("care") }
                )
                if (upcomingAppointment == null) {
                    EmptyStateCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = "No appointments scheduled",
                        description = "Add your neurology or lab visits and link them directly to any calendar app.",
                        buttonLabel = "Add Appointment",
                        onButtonClick = { onNavigateToTab("care") }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(upcomingAppointment.title, style = MaterialTheme.typography.titleMedium)
                            Text(formatDateTime(upcomingAppointment.appointmentMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            if (upcomingAppointment.doctorName.isNotBlank() || upcomingAppointment.clinicLocation.isNotBlank()) {
                                Text(
                                    listOfNotNull(
                                        upcomingAppointment.doctorName.takeIf { it.isNotBlank() },
                                        upcomingAppointment.clinicLocation.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = {
                                    if (CareIntents.openCalendarForAppointment(context, upcomingAppointment)) {
                                        onMarkAppointmentLinked(upcomingAppointment.id)
                                    }
                                }) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (upcomingAppointment.linkedToCalendar) "Linked to Calendar" else "Link to Calendar")
                                }
                                OutlinedButton(onClick = onOpenReportDialog) {
                                    Text("Email Summary")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = RobotoMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
