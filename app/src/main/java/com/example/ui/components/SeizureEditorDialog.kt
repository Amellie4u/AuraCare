package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SeizureLog
import kotlin.math.roundToInt

private val SEIZURE_TYPES = listOf(
    "Focal Aware",
    "Focal Impaired",
    "Generalized Tonic-Clonic",
    "Absence",
    "Myoclonic",
    "Atonic",
    "Other"
)

private val COMMON_TRIGGERS = listOf(
    "Sleep Deprivation",
    "Stress",
    "Missed Dose",
    "Flashing Lights",
    "Illness / Fever",
    "Dehydration",
    "Hormonal",
    "Alcohol",
    "Skipped Meal"
)

private val COMMON_SIDE_EFFECTS = listOf(
    "Drowsiness",
    "Dizziness",
    "Brain Fog",
    "Nausea",
    "Mood Changes",
    "Headache",
    "Blurred Vision",
    "Tremor",
    "Fatigue"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeizureEditorDialog(
    initialLog: SeizureLog?,
    prefilledSeconds: Long?,
    prefilledTimestampMillis: Long?,
    onDismiss: () -> Unit,
    onSave: (SeizureLog, Boolean) -> Unit
) {
    val startSeconds = prefilledSeconds ?: initialLog?.durationSeconds ?: 45L
    var seizureType by remember { mutableStateOf(initialLog?.seizureType ?: "Focal Aware") }
    var minutesText by remember { mutableStateOf((startSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((startSeconds % 60).toString()) }
    var severity by remember { mutableFloatStateOf((initialLog?.severity ?: 2).toFloat()) }
    var selectedTriggers by remember { mutableStateOf(initialLog?.triggers?.toSet() ?: emptySet()) }
    var customTrigger by remember { mutableStateOf("") }
    var selectedSideEffects by remember { mutableStateOf(initialLog?.medicationSideEffects?.toSet() ?: emptySet()) }
    var customSideEffect by remember { mutableStateOf("") }
    var auraText by remember { mutableStateOf(initialLog?.symptomsAndAura ?: "") }
    var notesText by remember { mutableStateOf(initialLog?.postIctalNotes ?: "") }
    var notifyEmergency by remember { mutableStateOf(initialLog?.emergencyNotified ?: false) }

    val occurredAt = initialLog?.occurredAtMillis?.takeIf { it > 0L }
        ?: prefilledTimestampMillis?.takeIf { it > 0L }
        ?: System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLog == null) "Log Seizure Event" else "Edit Seizure Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Recorded Time: ${formatDateTime(occurredAt)}", style = MaterialTheme.typography.labelMedium)

                Text("Seizure Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SEIZURE_TYPES.forEach { type ->
                        FilterChip(
                            selected = seizureType == type,
                            onClick = { seizureType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text("Duration", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Severity: ${severity.roundToInt()} / 5", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = severity,
                    onValueChange = { severity = it },
                    valueRange = 1f..5f,
                    steps = 3
                )

                Text("Possible Triggers", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (COMMON_TRIGGERS + selectedTriggers.filterNot { it in COMMON_TRIGGERS }).forEach { trigger ->
                        FilterChip(
                            selected = trigger in selectedTriggers,
                            onClick = {
                                selectedTriggers = if (trigger in selectedTriggers) {
                                    selectedTriggers - trigger
                                } else {
                                    selectedTriggers + trigger
                                }
                            },
                            label = { Text(trigger) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTrigger,
                        onValueChange = { customTrigger = it },
                        label = { Text("Custom trigger") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val trimmed = customTrigger.trim()
                            if (trimmed.isNotEmpty()) {
                                selectedTriggers = selectedTriggers + trimmed
                                customTrigger = ""
                            }
                        }
                    ) { Text("Add") }
                }

                Text("Medication Side Effects Noted", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (COMMON_SIDE_EFFECTS + selectedSideEffects.filterNot { it in COMMON_SIDE_EFFECTS }).forEach { effect ->
                        FilterChip(
                            selected = effect in selectedSideEffects,
                            onClick = {
                                selectedSideEffects = if (effect in selectedSideEffects) {
                                    selectedSideEffects - effect
                                } else {
                                    selectedSideEffects + effect
                                }
                            },
                            label = { Text(effect) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customSideEffect,
                        onValueChange = { customSideEffect = it },
                        label = { Text("Custom side effect") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val trimmed = customSideEffect.trim()
                            if (trimmed.isNotEmpty()) {
                                selectedSideEffects = selectedSideEffects + trimmed
                                customSideEffect = ""
                            }
                        }
                    ) { Text("Add") }
                }

                OutlinedTextField(
                    value = auraText,
                    onValueChange = { auraText = it },
                    label = { Text("Aura & Symptoms") },
                    placeholder = { Text("e.g., Tingling sensation, deja vu, visual aura...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Post-Ictal & Recovery Notes") },
                    placeholder = { Text("Recovery time, confusion, rescue med used...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notifyEmergency, onCheckedChange = { notifyEmergency = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Emergency Alert after saving", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesText.toLongOrNull() ?: 0L
                    val secs = secondsText.toLongOrNull() ?: 0L
                    val totalSecs = (mins * 60L + secs).coerceIn(0L, 86400L)
                    val entry = (initialLog ?: SeizureLog()).copy(
                        seizureType = seizureType,
                        durationSeconds = totalSecs,
                        occurredAtMillis = occurredAt,
                        triggers = selectedTriggers.toList().take(15),
                        symptomsAndAura = auraText.trim(),
                        medicationSideEffects = selectedSideEffects.toList().take(15),
                        postIctalNotes = notesText.trim(),
                        severity = severity.roundToInt().coerceIn(1, 5),
                        emergencyNotified = notifyEmergency
                    )
                    onSave(entry, notifyEmergency)
                },
                modifier = Modifier.testTag("save_seizure_button")
            ) {
                Text("Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
