package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class DoseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Scheduled Medication"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1001)

        showDoseNotification(context, medName, dosage, timeLabel, notifId)
    }

    companion object {
        const val CHANNEL_DOSE_ID = "auracare_dose_alerts"
        const val CHANNEL_EMERGENCY_ID = "auracare_emergency_alerts"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_TIME_LABEL = "extra_time_label"
        const val EXTRA_NOTIF_ID = "extra_notif_id"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val doseChannel = NotificationChannel(
                    CHANNEL_DOSE_ID,
                    "Medication Dose Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for scheduled anti-seizure medication doses"
                }
                val emergencyChannel = NotificationChannel(
                    CHANNEL_EMERGENCY_ID,
                    "Emergency Seizure Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent local alerts when emergency contact notification is triggered"
                }
                manager.createNotificationChannel(doseChannel)
                manager.createNotificationChannel(emergencyChannel)
            }
        }

        fun showDoseNotification(
            context: Context,
            medName: String,
            dosage: String,
            timeLabel: String,
            notifId: Int
        ) {
            ensureChannels(context)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val doseDetails = buildString {
                append(medName)
                if (dosage.isNotBlank()) append(" ($dosage)")
                if (timeLabel.isNotBlank()) append(" · Scheduled $timeLabel")
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_DOSE_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Medication Dose Due")
                .setContentText(doseDetails)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$doseDetails. Tap to mark this dose taken in AuraCare."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notifId, notification)
        }

        fun showEmergencyLocalAlert(
            context: Context,
            patientName: String,
            contactSummary: String
        ) {
            ensureChannels(context)
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                9901,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Emergency Seizure Alert Active")
                .setContentText("Prepared emergency alert for $patientName ($contactSummary).")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(9901, notification)
        }
    }
}
