package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyLog
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import com.example.data.recipes.RecipeCatalog
import com.example.reports.ClinicalPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuraCareFeatureTest {

    @Test
    fun recipeCatalog_prioritizesKetogenicRecipes() {
        val ketogenic = RecipeCatalog.suggestedFor("Ketogenic", "All")
        assertTrue(ketogenic.isNotEmpty())
        assertTrue(ketogenic.first().dietTags.contains("Ketogenic"))
    }

    @Test
    fun clinicalPdfGenerator_createsNonEmptyPdfSummary() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pdfFile = ClinicalPdfGenerator.generateClinicalSummaryPdf(
            context = context,
            profile = PatientProfile(
                userId = "u1",
                fullName = "Alex Rivera",
                epilepsyType = "Focal Temporal",
                primaryNeurologist = "Dr. Chen",
                neurologistEmail = "chen@clinic.org",
                dietaryPreference = "Ketogenic"
            ),
            seizures = listOf(
                SeizureLog(
                    id = "s1",
                    seizureType = "Focal Impaired",
                    durationSeconds = 95L,
                    occurredAtMillis = 1700000000000L,
                    triggers = listOf("Sleep Deprivation"),
                    medicationSideEffects = listOf("Drowsiness"),
                    severity = 3
                )
            ),
            medications = listOf(
                MedicationEntry(
                    id = "m1",
                    name = "Levetiracetam",
                    dosage = "500 mg",
                    frequency = "Twice Daily",
                    scheduleTimes = listOf("08:00", "20:00"),
                    status = "current",
                    reminderEnabled = true
                )
            ),
            dailyLogs = listOf(
                DailyLog(
                    id = "d1",
                    entryType = "food",
                    title = "Wild Salmon & Avocado Bowl",
                    category = "Lunch",
                    loggedAtMillis = 1700000000000L
                )
            ),
            records = listOf(
                MedicalRecord(
                    id = "r1",
                    title = "Oct EEG",
                    recordType = "EEG",
                    recordDate = "2026-10-15",
                    findingsSummary = "Mild focal slowing"
                )
            ),
            appointments = listOf(
                MedicalAppointment(
                    id = "a1",
                    title = "Neurology Review",
                    doctorName = "Dr. Chen",
                    appointmentMillis = 1700500000000L
                )
            )
        )
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 500L)
        assertTrue(pdfFile.readText(Charsets.ISO_8859_1).startsWith("%PDF-"))
        assertEquals("1m 35s", ClinicalPdfGenerator.formatDuration(95L))
    }
}
