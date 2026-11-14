package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MedicalAppointment
import java.io.File
import java.io.FileOutputStream

object CareIntents {
    private fun fileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun openCalendarForAppointment(context: Context, appointment: MedicalAppointment): Boolean {
        val endMillis = appointment.appointmentMillis + 60 * 60 * 1000L
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, appointment.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, appointment.clinicLocation)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                buildString {
                    if (appointment.doctorName.isNotBlank()) append("Provider: ${appointment.doctorName}\n")
                    if (appointment.providerEmail.isNotBlank()) append("Email: ${appointment.providerEmail}\n")
                    if (appointment.notes.isNotBlank()) append("Notes: ${appointment.notes}")
                }
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, appointment.appointmentMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Link Appointment to Calendar"))
            true
        }.getOrElse {
            Toast.makeText(context, "No calendar app available on this device.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openPdfFile(context: Context, file: File) {
        val uri = fileUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open Clinical PDF Summary"))
        }.onFailure {
            Toast.makeText(context, "PDF saved to ${file.name}. Share or email to view if no PDF viewer is installed.", Toast.LENGTH_LONG).show()
        }
    }

    fun openStoredMedicalRecord(context: Context, localPath: String, mimeType: String) {
        val file = File(localPath)
        if (!file.exists()) {
            Toast.makeText(context, "Attachment file not found on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = fileUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open Medical Record"))
        }.onFailure {
            Toast.makeText(context, "No viewer installed for this file type.", Toast.LENGTH_SHORT).show()
        }
    }

    fun emailReportToProvider(
        context: Context,
        recipientEmail: String,
        subject: String,
        body: String,
        pdfFile: File
    ) {
        val uri = fileUri(context, pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Email Report to Provider"))
        }.onFailure {
            Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmergencySmsIntent(context: Context, phone: String, message: String) {
        val uri = Uri.parse("smsto:${Uri.encode(phone.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Notify Emergency Contact via SMS"))
        }.onFailure {
            Toast.makeText(context, "No messaging app available.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmergencyEmailIntent(context: Context, email: String, subject: String, body: String) {
        val uri = Uri.parse("mailto:${Uri.encode(email.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Email Emergency Contact"))
        }.onFailure {
            Toast.makeText(context, "No email app available.", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialEmergencyContactIntent(context: Context, phone: String) {
        val uri = Uri.parse("tel:${Uri.encode(phone.trim())}")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "No phone dialer available.", Toast.LENGTH_SHORT).show()
        }
    }

    data class CopiedRecordFile(
        val fileName: String,
        val mimeType: String,
        val localPath: String,
        val sizeBytes: Long
    )

    fun copyUriToInternalRecords(context: Context, uid: String, uri: Uri): CopiedRecordFile? {
        val resolver = context.contentResolver
        var displayName = "record_${System.currentTimeMillis()}"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) {
                    cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val dir = File(context.filesDir, "medical_records/$uid").apply { mkdirs() }
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(dir, "${System.currentTimeMillis()}_$safeName")
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val finalSize = if (destFile.length() > 0L) destFile.length() else size
            CopiedRecordFile(
                fileName = displayName,
                mimeType = mimeType,
                localPath = destFile.absolutePath,
                sizeBytes = finalSize
            )
        }.getOrNull()
    }
}
