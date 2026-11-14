package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.PatientProfile
import com.example.ui.components.AppointmentEditorDialog
import com.example.ui.components.EmergencyContactEditorDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MedicalRecordUploadDialog
import com.example.ui.components.PatientProfileEditorDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateTime
import com.example.util.CareIntents
import java.io.File

@Composable
fun CareProfileScreen(
    uid: String,
    defaultDisplayName: String,
    profile: PatientProfile?,
    appointments: List<MedicalAppointment>,
    contacts: List<EmergencyContact>,
    records: List<MedicalRecord>,
    onSaveProfile: (PatientProfile) -> Unit,
    onSaveAppointment: (MedicalAppointment) -> Unit,
    onMarkAppointmentLinked: (String) -> Unit,
    onDeleteAppointment: (String) -> Unit,
    onSaveContact: (EmergencyContact) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSaveRecord: (MedicalRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onOpenReportDialog: (String) -> Unit,
    onOpenEmergencyDialog: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showProfileEditor by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<MedicalAppointment?>(null) }
    var showNewAppointment by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var showNewContact by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MedicalRecord?>(null) }
    var showNewRecord by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Patient Profile Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_placeholder),
                            contentDescription = "Patient avatar",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName.ifBlank { "Patient Profile" },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = profile?.epilepsyType?.ifBlank { "Tap Edit to add diagnosis & neurologist" } ?: "Tap Edit to add diagnosis & neurologist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showProfileEditor = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                        }
                    }

                    if (profile != null) {
                        val details = listOfNotNull(
                            profile.dateOfBirth.takeIf { it.isNotBlank() }?.let { "DOB: $it" },
                            profile.primaryNeurologist.takeIf { it.isNotBlank() }?.let { "Neurologist: $it" },
                            profile.neurologistEmail.takeIf { it.isNotBlank() }?.let { "Email: $it" },
                            "Diet Plan: ${profile.dietaryPreference}",
                            profile.allergies.takeIf { it.isNotBlank() }?.let { "Allergies: $it" }
                        )
                        details.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onOpenReportDialog(profile?.neurologistEmail ?: "") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("care_email_report_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF & Email Provider")
                        }
                        OutlinedButton(onClick = onSignOut) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // 2. Medical Records & Test Results Vault
        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                SectionHeader(
                    title = "Medical Records & Test Results (${records.size})",
                    subtitle = "Upload EEGs, MRIs, lab panels & prescriptions to your profile",
                    actionLabel = "+ Upload",
                    onActionClick = { showNewRecord = true }
                )
            }
        }

        if (records.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.UploadFile,
                        title = "No medical records uploaded",
                        description = "Attach EEG reports, blood test panels, or imaging photos directly to your patient profile.",
                        buttonLabel = "Upload Medical Record",
                        onButtonClick = { showNewRecord = true }
                    )
                }
            }
        } else {
            items(records, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        text = rec.recordType,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rec.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    listOfNotNull(
                                        rec.recordDate.takeIf { it.isNotBlank() },
                                        rec.facilityOrDoctor.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { editingRecord = rec }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit record")
                                }
                                IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (rec.findingsSummary.isNotBlank()) {
                            Text(rec.findingsSummary, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (rec.localFileUri.isNotBlank() && rec.mimeType.startsWith("image/") && File(rec.localFileUri).exists()) {
                            AsyncImage(
                                model = File(rec.localFileUri),
                                contentDescription = rec.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (rec.localFileUri.isNotBlank()) {
                            OutlinedButton(onClick = {
                                CareIntents.openStoredMedicalRecord(context, rec.localFileUri, rec.mimeType)
                            }) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open ${rec.fileName.ifBlank { "Attachment" }}")
                            }
                        } else if (rec.fileName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(rec.fileName, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // 3. Appointments & Calendar Linking
        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                SectionHeader(
                    title = "Appointments & Calendar (${appointments.size})",
                    subtitle = "Link doctor visits to any calendar & email reports",
                    actionLabel = "+ Add Visit",
                    onActionClick = { showNewAppointment = true }
                )
            }
        }

        if (appointments.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = "No appointments added",
                        description = "Schedule neurology follow-ups or lab visits and link them to your preferred calendar app.",
                        buttonLabel = "Add Appointment",
                        onButtonClick = { showNewAppointment = true }
                    )
                }
            }
        } else {
            items(appointments, key = { it.id }) { appt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appt.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(formatDateTime(appt.appointmentMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                if (appt.doctorName.isNotBlank() || appt.clinicLocation.isNotBlank()) {
                                    Text(
                                        listOfNotNull(
                                            appt.doctorName.takeIf { it.isNotBlank() },
                                            appt.clinicLocation.takeIf { it.isNotBlank() }
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { editingAppointment = appt }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit appointment")
                                }
                                IconButton(onClick = { onDeleteAppointment(appt.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete appointment", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        if (appt.notes.isNotBlank()) {
                            Text(appt.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (CareIntents.openCalendarForAppointment(context, appt)) {
                                    onMarkAppointmentLinked(appt.id)
                                }
                            }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appt.linkedToCalendar) "Linked to Calendar" else "Link to Calendar")
                            }
                            OutlinedButton(onClick = { onOpenReportDialog(appt.providerEmail) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email Report")
                            }
                        }
                    }
                }
            }
        }

        // 4. Emergency Contacts
        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                SectionHeader(
                    title = "Emergency Contacts (${contacts.size})",
                    subtitle = "1-tap SMS, Email, or Call during urgent seizure events",
                    actionLabel = "+ Add Contact",
                    onActionClick = { showNewContact = true }
                )
            }
        }

        if (contacts.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.ContactEmergency,
                        title = "No emergency contacts yet",
                        description = "Add family members or caregivers for rapid 1-tap SOS notifications.",
                        buttonLabel = "Add Emergency Contact",
                        onButtonClick = { showNewContact = true }
                    )
                }
            }
        } else {
            items(contacts, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name + if (contact.isPrimary) " · Primary" else "",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                val meta = listOfNotNull(
                                    contact.relationship.takeIf { it.isNotBlank() },
                                    contact.phone.takeIf { it.isNotBlank() },
                                    contact.email.takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { editingContact = contact }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit contact")
                                }
                                IconButton(onClick = { onDeleteContact(contact.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete contact", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val alertMsg = "AuraCare Emergency Alert from ${profile?.fullName?.ifBlank { defaultDisplayName } ?: defaultDisplayName}: Please check in or assist."
                            if (contact.phone.isNotBlank()) {
                                OutlinedButton(onClick = { CareIntents.sendEmergencySmsIntent(context, contact.phone, alertMsg) }) {
                                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS")
                                }
                                OutlinedButton(onClick = { CareIntents.dialEmergencyContactIntent(context, contact.phone) }) {
                                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call")
                                }
                            }
                            if (contact.email.isNotBlank()) {
                                OutlinedButton(onClick = {
                                    CareIntents.sendEmergencyEmailIntent(context, contact.email, "AuraCare Emergency Alert", alertMsg)
                                }) {
                                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Email")
                                }
                            }
                            OutlinedButton(onClick = onOpenEmergencyDialog) {
                                Text("SOS Sheet")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showProfileEditor) {
        PatientProfileEditorDialog(
            initialProfile = profile,
            userId = uid,
            defaultName = defaultDisplayName,
            onDismiss = { showProfileEditor = false },
            onSave = { updated ->
                showProfileEditor = false
                onSaveProfile(updated)
            }
        )
    }

    if (showNewAppointment || editingAppointment != null) {
        AppointmentEditorDialog(
            initial = editingAppointment,
            defaultDoctor = profile?.primaryNeurologist ?: "",
            defaultEmail = profile?.neurologistEmail ?: "",
            onDismiss = {
                showNewAppointment = false
                editingAppointment = null
            },
            onSave = { appt, linkNow ->
                val toSave = if (linkNow) {
                    val linked = CareIntents.openCalendarForAppointment(context, appt)
                    appt.copy(linkedToCalendar = linked || appt.linkedToCalendar)
                } else appt
                showNewAppointment = false
                editingAppointment = null
                onSaveAppointment(toSave)
            }
        )
    }

    if (showNewContact || editingContact != null) {
        EmergencyContactEditorDialog(
            initial = editingContact,
            onDismiss = {
                showNewContact = false
                editingContact = null
            },
            onSave = { contact ->
                showNewContact = false
                editingContact = null
                onSaveContact(contact)
            }
        )
    }

    if (showNewRecord || editingRecord != null) {
        MedicalRecordUploadDialog(
            uid = uid,
            initial = editingRecord,
            onDismiss = {
                showNewRecord = false
                editingRecord = null
            },
            onSave = { rec ->
                showNewRecord = false
                editingRecord = null
                onSaveRecord(rec)
            }
        )
    }
}
