package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DailyLog
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.example.notifications.DoseReminderReceiver
import com.example.reports.ClinicalPdfGenerator
import com.example.reports.ReportOptions
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.viewmodel.SeizureTimerState
import com.example.util.CareIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatStopwatch(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun formatDateTime(millis: Long): String {
    if (millis <= 0L) return "Unknown date"
    val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonLabel != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onButtonClick) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
fun LiveSeizureTimerCard(
    timerState: SeizureTimerState,
    onStart: () -> Unit,
    onStopAndLog: () -> Unit,
    onReset: () -> Unit,
    onEmergencyClick: () -> Unit
) {
    val containerColor = if (timerState.isRunning) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (timerState.isRunning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (timerState.isRunning) "SEIZURE TIMER ACTIVE" else "Seizure Duration Timer",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                    Text(
                        text = formatStopwatch(timerState.elapsedSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = contentColor
                    )
                }
                OutlinedButton(
                    onClick = onEmergencyClick,
                    modifier = Modifier.testTag("emergency_alert_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    border = null
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Emergency alert",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SOS Alert")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!timerState.isRunning) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_timer_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (timerState.elapsedSeconds > 0L) "Resume Timer" else "Start Seizure Timer")
                    }
                    if (timerState.elapsedSeconds > 0L) {
                        OutlinedButton(onClick = onStopAndLog) {
                            Text("Save (${formatStopwatch(timerState.elapsedSeconds)})")
                        }
                        IconButton(onClick = onReset) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset timer", tint = contentColor)
                        }
                    }
                } else {
                    Button(
                        onClick = onStopAndLog,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_timer_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop & Log Seizure")
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyAlertDialog(
    patientName: String,
    timerSeconds: Long,
    contacts: List<EmergencyContact>,
    onDismiss: () -> Unit,
    onLocalAlertTriggered: () -> Unit
) {
    val context = LocalContext.current
    val durationNotice = if (timerSeconds > 0L) " Active seizure duration: ${formatStopwatch(timerSeconds)}." else ""
    val defaultMsg = "URGENT AuraCare Alert: ${patientName.ifBlank { "Patient" }} may be experiencing a seizure and needs assistance.$durationNotice"

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val summary = contacts.firstOrNull()?.name ?: "Care team"
            DoseReminderReceiver.showEmergencyLocalAlert(context, patientName.ifBlank { "Patient" }, summary)
            onLocalAlertTriggered()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Emergency Contact Notification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Send an urgent alert to your saved care contacts or trigger a high-priority local alert.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPerm) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val summary = contacts.firstOrNull()?.name ?: "Care team"
                            DoseReminderReceiver.showEmergencyLocalAlert(context, patientName.ifBlank { "Patient" }, summary)
                            onLocalAlertTriggered()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger On-Device Caregiver Alert")
                }

                if (contacts.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No emergency contacts saved yet. Add contacts under the Care tab for 1-tap SMS, Email, and Call.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    contacts.forEach { contact ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${contact.name}${if (contact.isPrimary) " (Primary)" else ""}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (contact.relationship.isNotBlank()) {
                                    Text(contact.relationship, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (contact.phone.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            CareIntents.sendEmergencySmsIntent(context, contact.phone, defaultMsg)
                                        }) {
                                            Icon(Icons.Filled.Sms, contentDescription = "Send SMS", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SMS")
                                        }
                                        OutlinedButton(onClick = {
                                            CareIntents.dialEmergencyContactIntent(context, contact.phone)
                                        }) {
                                            Icon(Icons.Filled.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call")
                                        }
                                    }
                                    if (contact.email.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            CareIntents.sendEmergencyEmailIntent(
                                                context,
                                                contact.email,
                                                "URGENT: AuraCare Seizure Alert",
                                                defaultMsg
                                            )
                                        }) {
                                            Icon(Icons.Filled.Email, contentDescription = "Email", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Email")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalReportDialog(
    initialEmail: String,
    profile: PatientProfile?,
    seizures: List<SeizureLog>,
    medications: List<MedicationEntry>,
    dailyLogs: List<DailyLog>,
    records: List<MedicalRecord>,
    appointments: List<MedicalAppointment>,
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var recipientEmail by remember { mutableStateOf(initialEmail) }
    var includeSeizures by remember { mutableStateOf(true) }
    var includeMeds by remember { mutableStateOf(true) }
    var includeDaily by remember { mutableStateOf(true) }
    var includeRecords by remember { mutableStateOf(true) }
    var includeAppointments by remember { mutableStateOf(true) }

    val providerEmails = remember(profile, appointments) {
        buildList {
            profile?.neurologistEmail?.takeIf { it.isNotBlank() }?.let { add(it) }
            appointments.mapNotNull { it.providerEmail.takeIf { e -> e.isNotBlank() } }.forEach {
                if (it !in this) add(it)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Clinical PDF & Provider Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select sections to include in the PDF summary:",
                    style = MaterialTheme.typography.bodyMedium
                )
                ReportCheckRow("Seizures, Triggers & Side Effects (${seizures.size})", includeSeizures) { includeSeizures = it }
                ReportCheckRow("Current & Past Medications (${medications.size})", includeMeds) { includeMeds = it }
                ReportCheckRow("Daily Activities & Food Intake (${dailyLogs.size})", includeDaily) { includeDaily = it }
                ReportCheckRow("Uploaded Medical Records (${records.size})", includeRecords) { includeRecords = it }
                ReportCheckRow("Appointments (${appointments.size})", includeAppointments) { includeAppointments = it }

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text("Healthcare Provider Email") },
                    placeholder = { Text("neurologist@clinic.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (providerEmails.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        providerEmails.forEach { email ->
                            TextButton(onClick = { recipientEmail = email }) {
                                Text(email, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = ReportOptions(
                        includeSeizures = includeSeizures,
                        includeMedications = includeMeds,
                        includeDailyLogs = includeDaily,
                        includeMedicalRecords = includeRecords,
                        includeAppointments = includeAppointments
                    )
                    val file = ClinicalPdfGenerator.generateClinicalSummaryPdf(
                        context = context,
                        profile = profile,
                        seizures = seizures,
                        medications = medications,
                        dailyLogs = dailyLogs,
                        records = records,
                        appointments = appointments,
                        options = options
                    )
                    val subject = "AuraCare Clinical Summary - ${profile?.fullName?.ifBlank { "Patient" } ?: "Patient"}"
                    val body = "Attached is the AuraCare clinical PDF summary including seizure logs, medication history, side effects, daily wellness, and medical records index."
                    CareIntents.emailReportToProvider(context, recipientEmail, subject, body, file)
                    onFeedback("PDF generated & email composer opened")
                    onDismiss()
                },
                modifier = Modifier.testTag("email_pdf_report_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Email PDF")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        val options = ReportOptions(
                            includeSeizures = includeSeizures,
                            includeMedications = includeMeds,
                            includeDailyLogs = includeDaily,
                            includeMedicalRecords = includeRecords,
                            includeAppointments = includeAppointments
                        )
                        val file = ClinicalPdfGenerator.generateClinicalSummaryPdf(
                            context = context,
                            profile = profile,
                            seizures = seizures,
                            medications = medications,
                            dailyLogs = dailyLogs,
                            records = records,
                            appointments = appointments,
                            options = options
                        )
                        CareIntents.openPdfFile(context, file)
                        onFeedback("Clinical PDF summary generated")
                    },
                    modifier = Modifier.testTag("preview_pdf_report_button")
                ) {
                    Text("View PDF")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ReportCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
