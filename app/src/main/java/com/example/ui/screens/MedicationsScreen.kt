package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DoseLog
import com.example.data.model.MedicationEntry
import com.example.notifications.DoseReminderReceiver
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.RobotoMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationsScreen(
    medications: List<MedicationEntry>,
    doseLogs: List<DoseLog>,
    onAddMedication: (String) -> Unit,
    onEditMedication: (MedicationEntry) -> Unit,
    onToggleReminder: (String, Boolean) -> Unit,
    onArchiveToPast: (MedicationEntry, String) -> Unit,
    onDeleteMedication: (String) -> Unit,
    onToggleDose: (MedicationEntry, String, Boolean) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var showPast by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<MedicationEntry?>(null) }
    var archiveReason by remember { mutableStateOf("") }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNotificationAction?.invoke()
        } else {
            onFeedback("Notification permission denied. Enable notifications in settings to receive dose alerts.")
        }
        pendingNotificationAction = null
    }

    fun withNotificationPermission(action: () -> Unit) {
        val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPerm) {
            pendingNotificationAction = action
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val currentMeds = remember(medications) { medications.filter { it.status == "current" } }
    val pastMeds = remember(medications) { medications.filter { it.status == "past" } }
    val displayedMeds = if (showPast) pastMeds else currentMeds
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showPast,
                    onClick = { showPast = false },
                    label = { Text("Current (${currentMeds.size})") },
                    leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("meds_tab_current")
                )
                FilterChip(
                    selected = showPast,
                    onClick = { showPast = true },
                    label = { Text("Past History (${pastMeds.size})") },
                    leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("meds_tab_past")
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onAddMedication(if (showPast) "past" else "current") },
                    modifier = Modifier.testTag("add_medication_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        if (!showPast && currentMeds.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 840.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionHeader(
                        title = "Today's Dose Alerts & Checklist",
                        actionLabel = "Test Alert",
                        onActionClick = {
                            withNotificationPermission {
                                val firstMed = currentMeds.first()
                                DoseReminderReceiver.showDoseNotification(
                                    context = context,
                                    medName = firstMed.name,
                                    dosage = firstMed.dosage,
                                    timeLabel = firstMed.scheduleTimes.firstOrNull() ?: "Now",
                                    notifId = 2026
                                )
                                onFeedback("Test dose alert sent")
                            }
                        }
                    )
                    val scheduleList = currentMeds.flatMap { med ->
                        med.scheduleTimes.map { time -> med to time }
                    }.sortedBy { it.second }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scheduleList.forEach { (med, time) ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(
                                            imageVector = if (taken) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = if (taken) "Taken" else "Pending",
                                            tint = if (taken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text("${med.name} · ${med.dosage}", style = MaterialTheme.typography.titleMedium)
                                            Text("Time: $time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        text = if (taken) "Taken" else "Tap to mark",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (taken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayedMeds.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = if (showPast) Icons.Filled.History else Icons.Filled.Medication,
                        title = if (showPast) "No past medications logged" else "No current medications",
                        description = if (showPast) {
                            "Record previous anti-seizure medications, dates, and why they were stopped for complete medical history."
                        } else {
                            "Add your prescribed medications with schedule times and enable scheduled dose alerts."
                        },
                        buttonLabel = if (showPast) "Add Past Medication" else "Add Current Medication",
                        onButtonClick = { onAddMedication(if (showPast) "past" else "current") }
                    )
                }
            }
        } else {
            items(displayedMeds, key = { it.id }) { med ->
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
                                    text = "${med.name} · ${med.dosage}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = med.frequency + if (med.prescribedBy.isNotBlank()) " · ${med.prescribedBy}" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onEditMedication(med) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit medication")
                                }
                                IconButton(onClick = { onDeleteMedication(med.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete medication", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (med.scheduleTimes.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                med.scheduleTimes.forEach { t ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = t,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = RobotoMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (med.startDate.isNotBlank() || med.endDate.isNotBlank()) {
                            Text(
                                text = "Dates: ${med.startDate.ifBlank { "Started" }} → ${if (med.status == "past") med.endDate.ifBlank { "Stopped" } else "Ongoing"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (med.status == "past" && med.stopReason.isNotBlank()) {
                            Text(
                                text = "Stop reason: ${med.stopReason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        if (med.sideEffectsNoted.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                med.sideEffectsNoted.forEach { eff ->
                                    AssistChip(onClick = {}, label = { Text("Side effect: $eff") })
                                }
                            }
                        }

                        if (med.notes.isNotBlank()) {
                            Text(med.notes, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (med.status == "current") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (med.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Dose Alert", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = med.reminderEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                withNotificationPermission {
                                                    onToggleReminder(med.id, true)
                                                }
                                            } else {
                                                onToggleReminder(med.id, false)
                                            }
                                        },
                                        modifier = Modifier.testTag("med_reminder_switch")
                                    )
                                }
                                OutlinedButton(onClick = {
                                    archiveTarget = med
                                    archiveReason = ""
                                }) {
                                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Move to Past")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (archiveTarget != null) {
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            title = { Text("Archive ${archiveTarget!!.name} to Past") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record why this medication was discontinued for your neurologist's reference.")
                    OutlinedTextField(
                        value = archiveReason,
                        onValueChange = { archiveReason = it },
                        label = { Text("Reason stopped / switched") },
                        placeholder = { Text("e.g., Side effects, dosage change, resolved") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = archiveTarget!!
                    archiveTarget = null
                    onArchiveToPast(target, archiveReason.trim())
                }) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveTarget = null }) { Text("Cancel") }
            }
        )
    }
}
