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
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.ReminderItem
import com.flux.data.model.ReminderType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "flux.action.MARK_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ─────────────────────────────────────────────
        // MARK DONE ACTION
        // ─────────────────────────────────────────────
        if (intent.action == ACTION_MARK_DONE) {
            val id = intent.getStringExtra("ID") ?: return
            val type = intent.getStringExtra("TYPE") ?: return
            val workspaceId = intent.getStringExtra("WORKSPACE_ID") ?: return

            markDone(context, type, id, workspaceId)
            return
        }

        // ─────────────────────────────────────────────
        // EXTRACT PAYLOAD
        // ─────────────────────────────────────────────
        val title =
            intent.getStringExtra("TITLE") ?: "Reminder"

        val description =
            intent.getStringExtra("DESCRIPTION")
                ?: "It's time to complete pending things"

        val id = intent.getStringExtra("ID") ?: return
        val workspaceId =
            intent.getStringExtra("WORKSPACE_ID") ?: ""

        val type =
            intent.getStringExtra("TYPE") ?: "EVENT"

        val endTimeInMillis =
            intent.getLongExtra("ENDTIME", -1L)

        val startDateTime =
            intent.getLongExtra("START_TIME", -1L)

        val offset =
            intent.getLongExtra("OFFSET", 0L)

        // Guard against corrupted / legacy alarms
        if (startDateTime <= 0L) return

        // Deserialize recurrence rule
        val recurrenceJson =
            intent.getStringExtra("RECURRENCE")

        val recurrence =
            recurrenceJson?.let {
                Json.decodeFromString<RecurrenceRule>(it)
            } ?: RecurrenceRule.Once

        // ─────────────────────────────────────────────
        // SHOW NOTIFICATION
        // ─────────────────────────────────────────────
        val icon =
            if (type == "EVENT")
                R.drawable.check_list
            else
                R.drawable.calendar_check

        val notificationId =
            getUniqueRequestCode(type, id)

        val doneIntent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra("ID", id)
                putExtra("TYPE", type)
                putExtra("WORKSPACE_ID", workspaceId)
            }

        val donePendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                doneIntent,
                PendingIntent.FLAG_IMMUTABLE or
                        PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notification =
            NotificationCompat.Builder(
                context,
                "notification_channel"
            )
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(
                    R.drawable.check_list,
                    "Done",
                    donePendingIntent
                )
                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(notificationId, notification)

        // ─────────────────────────────────────────────
        // BUILD REMINDER ITEM
        // ─────────────────────────────────────────────
        val item = object : ReminderItem {
            override val id = id
            override val title = title
            override val description = description
            override val recurrence = recurrence
            override val type =
                ReminderType.valueOf(type)
            override val startDateTime = startDateTime
            override val endDateTime = endTimeInMillis
            override val workspaceId = workspaceId
            override val notificationOffset = offset
        }

        // ─────────────────────────────────────────────
        // RESCHEDULE NEXT OCCURRENCE
        // ─────────────────────────────────────────────
        scheduleNextReminder(context, item)
    }
}

private fun markDone(
    context: Context,
    type: String,
    id: String,
    workspaceId: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    context,
                    ReceiverEntryPoint::class.java
                )

            when (type) {

                "EVENT" ->
                    entryPoint.eventRepository()
                        .upsertEventInstance(
                            EventInstanceModel(
                                eventId = id,
                                workspaceId = workspaceId
                            )
                        )

                else ->
                    entryPoint.habitRepository()
                        .upsertHabitInstance(
                            HabitInstanceModel(
                                habitId = id,
                                workspaceId = workspaceId
                            )
                        )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Done!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed to mark done",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val manager =
        context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

    manager.cancel(
        getUniqueRequestCode(type, id)
    )
}

fun scheduleNextReminder(
    context: Context,
    item: ReminderItem
) {
    val now = System.currentTimeMillis()

    if (item.startDateTime <= 0L) return

    var candidate =
        getNextOccurrence(
            item.recurrence,
            item.startDateTime
        ) ?: return

    var finalTime: Long?

    // Catch-up loop (handles missed alarms / backups)
    while (true) {

        val alarmTime =
            candidate - item.notificationOffset

        if (alarmTime > now) {
            finalTime = alarmTime
            break
        }

        candidate =
            getNextOccurrence(
                item.recurrence,
                candidate
            ) ?: return
    }

    if (item.endDateTime != -1L &&
        finalTime > item.endDateTime
    ) return

    scheduleReminder(
        context = context,
        id = item.id,
        type = item.type.toString(),
        recurrence = item.recurrence,
        timeInMillis = finalTime,
        endTimeInMillis = item.endDateTime,
        startDateTime = item.startDateTime,
        notificationOffset = item.notificationOffset,
        workspaceId = item.workspaceId,
        title = item.title,
        description = item.description
    )
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
    endTimeInMillis: Long,
    startDateTime: Long,
    notificationOffset: Long,
    workspaceId: String,
    title: String,
    description: String
) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createReminderIntent(context, id, type, title, description, workspaceId, startDateTime, notificationOffset, endTimeInMillis, recurrence)
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
    workspaceId: String,
    startDateTime: Long,
    notificationOffset: Long,
    endTimeInMillis: Long,
    recurrence: RecurrenceRule
): Intent {
    return Intent(context, ReminderReceiver::class.java).apply {
        putExtra("TITLE", title)
        putExtra("DESCRIPTION", description)
        putExtra("ID", id)
        putExtra("TYPE", type)
        putExtra("RECURRENCE", Json.encodeToString(recurrence))
        putExtra("ENDTIME", endTimeInMillis)
        putExtra("WORKSPACE_ID", workspaceId)
        putExtra("START_TIME", startDateTime)
        putExtra("OFFSET", notificationOffset)
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
    workspaceId: String,
    endTimeInMillis: Long,
    startDateTime: Long,
    notificationOffset: Long,
    recurrence: RecurrenceRule
) {
    try {
        val intent = createReminderIntent(
            context = context,
            id = id,
            type = type,
            title = title,
            description = description,
            recurrence = recurrence,
            workspaceId = workspaceId,
            endTimeInMillis = endTimeInMillis,
            startDateTime = startDateTime,
            notificationOffset = notificationOffset
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