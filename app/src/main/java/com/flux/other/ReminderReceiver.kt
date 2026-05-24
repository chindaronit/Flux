package com.flux.other

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.ReminderType
import com.flux.data.model.ScheduleRequest
import com.flux.data.model.toIntent
import com.flux.other.Constants.Other.ACTION_MARK_DONE
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == ACTION_MARK_DONE) {
            MarkDoneHandler.handle(context, intent)
            return
        }

        val request = ScheduleRequest.fromIntent(intent) ?: return

        NotificationDispatcher.notify(context, request)

        if (request.recurrence !is RecurrenceRule.Once) {
            scheduleNextReminder(context, request)
        }
    }
}

object MarkDoneHandler {
    fun handle(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: return
        val itemType = intent.getStringExtra("itemType") ?: return
        val workspaceId = intent.getStringExtra("workspaceId") ?: return
        val markedDone = context.getString(R.string.marked_done)
        val failed = context.getString(R.string.failed)
        val notificationId = (itemId + itemType).hashCode()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors
                    .fromApplication(context, ReceiverEntryPoint::class.java)

                when (itemType) {
                    "EVENT" -> entryPoint.eventRepository()
                        .upsertEventInstance(EventInstanceModel(itemId, workspaceId))

                    "HABIT" -> {
                        val config = intent.getStringExtra("habitConfig")
                            ?.let { runCatching { Json.decodeFromString<HabitConfig>(it) }.getOrNull() }

                        val repo = entryPoint.habitRepository()

                        if (config is HabitConfig.Counted) {
                            val today = LocalDate.now().toEpochDay()
                            val existing = repo.getHabitInstance(itemId, today)
                            val updated = existing
                                ?.copy(count = existing.count + 1)
                                ?: HabitInstanceModel(habitId = itemId, workspaceId = workspaceId, instanceDate = today, count = 1)
                            repo.upsertHabitInstance(updated)
                        } else { repo.upsertHabitInstance(HabitInstanceModel(itemId, workspaceId)) }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, markedDone, Toast.LENGTH_SHORT).show()
                }

            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                }
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}

object NotificationDispatcher {

    private const val CHANNEL_ID = "notification_channel"

    fun notify(context: Context, request: ScheduleRequest) {
        ensureChannel(context)

        val isHabit = request.itemType == ReminderType.HABIT

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra("itemId", request.itemId)
            putExtra("itemType", request.itemType.name)
            putExtra("workspaceId", request.workspaceId)
            request.habitConfig?.let {                           // ADD
                putExtra("habitConfig", Json.encodeToString(it))
            }
        }

        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            getUniqueRequestCode(request.itemType.name, request.itemId),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(request.title)
            .setContentText(request.description)
            .setSmallIcon(
                if (isHabit) R.drawable.calendar_check
                else R.drawable.check_list
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // persistent notification
        builder.setOngoing(true).setAutoCancel(false)

        val notification = builder.addAction(R.drawable.check_list, "Done", donePendingIntent).build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((request.itemId + request.itemType.name).hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
    }
}

fun scheduleReminder(context: Context, request: ScheduleRequest, triggerAt: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            requestExactAlarmPermission(context)
            return
        }
    }

    val intent = request.toIntent(context)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        getUniqueRequestCode(request.itemType.name, request.itemId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAt,
        pendingIntent
    )
}

private val zone: ZoneId get() = ZoneId.systemDefault()

fun scheduleNextReminder(
    context: Context,
    request: ScheduleRequest
) {
    when (val config = request.habitConfig) {
        is HabitConfig.Counted -> { scheduleNextCountedReminder(context, request, config) }
        else -> { scheduleStandardReminder(context, request) }
    }
}

private fun scheduleStandardReminder(
    context: Context,
    request: ScheduleRequest
) {
    val now = System.currentTimeMillis()
    var candidate = getNextOccurrence(request.recurrence, request.startDateTime) ?: return
    var finalTime: Long

    while (true) {
        val alarmTime = candidate - request.notificationOffset
        if (alarmTime > now) { finalTime = alarmTime; break }
        candidate = getNextOccurrence(request.recurrence, candidate) ?: return
    }

    if (request.endDateTime != -1L && finalTime > request.endDateTime) return
    scheduleReminder(context, request, finalTime)
}

private fun scheduleNextCountedReminder(
    context: Context,
    request: ScheduleRequest,
    config: HabitConfig.Counted
) {
    val now = ZonedDateTime.now(zone)
    val nextTrigger = calculateNextCountedTrigger(now, request, config) ?: run { return }

    if (request.endDateTime != -1L) {
        val endDate = Instant.ofEpochMilli(request.endDateTime).atZone(zone).toLocalDate()
        val finalAllowed = ZonedDateTime.of(endDate, millisToLocalTime(config.activeEndTime), zone)

        if (nextTrigger.isAfter(finalAllowed)) { return }
    }

    scheduleReminder(context, request, nextTrigger.toInstant().toEpochMilli())
}

private fun calculateNextCountedTrigger(
    now: ZonedDateTime,
    request: ScheduleRequest,
    config: HabitConfig.Counted
): ZonedDateTime? {
    val recurrence = request.recurrence as? RecurrenceRule.Weekly ?: run { return null }

    var currentDate = now.toLocalDate()

    repeat(14) {
        // Monday=0 ... Sunday=6
        val currentDayOfWeek = currentDate.dayOfWeek.value - 1
        val isValidDay = currentDayOfWeek in recurrence.daysOfWeek

        if (isValidDay) {
            val startTime = millisToLocalTime(config.activeStartTime)
            val endTime = millisToLocalTime(config.activeEndTime)

            val windowStart = ZonedDateTime.of(currentDate, startTime, zone)
            val windowEnd = ZonedDateTime.of(currentDate, endTime, zone)

            when {
                now.isBefore(windowStart) || now.isEqual(windowStart) -> { return windowStart }
                now.isBefore(windowEnd) -> {
                    val elapsedMillis = Duration.between(windowStart, now).toMillis()
                    val intervalsPassed = elapsedMillis / config.intervalMillis + 1
                    val nextSlot = windowStart.plus(Duration.ofMillis(intervalsPassed * config.intervalMillis))
                    if (nextSlot.isBefore(windowEnd) || nextSlot.isEqual(windowEnd)) {
                        return nextSlot
                    }
                }
            }
        }

        currentDate = currentDate.plusDays(1)
    }

    return null
}

private fun millisToLocalTime(millis: Long): LocalTime {
    return Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun createNotificationChannel(context: Context) {
    if (!isNotificationPermissionGranted(context)) {
        requestNotificationPermission(context as Activity)
    }

    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel("notification_channel", "Reminders", importance)
    channel.enableVibration(true)
    channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

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

fun cancelReminder(
    context: Context,
    request: ScheduleRequest
) {
    try {
        val intent = request.toIntent(context)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getUniqueRequestCode(request.itemType.name, request.itemId),
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

fun getUniqueRequestCode(type: String, uuid: String): Int {
    return (type + uuid).hashCode()
}