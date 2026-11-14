package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.PatientProfile
import com.example.util.CareIntents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RECORD_TYPES = listOf("EEG", "MRI / Imaging", "Blood / Lab Test", "Prescription", "Clinical Summary", "Other")
private val DIET_OPTIONS = listOf("Balanced", "Ketogenic", "Modified Atkins", "Low-Glycemic")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientProfileEditorDialog(
    initialProfile: PatientProfile?,
    userId: String,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (PatientProfile) -> Unit
) {
    var fullName by remember { mutableStateOf(initialProfile?.fullName?.ifBlank { defaultName } ?: defaultName) }
    var dob by remember { mutableStateOf(initialProfile?.dateOfBirth ?: "") }
    var epilepsyType by remember { mutableStateOf(initialProfile?.epilepsyType ?: "") }
    var neurologist by remember { mutableStateOf(initialProfile?.primaryNeurologist ?: "") }
    var neuroEmail by remember { mutableStateOf(initialProfile?.neurologistEmail ?: "") }
    var dietPref by remember { mutableStateOf(initialProfile?.dietaryPreference?.ifBlank { "Balanced" } ?: "Balanced") }
    var allergies by remember { mutableStateOf(initialProfile?.allergies ?: "") }
    var medicalNotes by remember { mutableStateOf(initialProfile?.medicalNotes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Patient Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMsg = null },
                    label = { Text("Patient Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = epilepsyType,
                    onValueChange = { epilepsyType = it },
                    label = { Text("Epilepsy / Seizure Diagnosis") },
                    placeholder = { Text("e.g., Focal Temporal Lobe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = neurologist,
                    onValueChange = { neurologist = it },
                    label = { Text("Primary Neurologist") },
                    placeholder = { Text("Dr. Patel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = neuroEmail,
                    onValueChange = { neuroEmail = it },
                    label = { Text("Neurologist / Clinic Email") },
                    placeholder = { Text("dr.patel@clinic.org") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Dietary Plan (for recipe suggestions)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DIET_OPTIONS.forEach { diet ->
                        FilterChip(selected = dietPref == diet, onClick = { dietPref = diet }, label = { Text(diet) })
                    }
                }
                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("General Care Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isBlank()) {
                    errorMsg = "Please enter the patient's name."
                    return@Button
                }
                onSave(
                    (initialProfile ?: PatientProfile(userId = userId)).copy(
                        userId = userId,
                        fullName = fullName.trim(),
                        dateOfBirth = dob.trim(),
                        epilepsyType = epilepsyType.trim(),
                        primaryNeurologist = neurologist.trim(),
                        neurologistEmail = neuroEmail.trim(),
                        dietaryPreference = dietPref,
                        allergies = allergies.trim(),
                        medicalNotes = medicalNotes.trim()
                    )
                )
            }) { Text("Save Profile") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AppointmentEditorDialog(
    initial: MedicalAppointment?,
    defaultDoctor: String,
    defaultEmail: String,
    onDismiss: () -> Unit,
    onSave: (MedicalAppointment, Boolean) -> Unit
) {
    val defaultDate = remember(initial) {
        val ms = initial?.appointmentMillis?.takeIf { it > 0L } ?: (System.currentTimeMillis() + 7 * 86400_000L)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))
    }
    val defaultTime = remember(initial) {
        val ms = initial?.appointmentMillis?.takeIf { it > 0L } ?: (System.currentTimeMillis() + 7 * 86400_000L)
        SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))
    }
    var title by remember { mutableStateOf(initial?.title ?: "Neurology Follow-up") }
    var doctor by remember { mutableStateOf(initial?.doctorName?.ifBlank { defaultDoctor } ?: defaultDoctor) }
    var location by remember { mutableStateOf(initial?.clinicLocation ?: "") }
    var providerEmail by remember { mutableStateOf(initial?.providerEmail?.ifBlank { defaultEmail } ?: defaultEmail) }
    var dateText by remember { mutableStateOf(defaultDate) }
    var timeText by remember { mutableStateOf(defaultTime) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var linkNow by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule Appointment" else "Edit Appointment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Visit Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = { Text("Doctor / Provider") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = providerEmail,
                    onValueChange = { providerEmail = it },
                    label = { Text("Provider Email (for reports)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Clinic Location / Video Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Questions / Notes for Visit") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = linkNow, onCheckedChange = { linkNow = it })
                    Text("Open calendar app to link event", style = MaterialTheme.typography.bodyMedium)
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    errorMsg = "Visit title is required."
                    return@Button
                }
                val parsedMs = runCatching {
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    fmt.parse("${dateText.trim()} ${timeText.trim()}")?.time
                }.getOrNull() ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis

                onSave(
                    (initial ?: MedicalAppointment()).copy(
                        title = title.trim(),
                        doctorName = doctor.trim(),
                        clinicLocation = location.trim(),
                        providerEmail = providerEmail.trim(),
                        appointmentMillis = parsedMs,
                        notes = notes.trim(),
                        linkedToCalendar = initial?.linkedToCalendar ?: false
                    ),
                    linkNow
                )
            }) { Text("Save Visit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmergencyContactEditorDialog(
    initial: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var relationship by remember { mutableStateOf(initial?.relationship ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var isPrimary by remember { mutableStateOf(initial?.isPrimary ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Emergency Contact" else "Edit Emergency Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Contact Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    placeholder = { Text("Spouse, Parent, Caregiver") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                    Text("Primary Emergency Contact")
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    errorMsg = "Contact name is required."
                    return@Button
                }
                onSave(
                    (initial ?: EmergencyContact()).copy(
                        name = name.trim(),
                        relationship = relationship.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        isPrimary = isPrimary
                    )
                )
            }) { Text("Save Contact") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalRecordUploadDialog(
    uid: String,
    initial: MedicalRecord?,
    onDismiss: () -> Unit,
    onSave: (MedicalRecord) -> Unit
) {
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var recordType by remember { mutableStateOf(initial?.recordType ?: "EEG") }
    var recordDate by remember { mutableStateOf(initial?.recordDate?.ifBlank { today } ?: today) }
    var facility by remember { mutableStateOf(initial?.facilityOrDoctor ?: "") }
    var summary by remember { mutableStateOf(initial?.findingsSummary ?: "") }
    var fileName by remember { mutableStateOf(initial?.fileName ?: "") }
    var mimeType by remember { mutableStateOf(initial?.mimeType ?: "") }
    var localUri by remember { mutableStateOf(initial?.localFileUri ?: "") }
    var fileSize by remember { mutableStateOf(initial?.fileSizeBytes ?: 0L) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val copied = CareIntents.copyUriToInternalRecords(context, uid, uri)
            if (copied != null) {
                fileName = copied.fileName
                mimeType = copied.mimeType
                localUri = copied.localPath
                fileSize = copied.sizeBytes
                if (title.isBlank()) {
                    title = copied.fileName.substringBeforeLast(".")
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val copied = CareIntents.copyUriToInternalRecords(context, uid, uri)
            if (copied != null) {
                fileName = copied.fileName
                mimeType = copied.mimeType
                localUri = copied.localPath
                fileSize = copied.sizeBytes
                if (title.isBlank()) {
                    title = "Test Scan ($today)"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Upload Medical Record / Result" else "Edit Medical Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { documentPicker.launch(arrayOf("application/pdf", "image/*", "text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick PDF/Doc")
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Photo")
                    }
                }

                if (fileName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attached: $fileName (${(fileSize / 1024L).coerceAtLeast(1L)} KB)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Record Title *") },
                    placeholder = { Text("e.g., Video EEG Report, Keppra Serum Level") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Record Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RECORD_TYPES.forEach { t ->
                        FilterChip(selected = recordType == t, onClick = { recordType = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = recordDate,
                    onValueChange = { recordDate = it },
                    label = { Text("Test / Record Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = facility,
                    onValueChange = { facility = it },
                    label = { Text("Lab / Hospital / Ordering Doctor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Results & Clinical Findings Summary") },
                    placeholder = { Text("Key values, doctor notes, normal/abnormal findings...") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    errorMsg = "Record title is required."
                    return@Button
                }
                onSave(
                    (initial ?: MedicalRecord()).copy(
                        title = title.trim(),
                        recordType = recordType,
                        recordDate = recordDate.trim(),
                        facilityOrDoctor = facility.trim(),
                        findingsSummary = summary.trim(),
                        fileName = fileName,
                        mimeType = mimeType,
                        localFileUri = localUri,
                        fileSizeBytes = fileSize
                    )
                )
            }) { Text("Save to Profile") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
