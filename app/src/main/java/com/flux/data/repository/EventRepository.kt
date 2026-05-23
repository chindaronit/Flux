package com.flux.data.repository

import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun upsertEvent(event: EventModel)
    suspend fun deleteEvent(event: EventModel)
    suspend fun deleteAllWorkspaceEvent(workspaceId: String)
    suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel)
    suspend fun upsertEventInstance(eventInstanceModel: EventInstanceModel)
    suspend fun loadAllEvents(): List<EventModel>
    fun loadEventData(): Flow<List<EventModel>>
    fun loadEventInstanceData(): Flow<List<EventInstanceModel>>
}