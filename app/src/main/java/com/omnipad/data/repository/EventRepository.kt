package com.omnipad.data.repository

import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventOccurrenceColorModel
import com.omnipad.data.model.EventModel
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun upsertEvent(event: EventModel)
    suspend fun upsertEvents(events: List<EventModel>)
    suspend fun deleteEvent(event: EventModel)
    suspend fun deleteAllWorkspaceEvent(workspaceId: String)
    suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel)
    suspend fun upsertEventInstance(eventInstanceModel: EventInstanceModel)
    suspend fun detachEventOccurrence(exception: EventExceptionModel, detachedEvent: EventModel)
    suspend fun deleteEventOccurrence(exception: EventExceptionModel)
    suspend fun loadAllEvents(): List<EventModel>
    fun loadEventData(): Flow<List<EventModel>>
    fun loadEventInstanceData(): Flow<List<EventInstanceModel>>
    fun loadEventExceptionData(): Flow<List<EventExceptionModel>>
    fun loadEventOccurrenceColorData(): Flow<List<EventOccurrenceColorModel>>
    suspend fun upsertEventOccurrenceColor(color: EventOccurrenceColorModel)
    suspend fun deleteEventOccurrenceColor(eventId: String, occurrenceDate: Long)
    suspend fun setEventColorForSeries(event: EventModel)
}