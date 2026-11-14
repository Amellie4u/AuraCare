package com.example.reports

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.DailyLog
import com.example.data.model.MedicalAppointment
import com.example.data.model.MedicalRecord
import com.example.data.model.MedicationEntry
import com.example.data.model.PatientProfile
import com.example.data.model.SeizureLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportOptions(
    val includeSeizures: Boolean = true,
    val includeMedications: Boolean = true,
    val includeDailyLogs: Boolean = true,
    val includeMedicalRecords: Boolean = true,
    val includeAppointments: Boolean = true
)

object ClinicalPdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generateClinicalSummaryPdf(
        context: Context,
        profile: PatientProfile?,
        seizures: List<SeizureLog>,
        medications: List<MedicationEntry>,
        dailyLogs: List<DailyLog>,
        records: List<MedicalRecord>,
        appointments: List<MedicalAppointment>,
        options: ReportOptions = ReportOptions()
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(reportsDir, "AuraCare_Clinical_Summary_${System.currentTimeMillis()}.pdf")

        val nativePdfSucceeded = runCatching {
            val pdf = PdfDocument()
            val dateTimeFmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val generatedStr = dateTimeFmt.format(Date())

            val headerBgPaint = Paint().apply { color = Color.parseColor("#0D6E6E") }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerSubPaint = Paint().apply {
                color = Color.parseColor("#D5EFEA")
                textSize = 10.5f
            }
            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#0D6E6E")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyBoldPaint = Paint().apply {
                color = Color.parseColor("#162120")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 10f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            var canvas = page.canvas

            // Draw top clinical banner on page 1
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 76f, headerBgPaint)
            canvas.drawText("AuraCare Clinical Medical Summary", MARGIN, 34f, headerTextPaint)
            canvas.drawText("Generated: $generatedStr", MARGIN, 54f, headerSubPaint)

            var y = 100f

            fun ensureSpace(needed: Float) {
                if (y + needed > PAGE_HEIGHT - MARGIN) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = MARGIN + 10f
                    canvas.drawText("AuraCare Clinical Summary (Page $pageNum)", MARGIN, MARGIN, headerSubPaint.apply { color = Color.GRAY })
                    y += 16f
                }
            }

            fun drawWrappedLine(text: String, paint: Paint, indent: Float = 0f) {
                val maxWidth = PAGE_WIDTH - (MARGIN * 2) - indent
                val words = text.split(" ")
                var line = StringBuilder()
                for (word in words) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                        ensureSpace(15f)
                        canvas.drawText(line.toString(), MARGIN + indent, y, paint)
                        y += 14f
                        line = StringBuilder(word)
                    } else {
                        line = StringBuilder(candidate)
                    }
                }
                if (line.isNotEmpty()) {
                    ensureSpace(15f)
                    canvas.drawText(line.toString(), MARGIN + indent, y, paint)
                    y += 14f
                }
            }

            fun drawSectionHeader(title: String) {
                ensureSpace(34f)
                y += 8f
                canvas.drawText(title, MARGIN, y, sectionTitlePaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                y += 14f
            }

            // 1. Patient Overview
            drawSectionHeader("Patient Profile & Neurological Context")
            drawWrappedLine("Patient: ${profile?.fullName?.ifBlank { "Not specified" } ?: "Not specified"}", bodyBoldPaint)
            if (!profile?.dateOfBirth.isNullOrBlank()) {
                drawWrappedLine("Date of Birth: ${profile?.dateOfBirth}", bodyPaint)
            }
            if (!profile?.epilepsyType.isNullOrBlank()) {
                drawWrappedLine("Diagnosis / Classification: ${profile?.epilepsyType}", bodyPaint)
            }
            if (!profile?.primaryNeurologist.isNullOrBlank()) {
                drawWrappedLine("Primary Neurologist: ${profile?.primaryNeurologist} (${profile?.neurologistEmail ?: ""})", bodyPaint)
            }
            drawWrappedLine("Dietary Plan: ${profile?.dietaryPreference ?: "Balanced"}", bodyPaint)
            if (!profile?.allergies.isNullOrBlank()) {
                drawWrappedLine("Allergies: ${profile?.allergies}", bodyPaint)
            }

            // 2. Seizure Events
            if (options.includeSeizures) {
                drawSectionHeader("Seizure Events & Side-Effect Log (${seizures.size} recorded)")
                if (seizures.isEmpty()) {
                    drawWrappedLine("No seizure events recorded.", bodyPaint)
                } else {
                    val avgDur = seizures.map { it.durationSeconds }.average().toInt()
                    val topTriggers = seizures.flatMap { it.triggers }.groupingBy { it }.eachCount()
                        .entries.sortedByDescending { it.value }.take(4).joinToString { "${it.key} (${it.value})" }
                    drawWrappedLine("Average Duration: ${formatDuration(avgDur.toLong())} · Top Triggers: ${topTriggers.ifBlank { "None logged" }}", bodyBoldPaint)
                    y += 4f
                    seizures.take(25).forEach { s ->
                        ensureSpace(58f)
                        val dateStr = dateTimeFmt.format(Date(s.occurredAtMillis))
                        drawWrappedLine("• $dateStr — ${s.seizureType} (Duration: ${formatDuration(s.durationSeconds)}, Severity: ${s.severity}/5)", bodyBoldPaint)
                        if (s.triggers.isNotEmpty()) {
                            drawWrappedLine("Triggers: ${s.triggers.joinToString(", ")}", bodyPaint, 12f)
                        }
                        if (s.medicationSideEffects.isNotEmpty()) {
                            drawWrappedLine("Medication Side Effects: ${s.medicationSideEffects.joinToString(", ")}", bodyPaint, 12f)
                        }
                        if (s.symptomsAndAura.isNotBlank()) {
                            drawWrappedLine("Aura / Symptoms: ${s.symptomsAndAura}", bodyPaint, 12f)
                        }
                        if (s.postIctalNotes.isNotBlank()) {
                            drawWrappedLine("Post-Ictal Notes: ${s.postIctalNotes}", bodyPaint, 12f)
                        }
                        y += 4f
                    }
                }
            }

            // 3. Current & Past Medications
            if (options.includeMedications) {
                val currentMeds = medications.filter { it.status == "current" }
                val pastMeds = medications.filter { it.status == "past" }
                drawSectionHeader("Current Medications (${currentMeds.size}) & Past History (${pastMeds.size})")
                if (medications.isEmpty()) {
                    drawWrappedLine("No medications logged.", bodyPaint)
                } else {
                    if (currentMeds.isNotEmpty()) {
                        drawWrappedLine("CURRENT MEDICATIONS:", bodyBoldPaint)
                        currentMeds.forEach { m ->
                            drawWrappedLine("• ${m.name} ${m.dosage} — ${m.frequency} (${m.scheduleTimes.joinToString(", ")})", bodyBoldPaint, 8f)
                            val meta = listOfNotNull(
                                m.prescribedBy.takeIf { it.isNotBlank() }?.let { "Prescriber: $it" },
                                m.startDate.takeIf { it.isNotBlank() }?.let { "Started: $it" },
                                if (m.reminderEnabled) "Reminders ON" else null
                            ).joinToString(" · ")
                            if (meta.isNotBlank()) drawWrappedLine(meta, bodyPaint, 16f)
                            if (m.sideEffectsNoted.isNotEmpty()) {
                                drawWrappedLine("Side effects noted: ${m.sideEffectsNoted.joinToString(", ")}", bodyPaint, 16f)
                            }
                        }
                    }
                    if (pastMeds.isNotEmpty()) {
                        y += 4f
                        drawWrappedLine("PAST MEDICATIONS:", bodyBoldPaint)
                        pastMeds.forEach { m ->
                            drawWrappedLine("• ${m.name} ${m.dosage} (${m.startDate.ifBlank { "?" }} to ${m.endDate.ifBlank { "Past" }})", bodyBoldPaint, 8f)
                            if (m.stopReason.isNotBlank()) {
                                drawWrappedLine("Stop reason: ${m.stopReason}", bodyPaint, 16f)
                            }
                            if (m.sideEffectsNoted.isNotEmpty()) {
                                drawWrappedLine("Side effects noted: ${m.sideEffectsNoted.joinToString(", ")}", bodyPaint, 16f)
                            }
                        }
                    }
                }
            }

            // 4. Daily Activities & Food Intake
            if (options.includeDailyLogs) {
                drawSectionHeader("Recent Daily Activities & Food Intake (${dailyLogs.size} logs)")
                if (dailyLogs.isEmpty()) {
                    drawWrappedLine("No daily activity or nutrition entries logged.", bodyPaint)
                } else {
                    dailyLogs.take(20).forEach { log ->
                        val dStr = dateTimeFmt.format(Date(log.loggedAtMillis))
                        val prefix = if (log.entryType == "food") "[Food · ${log.category}]" else "[Activity · ${log.category}]"
                        val extra = listOfNotNull(
                            log.caloriesOrAmount.takeIf { it.isNotBlank() },
                            if (log.durationMinutes > 0) "${log.durationMinutes} min" else null,
                            log.dietTags.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ).joinToString(" · ")
                        drawWrappedLine("• $dStr $prefix ${log.title}" + if (extra.isNotBlank()) " ($extra)" else "", bodyPaint)
                    }
                }
            }

            // 5. Uploaded Medical Records & Test Results
            if (options.includeMedicalRecords) {
                drawSectionHeader("Uploaded Medical Records & Test Results (${records.size})")
                if (records.isEmpty()) {
                    drawWrappedLine("No medical records uploaded.", bodyPaint)
                } else {
                    records.forEach { r ->
                        drawWrappedLine("• [${r.recordType}] ${r.title} (${r.recordDate.ifBlank { "Undated" }})", bodyBoldPaint)
                        if (r.facilityOrDoctor.isNotBlank()) {
                            drawWrappedLine("Facility/Doctor: ${r.facilityOrDoctor} · File: ${r.fileName.ifBlank { "Summary only" }}", bodyPaint, 12f)
                        }
                        if (r.findingsSummary.isNotBlank()) {
                            drawWrappedLine("Findings: ${r.findingsSummary}", bodyPaint, 12f)
                        }
                    }
                }
            }

            // 6. Appointments
            if (options.includeAppointments && appointments.isNotEmpty()) {
                drawSectionHeader("Scheduled Healthcare Appointments (${appointments.size})")
                appointments.forEach { a ->
                    val dateStr = dateTimeFmt.format(Date(a.appointmentMillis))
                    drawWrappedLine("• $dateStr — ${a.title} (${a.doctorName.ifBlank { "Provider" }} @ ${a.clinicLocation.ifBlank { "Clinic" }})", bodyPaint)
                }
            }

            pdf.finishPage(page)
            FileOutputStream(outFile).use { stream ->
                pdf.writeTo(stream)
            }
            pdf.close()
        }.isSuccess

        if (!nativePdfSucceeded || outFile.length() == 0L) {
            val lines = buildSummaryLines(profile, seizures, medications, dailyLogs, records, appointments, options)
            writeFallbackPdf14(outFile, lines)
        }
        return outFile
    }

    fun buildSummaryLines(
        profile: PatientProfile?,
        seizures: List<SeizureLog>,
        medications: List<MedicationEntry>,
        dailyLogs: List<DailyLog>,
        records: List<MedicalRecord>,
        appointments: List<MedicalAppointment>,
        options: ReportOptions = ReportOptions()
    ): List<String> {
        val dateTimeFmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return buildList {
            add("AuraCare Clinical Medical Summary")
            add("Generated: ${dateTimeFmt.format(Date())}")
            add("Patient: ${profile?.fullName?.ifBlank { "Not specified" } ?: "Not specified"}")
            if (!profile?.epilepsyType.isNullOrBlank()) add("Diagnosis: ${profile?.epilepsyType}")
            if (!profile?.primaryNeurologist.isNullOrBlank()) add("Neurologist: ${profile?.primaryNeurologist}")
            add("Dietary Plan: ${profile?.dietaryPreference ?: "Balanced"}")
            if (options.includeSeizures) {
                add("--- Seizure Events (${seizures.size}) ---")
                seizures.take(25).forEach { s ->
                    add("${dateTimeFmt.format(Date(s.occurredAtMillis))} | ${s.seizureType} | ${formatDuration(s.durationSeconds)} | Sev ${s.severity}/5")
                    if (s.triggers.isNotEmpty()) add("  Triggers: ${s.triggers.joinToString(", ")}")
                    if (s.medicationSideEffects.isNotEmpty()) add("  Side Effects: ${s.medicationSideEffects.joinToString(", ")}")
                }
            }
            if (options.includeMedications) {
                add("--- Medications (${medications.size}) ---")
                medications.forEach { m ->
                    add("[${m.status.uppercase(Locale.US)}] ${m.name} ${m.dosage} (${m.frequency})")
                }
            }
            if (options.includeDailyLogs) {
                add("--- Daily Activities & Food (${dailyLogs.size}) ---")
                dailyLogs.take(20).forEach { d ->
                    add("[${d.entryType}] ${d.title} (${d.category})")
                }
            }
            if (options.includeMedicalRecords) {
                add("--- Medical Records (${records.size}) ---")
                records.forEach { r ->
                    add("[${r.recordType}] ${r.title} (${r.recordDate})")
                }
            }
            if (options.includeAppointments && appointments.isNotEmpty()) {
                add("--- Appointments (${appointments.size}) ---")
                appointments.forEach { a ->
                    add("${dateTimeFmt.format(Date(a.appointmentMillis))} - ${a.title}")
                }
            }
        }
    }

    private fun writeFallbackPdf14(outFile: File, lines: List<String>) {
        val streamContent = buildString {
            append("BT\n/F1 10 Tf\n40 790 Td\n14 TL\n")
            lines.take(50).forEach { raw ->
                val safe = raw.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                append("($safe) Tj T*\n")
            }
            append("ET\n")
        }
        val streamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
        val sb = StringBuilder()
        val offsets = mutableListOf<Int>()
        sb.append("%PDF-1.4\n")
        offsets.add(sb.length)
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        offsets.add(sb.length)
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
        offsets.add(sb.length)
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")
        offsets.add(sb.length)
        sb.append("4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n")
        sb.append(streamContent)
        sb.append("endstream\nendobj\n")
        offsets.add(sb.length)
        sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")
        val xrefPos = sb.length
        sb.append("xref\n0 6\n0000000000 65535 f \n")
        offsets.forEach { off ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
        outFile.writeText(sb.toString(), Charsets.ISO_8859_1)
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }
}
