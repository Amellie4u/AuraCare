package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.MedicationEntry
import java.util.Calendar

object DoseReminderScheduler {
    fun syncMedicationReminders(context: Context, medications: List<MedicationEntry>) {
        DoseReminderReceiver.ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        medications.forEach { med ->
            med.scheduleTimes.forEachIndexed { index, timeStr ->
                val requestCode = (med.id + "_" + index).hashCode()
                val intent = Intent(context, DoseReminderReceiver::class.java).apply {
                    putExtra(DoseReminderReceiver.EXTRA_MED_NAME, med.name)
                    putExtra(DoseReminderReceiver.EXTRA_DOSAGE, med.dosage)
                    putExtra(DoseReminderReceiver.EXTRA_TIME_LABEL, timeStr)
                    putExtra(DoseReminderReceiver.EXTRA_NOTIF_ID, requestCode)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (med.status == "current" && med.reminderEnabled) {
                    val triggerMillis = nextTriggerMillis(timeStr)
                    if (triggerMillis != null) {
                        alarmManager.setInexactRepeating(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    private fun nextTriggerMillis(timeStr: String): Long? {
        val parts = timeStr.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
