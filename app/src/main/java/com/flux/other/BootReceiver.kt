package com.flux.other

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.data.repository.EventRepository
import com.flux.data.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                val reminders = getStoredReminders(context)
                for (reminder in reminders) {
                    val timeInMillis =
                        getNextValidTimeFromOriginal(reminder.timeInMillis, reminder.repeat)
                    if (timeInMillis != -1L) {
                        scheduleReminder(
                            context = context,
                            id = reminder.id,
                            type = reminder.type,
                            repeat = reminder.repeat,
                            timeInMillis = timeInMillis,
                            title = reminder.title,
                            description = reminder.description
                        )
                    }
                }
                pendingResult.finish()
            }
        }
    }

    private fun getNextValidTimeFromOriginal(originalTime: Long, repeat: String): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = originalTime
        }

        if (calendar.timeInMillis >= now) return calendar.timeInMillis
        if (repeat == "NONE") return -1L

        when (repeat) {
            "DAILY" -> {
                val diff = ((now - originalTime) / (1000 * 60 * 60 * 24)).toInt() + 1
                calendar.add(Calendar.DAY_OF_YEAR, diff)
            }

            "WEEKLY" -> {
                val diff = ((now - originalTime) / (1000 * 60 * 60 * 24 * 7)).toInt() + 1
                calendar.add(Calendar.WEEK_OF_YEAR, diff)
            }

            "MONTHLY" -> {
                val start = Calendar.getInstance().apply { timeInMillis = originalTime }
                val diff = (now to start).monthDiff() + 1
                calendar.add(Calendar.MONTH, diff)
            }

            "YEARLY" -> {
                val start = Calendar.getInstance().apply { timeInMillis = originalTime }
                val diff = (now to start).yearDiff() + 1
                calendar.add(Calendar.YEAR, diff)
            }
        }

        return calendar.timeInMillis
    }

    // Extension functions unchanged
    infix fun Long.monthDiffFrom(start: Calendar): Int {
        val now = Calendar.getInstance().apply { timeInMillis = this@monthDiffFrom }
        val yearDiff = now.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        val monthDiff = now.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        return yearDiff * 12 + monthDiff
    }

    infix fun Long.yearDiffFrom(start: Calendar): Int {
        val now = Calendar.getInstance().apply { timeInMillis = this@yearDiffFrom }
        return now.get(Calendar.YEAR) - start.get(Calendar.YEAR)
    }

    fun Pair<Long, Calendar>.monthDiff(): Int = first.monthDiffFrom(second)
    fun Pair<Long, Calendar>.yearDiff(): Int = first.yearDiffFrom(second)

    private suspend fun getStoredReminders(context: Context): List<Reminder> {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, BootReceiverEntryPoint::class.java)
        val habitRepository = entryPoint.habitRepository()
        val eventRepository = entryPoint.eventRepository()

        val reminders = mutableListOf<Reminder>()

        val habits = habitRepository.loadAllHabits()
        val events = eventRepository.loadAllEvents()

        reminders += habits.map {
            Reminder(
                id = it.habitId,
                type = "HABIT",
                repeat = "DAILY",
                timeInMillis = it.startDateTime,
                title = it.title,
                description = it.description
            )
        }

        reminders += events.map {
            Reminder(
                id = it.eventId,
                type = "EVENT",
                repeat = it.repetition,
                timeInMillis = it.startDateTime - it.notificationOffset,
                title = it.title,
                description = it.description
            )
        }

        return reminders
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun habitRepository(): HabitRepository
    fun eventRepository(): EventRepository
}

data class Reminder(
    val id: String,
    val type: String,
    val repeat: String,
    val timeInMillis: Long,
    val title: String,
    val description: String
)