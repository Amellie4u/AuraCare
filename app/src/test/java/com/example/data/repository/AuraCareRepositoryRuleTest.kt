package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DailyLog
import com.example.data.model.EmergencyContact
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraCareRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun patientProfile_and_allSubcollections_ownerCrud_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = AuraCareRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProfile(
                PatientProfile(
                    userId = uid,
                    fullName = "Alice Morgan",
                    epilepsyType = "Focal Temporal",
                    primaryNeurologist = "Dr. Patel",
                    neurologistEmail = "patel@neuroclinic.org",
                    dietaryPreference = "Ketogenic"
                )
            )
        }
        assertTrue(profileRes.isSuccess)

        val seizureId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSeizure(
                SeizureLog(
                    seizureType = "Focal Impaired",
                    durationSeconds = 75L,
                    occurredAtMillis = 1700000000000L,
                    triggers = listOf("Sleep Deprivation", "Stress"),
                    symptomsAndAura = "Deja vu aura",
                    medicationSideEffects = listOf("Drowsiness"),
                    postIctalNotes = "Rested 30m",
                    severity = 3
                )
            ).getOrThrow()
        }
        assertTrue(seizureId.isNotBlank())

        val medId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMedication(
                MedicationEntry(
                    name = "Levetiracetam",
                    dosage = "500 mg",
                    frequency = "Twice Daily",
                    scheduleTimes = listOf("08:00", "20:00"),
                    status = "current",
                    reminderEnabled = true
                )
            ).getOrThrow()
        }
        assertTrue(medId.isNotBlank())

        val doseRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.setDoseTaken(medId, "Levetiracetam", "500 mg", "08:00", "2026-11-13", true)
        }
        assertTrue(doseRes.isSuccess)

        val dailyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveDailyLog(
                DailyLog(
                    entryType = "food",
                    title = "Salmon & Avocado Bowl",
                    category = "Lunch",
                    mealTime = "12:30",
                    dietTags = listOf("Ketogenic"),
                    loggedAtMillis = 1700000000000L
                )
            ).getOrThrow()
        }
        assertTrue(dailyId.isNotBlank())

        val apptId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveAppointment(
                MedicalAppointment(
                    title = "Neurology Visit",
                    doctorName = "Dr. Patel",
                    providerEmail = "patel@neuroclinic.org",
                    appointmentMillis = 1700500000000L
                )
            ).getOrThrow()
        }
        assertTrue(apptId.isNotBlank())

        val contactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveEmergencyContact(
                EmergencyContact(
                    name = "Sam Morgan",
                    relationship = "Spouse",
                    phone = "555-0199",
                    isPrimary = true
                )
            ).getOrThrow()
        }
        assertTrue(contactId.isNotBlank())

        val recordId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveMedicalRecord(
                MedicalRecord(
                    title = "Oct EEG Report",
                    recordType = "EEG",
                    recordDate = "2026-10-15",
                    findingsSummary = "Normal background rhythm",
                    fileName = "eeg.pdf",
                    mimeType = "application/pdf",
                    fileSizeBytes = 12345L
                )
            ).getOrThrow()
        }
        assertTrue(recordId.isNotBlank())

        val observedSeizures = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSeizures(uid).first { list -> list.any { it.id == seizureId } }
        }
        assertEquals(75L, observedSeizures.first { it.id == seizureId }.durationSeconds)
    }

    @Test
    fun observeSeizures_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AuraCareRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveSeizure(
                SeizureLog(
                    seizureType = "Absence",
                    durationSeconds = 20L,
                    occurredAtMillis = 1700000000000L
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AuraCareRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeSeizures(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun observeSeizures_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = AuraCareRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSeizures(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@auracare.test"
        const val BOB_EMAIL = "bob@auracare.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
