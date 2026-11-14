package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MedicationEntry

private val FREQUENCIES = listOf("Once Daily", "Twice Daily", "Three Times Daily", "Every 12 Hours", "As Needed")
private val PRESET_TIMES = listOf("07:00", "08:00", "12:00", "14:00", "18:00", "20:00", "22:00")
private val MED_SIDE_EFFECTS = listOf("Drowsiness", "Dizziness", "Nausea", "Irritability", "Brain Fog", "Fatigue", "Rash", "Weight Change")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationEditorDialog(
    initialMed: MedicationEntry?,
    defaultStatus: String = "current",
    onDismiss: () -> Unit,
    onSave: (MedicationEntry) -> Unit
) {
    var name by remember { mutableStateOf(initialMed?.name ?: "") }
    var dosage by remember { mutableStateOf(initialMed?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initialMed?.frequency ?: "Twice Daily") }
    var status by remember { mutableStateOf(initialMed?.status ?: defaultStatus) }
    var scheduleTimes by remember { mutableStateOf(initialMed?.scheduleTimes?.toSet() ?: setOf("08:00", "20:00")) }
    var customTime by remember { mutableStateOf("") }
    var prescribedBy by remember { mutableStateOf(initialMed?.prescribedBy ?: "") }
    var startDate by remember { mutableStateOf(initialMed?.startDate ?: "") }
    var endDate by remember { mutableStateOf(initialMed?.endDate ?: "") }
    var stopReason by remember { mutableStateOf(initialMed?.stopReason ?: "") }
    var sideEffects by remember { mutableStateOf(initialMed?.sideEffectsNoted?.toSet() ?: emptySet()) }
    var reminderEnabled by remember { mutableStateOf(initialMed?.reminderEnabled ?: (status == "current")) }
    var notes by remember { mutableStateOf(initialMed?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMed == null) "Add Medication" else "Edit Medication") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == "current",
                        onClick = { status = "current" },
                        label = { Text("Current Med") }
                    )
                    FilterChip(
                        selected = status == "past",
                        onClick = {
                            status = "past"
                            reminderEnabled = false
                        },
                        label = { Text("Past Med") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g., Levetiracetam, Lamotrigine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it; validationError = null },
                    label = { Text("Dosage *") },
                    placeholder = { Text("e.g., 500 mg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FREQUENCIES.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq) }
                        )
                    }
                }

                Text("Scheduled Dose Times", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (PRESET_TIMES + scheduleTimes.filterNot { it in PRESET_TIMES }).forEach { time ->
                        FilterChip(
                            selected = time in scheduleTimes,
                            onClick = {
                                scheduleTimes = if (time in scheduleTimes) scheduleTimes - time else scheduleTimes + time
                            },
                            label = { Text(time) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTime,
                        onValueChange = { customTime = it.take(5) },
                        label = { Text("Custom time (HH:mm)") },
                        placeholder = { Text("09:30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val trimmed = customTime.trim()
                        if (Regex("^([01]?\\d|2[0-3]):[0-5]\\d$").matches(trimmed)) {
                            val padded = if (trimmed.length == 4) "0$trimmed" else trimmed
                            scheduleTimes = scheduleTimes + padded
                            customTime = ""
                        }
                    }) { Text("Add") }
                }

                if (status == "current") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dose Reminders", style = MaterialTheme.typography.titleMedium)
                            Text("Send local alert at scheduled times", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }
                }

                OutlinedTextField(
                    value = prescribedBy,
                    onValueChange = { prescribedBy = it },
                    label = { Text("Prescribed By") },
                    placeholder = { Text("Dr. Patel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("2026-01-10") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (status == "past") {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("End Date") },
                            placeholder = { Text("2026-10-01") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (status == "past") {
                    OutlinedTextField(
                        value = stopReason,
                        onValueChange = { stopReason = it },
                        label = { Text("Reason Stopped / Switched") },
                        placeholder = { Text("e.g., Switched due to drowsiness") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Side Effects Noted", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MED_SIDE_EFFECTS.forEach { eff ->
                        FilterChip(
                            selected = eff in sideEffects,
                            onClick = {
                                sideEffects = if (eff in sideEffects) sideEffects - eff else sideEffects + eff
                            },
                            label = { Text(eff) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Instructions / Notes") },
                    placeholder = { Text("Take with food, avoid missed doses...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || dosage.isBlank()) {
                        validationError = "Please enter both medication name and dosage."
                        return@Button
                    }
                    val med = (initialMed ?: MedicationEntry()).copy(
                        name = name.trim(),
                        dosage = dosage.trim(),
                        frequency = frequency,
                        scheduleTimes = scheduleTimes.toList().sorted().take(8),
                        status = status,
                        prescribedBy = prescribedBy.trim(),
                        startDate = startDate.trim(),
                        endDate = if (status == "past") endDate.trim() else "",
                        stopReason = if (status == "past") stopReason.trim() else "",
                        sideEffectsNoted = sideEffects.toList().take(15),
                        reminderEnabled = status == "current" && reminderEnabled,
                        notes = notes.trim()
                    )
                    onSave(med)
                },
                modifier = Modifier.testTag("save_medication_button")
            ) {
                Text("Save Medication")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
