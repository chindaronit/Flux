package com.flux.other

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.ui.screens.events.toFormattedDateTime
import kotlinx.serialization.json.Json

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "Reminder"
        val description =
            intent.getStringExtra("DESCRIPTION") ?: "It's time to complete pending things"
        val id = intent.getStringExtra("ID") ?: ""
        val type = intent.getStringExtra("TYPE") ?: "EVENT"

        // Deserialize recurrence rule
        val recurrenceJson = intent.getStringExtra("RECURRENCE")
        val recurrence = recurrenceJson?.let {
            Json.decodeFromString<RecurrenceRule>(it)
        } ?: RecurrenceRule.Once()

        val notificationId = getUniqueRequestCode(type, id)

        val notification = NotificationCompat.Builder(context, "notification_channel")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        // Reschedule next occurrence if recurring
        val nextTime = getNextOccurrence(recurrence, System.currentTimeMillis())
        println("scheduling for ${nextTime?.toFormattedDateTime(context)}, current: ${System.currentTimeMillis().toFormattedDateTime(context)}")
        if (nextTime != null) {
            scheduleReminder(
                context = context,
                id = id,
                type = type,
                recurrence = recurrence,
                timeInMillis = nextTime,
                title = title,
                description = description
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun createNotificationChannel(context: Context) {
    if (!isNotificationPermissionGranted(context)) {
        requestNotificationPermission(context as Activity)
    }

    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel("notification_channel", "Reminders", importance)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
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
        Toast.makeText(context, context.getString(R.string.Error_Alarm_Permission_Settings), Toast.LENGTH_LONG)
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
    id: String,
    type: String,
    recurrence: RecurrenceRule,
    timeInMillis: Long,
    title: String,
    description: String
) {
    try {
        println("scheduleReminder ${timeInMillis.toFormattedDateTime(context)}")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createReminderIntent(context, id, type, title, description, recurrence)
        val requestCode = getUniqueRequestCode(type, id)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    } catch (e: SecurityException) {
        e.printStackTrace()
        Handler(context.mainLooper).post {
            Toast.makeText(context, context.getString(R.string.Error_Schedule_Alarm_Failed), Toast.LENGTH_LONG).show()
        }
    }
}

fun createReminderIntent(
    context: Context,
    id: String,
    type: String,
    title: String,
    description: String,
    recurrence: RecurrenceRule
): Intent {
    return Intent(context, ReminderReceiver::class.java).apply {
        putExtra("TITLE", title)
        putExtra("DESCRIPTION", description)
        putExtra("ID", id)
        putExtra("TYPE", type)
        putExtra("RECURRENCE", Json.encodeToString(recurrence))
    }
}

fun getUniqueRequestCode(type: String, uuid: String): Int {
    return (type + uuid).hashCode()
}

fun cancelReminder(
    context: Context,
    id: String,
    type: String,
    title: String,
    description: String,
    recurrence: RecurrenceRule
) {
    try {
        val intent = createReminderIntent(
            context = context,
            id = id,
            type = type,
            title = title,
            description = description,
            recurrence = recurrence
        )
        val requestCode = getUniqueRequestCode(type, id)
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
        Toast.makeText(context, context.getString(R.string.Error_Cancel_Alarm_Failed), Toast.LENGTH_LONG).show()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun isNotificationPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun requestNotificationPermission(activity: Activity, requestCode: Int = 1001) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        requestCode
    )
}

fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}