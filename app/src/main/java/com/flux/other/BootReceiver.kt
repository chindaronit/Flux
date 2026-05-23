package com.flux.other

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.data.model.ScheduleRequest
import com.flux.data.model.isLive
import com.flux.data.model.toScheduleRequest
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

        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = getStoredReminders(context)

                reminders.forEach { request ->
                    scheduleNextReminder(context, request)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ---------------------- LOAD + CONVERT ----------------------

    private suspend fun getStoredReminders(context: Context): List<ScheduleRequest> {

        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            ReceiverEntryPoint::class.java
        )

        val habitRepo = entryPoint.habitRepository()
        val eventRepo = entryPoint.eventRepository()

        val habits = habitRepo.loadAllHabits().filter { it.isLive() }.map { it.toScheduleRequest() }
        val events = eventRepo.loadAllEvents().filter { it.isLive() }.map { it.toScheduleRequest() }

        return habits + events
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun habitRepository(): HabitRepository
    fun eventRepository(): EventRepository
}
