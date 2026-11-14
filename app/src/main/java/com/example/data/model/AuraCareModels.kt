package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PatientProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val epilepsyType: String = "",
    val primaryNeurologist: String = "",
    val neurologistEmail: String = "",
    val dietaryPreference: String = "Balanced",
    val allergies: String = "",
    val medicalNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SeizureLog(
    @DocumentId val id: String = "",
    val seizureType: String = "Focal Aware",
    val durationSeconds: Long = 0L,
    val occurredAtMillis: Long = 0L,
    val triggers: List<String> = emptyList(),
    val symptomsAndAura: String = "",
    val medicationSideEffects: List<String> = emptyList(),
    val postIctalNotes: String = "",
    val severity: Int = 2,
    val emergencyNotified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MedicationEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "Twice Daily",
    val scheduleTimes: List<String> = listOf("08:00", "20:00"),
    val status: String = "current",
    val prescribedBy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val stopReason: String = "",
    val sideEffectsNoted: List<String> = emptyList(),
    val reminderEnabled: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DoseLog(
    @DocumentId val id: String = "",
    val medicationId: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val scheduledTime: String = "",
    val dateKey: String = "",
    val taken: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLog(
    @DocumentId val id: String = "",
    val entryType: String = "activity", // "activity" or "food"
    val title: String = "",
    val category: String = "",
    val mealTime: String = "",
    val durationMinutes: Int = 0,
    val caloriesOrAmount: String = "",
    val dietTags: List<String> = emptyList(),
    val notes: String = "",
    val loggedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MedicalAppointment(
    @DocumentId val id: String = "",
    val title: String = "",
    val doctorName: String = "",
    val clinicLocation: String = "",
    val providerEmail: String = "",
    val appointmentMillis: Long = 0L,
    val notes: String = "",
    val linkedToCalendar: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EmergencyContact(
    @DocumentId val id: String = "",
    val name: String = "",
    val relationship: String = "",
    val phone: String = "",
    val email: String = "",
    val isPrimary: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MedicalRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val recordType: String = "Lab Test",
    val recordDate: String = "",
    val facilityOrDoctor: String = "",
    val findingsSummary: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val localFileUri: String = "",
    val fileSizeBytes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
