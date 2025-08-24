package com.flux.other

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.flux.R
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "Reminder"
        val description =
            intent.getStringExtra("DESCRIPTION") ?: "It's time to complete pending things"
        val id = intent.getStringExtra("ID") ?: "" // UUID string
        val type = intent.getStringExtra("TYPE") ?: "HABIT"
        val repeat = intent.getStringExtra("REPEAT") ?: "NONE"
        val notificationId = Notifications.getUniqueRequestCode(type, id)

        val notification = NotificationCompat.Builder(context, "notification_channel")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        if (repeat != "NONE") {
            val nextTime = Calendar.getInstance()
            when (repeat) {
                "DAILY" -> nextTime.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> nextTime.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> nextTime.add(Calendar.MONTH, 1)
                "YEARLY" -> nextTime.add(Calendar.YEAR, 1)
            }

            scheduleReminder(
                context = context,
                id = id,
                type = type,
                repeat = repeat,
                timeInMillis = nextTime.timeInMillis,
                title = title,
                description = description,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun requestExactAlarmPermission(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = "package:${context.packageName}".toUri()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(context, "Unable to open alarm permission settings", Toast.LENGTH_LONG)
            .show()
    }
}

fun canScheduleReminder(context: Context): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

fun scheduleReminder(
    context: Context,
    id: String, // UUID string
    type: String,
    repeat: String = "NONE",
    timeInMillis: Long,
    title: String,
    description: String
) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createReminderIntent(context, id, type, title, description, repeat)
        val requestCode = Notifications.getUniqueRequestCode(type, id)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    } catch (e: SecurityException) {
        e.printStackTrace()
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, "Error: Failed to schedule alarm", Toast.LENGTH_LONG).show()
        }
    }
}

fun createReminderIntent(
    context: Context,
    id: String, // UUID string
    type: String,
    title: String,
    description: String,
    repeat: String
): Intent {
    return Intent(context, ReminderReceiver::class.java).apply {
        putExtra("TITLE", title)
        putExtra("DESCRIPTION", description)
        putExtra("ID", id)
        putExtra("TYPE", type)
        putExtra("REPEAT", repeat)
    }
}

fun cancelReminder(
    context: Context,
    id: String, // UUID string
    type: String,
    title: String,
    description: String,
    repeat: String = "NONE"
) {
    try {
        val intent = createReminderIntent(
            context = context,
            id = id,
            type = type,
            title = title,
            description = description,
            repeat = repeat
        )
        val requestCode = Notifications.getUniqueRequestCode(type, id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    } catch (e: SecurityException) {
        e.printStackTrace()
        Toast.makeText(context, "Error: Failed to cancel alarm", Toast.LENGTH_LONG).show()
    }
}