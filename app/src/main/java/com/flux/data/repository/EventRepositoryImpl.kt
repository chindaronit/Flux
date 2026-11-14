package com.flux.data.repository

import com.flux.data.dao.EventDao
import com.flux.data.dao.EventInstanceDao
import com.flux.data.dao.EventExceptionDao
import com.flux.data.dao.EventOccurrenceColorDao
import com.flux.data.database.FluxDatabase
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventExceptionModel
import com.flux.data.model.EventOccurrenceColorModel
import com.flux.data.model.EventModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventInstanceDao: EventInstanceDao,
    private val eventExceptionDao: EventExceptionDao,
    private val eventOccurrenceColorDao: EventOccurrenceColorDao,
    private val database: FluxDatabase
) : EventRepository {
    override suspend fun upsertEvent(event: EventModel) {
        return withContext(Dispatchers.IO) { eventDao.upsertEvent(event) }
    }

    override suspend fun upsertEvents(events: List<EventModel>) {
        withContext(Dispatchers.IO) { eventDao.upsertEvents(events) }
    }

    override suspend fun upsertEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(Dispatchers.IO) { eventInstanceDao.upsertEventInstance(eventInstanceModel) }
    }

    override suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(Dispatchers.IO) { eventInstanceDao.deleteEventInstance(eventInstanceModel) }
    }

    override suspend fun detachEventOccurrence(
        exception: EventExceptionModel,
        detachedEvent: EventModel
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                eventExceptionDao.upsert(exception)
                eventOccurrenceColorDao.deleteForOccurrence(exception.eventId, exception.occurrenceDate)
                eventDao.upsertEvent(detachedEvent)
            }
        }
    }

    override suspend fun deleteEventOccurrence(exception: EventExceptionModel) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                eventExceptionDao.upsert(exception)
                eventOccurrenceColorDao.deleteForOccurrence(exception.eventId, exception.occurrenceDate)
                eventInstanceDao.deleteEventInstance(
                    EventInstanceModel(
                        eventId = exception.eventId,
                        workspaceId = exception.workspaceId,
                        instanceDate = exception.occurrenceDate
                    )
                )
            }
        }
    }

    override suspend fun loadAllEvents(): List<EventModel> {
        return eventDao.loadAllEvents()
    }

    override fun loadEventData(): Flow<List<EventModel>> {
        return eventDao.loadEventData()
    }

    override fun loadEventInstanceData(): Flow<List<EventInstanceModel>> {
        return eventInstanceDao.loadEventInstanceData()
    }

    override fun loadEventExceptionData(): Flow<List<EventExceptionModel>> =
        eventExceptionDao.loadAll()

    override fun loadEventOccurrenceColorData(): Flow<List<EventOccurrenceColorModel>> =
        eventOccurrenceColorDao.loadAll()

    override suspend fun upsertEventOccurrenceColor(color: EventOccurrenceColorModel) {
        withContext(Dispatchers.IO) { eventOccurrenceColorDao.upsert(color) }
    }

    override suspend fun deleteEventOccurrenceColor(eventId: String, occurrenceDate: Long) {
        withContext(Dispatchers.IO) {
            eventOccurrenceColorDao.deleteForOccurrence(eventId, occurrenceDate)
        }
    }

    override suspend fun setEventColorForSeries(event: EventModel) {
        withContext(Dispatchers.IO) {
            eventDao.upsertEvent(event)
        }
    }

    override suspend fun deleteEvent(event: EventModel) {
        return withContext(Dispatchers.IO) {
            eventExceptionDao.deleteForEvent(event.id)
            eventOccurrenceColorDao.deleteForEvent(event.id)
            eventInstanceDao.deleteAllEventInstance(event.id)
            eventDao.deleteEvent(event)
        }
    }

    override suspend fun deleteAllWorkspaceEvent(workspaceId: String) {
        return withContext(Dispatchers.IO) {
            eventDao.deleteAllWorkspaceEvents(workspaceId)
            eventInstanceDao.deleteAllWorkspaceInstance(workspaceId)
            eventExceptionDao.deleteForWorkspace(workspaceId)
            eventOccurrenceColorDao.deleteForWorkspace(workspaceId)
        }
    }
}