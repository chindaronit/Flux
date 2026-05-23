package com.flux.data.repository

import com.flux.data.dao.EventDao
import com.flux.data.dao.EventInstanceDao
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventInstanceDao: EventInstanceDao
) : EventRepository {
    override suspend fun upsertEvent(event: EventModel) {
        return withContext(Dispatchers.IO) { eventDao.upsertEvent(event) }
    }

    override suspend fun upsertEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(Dispatchers.IO) { eventInstanceDao.upsertEventInstance(eventInstanceModel) }
    }

    override suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(Dispatchers.IO) { eventInstanceDao.deleteEventInstance(eventInstanceModel) }
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

    override suspend fun deleteEvent(event: EventModel) {
        return withContext(Dispatchers.IO) {
            eventInstanceDao.deleteAllEventInstance(event.id)
            eventDao.deleteEvent(event)
        }
    }

    override suspend fun deleteAllWorkspaceEvent(workspaceId: String) {
        return withContext(Dispatchers.IO) {
            eventDao.deleteAllWorkspaceEvents(workspaceId)
            eventInstanceDao.deleteAllWorkspaceInstance(workspaceId)
        }
    }
}