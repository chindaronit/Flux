package com.flux.other

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.data.model.ReminderItem
import com.flux.data.repository.EventRepository
import com.flux.data.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                val reminders = getStoredReminders(context)

                // Reschedule each reminder at its next occurrence
                reminders.forEach { reminder ->
                    val nextTime = getNextOccurrence(reminder.recurrence, reminder.startDateTime)
                    if (nextTime != null) {
                        scheduleReminder(
                            context = context,
                            id = reminder.id,
                            type = reminder.type.name,
                            recurrence = reminder.recurrence,
                            timeInMillis = nextTime-reminder.notificationOffset,
                            title = reminder.title,
                            description = reminder.description
                        )
                    }
                }

                pendingResult.finish()
            }
        }
    }

    // --- Load reminders from repositories ---
    private suspend fun getStoredReminders(context: Context): List<ReminderItem> {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            BootReceiverEntryPoint::class.java
        )
        val habitRepository = entryPoint.habitRepository()
        val eventRepository = entryPoint.eventRepository()

        val habits = habitRepository.loadAllHabits()
        val events = eventRepository.loadAllEvents()

        return habits + events
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun habitRepository(): HabitRepository
    fun eventRepository(): EventRepository
}
