package com.flux.data.repository

import com.flux.data.dao.EventDao
import com.flux.data.dao.EventInstanceDao
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventInstanceDao: EventInstanceDao,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : EventRepository {
    override suspend fun upsertEvent(event: EventModel) {
        return withContext(ioDispatcher) { eventDao.upsertEvent(event) }
    }

    override suspend fun upsertEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(ioDispatcher) { eventInstanceDao.upsertEventInstance(eventInstanceModel) }
    }

    override suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel) {
        return withContext(ioDispatcher) { eventInstanceDao.deleteEventInstance(eventInstanceModel) }
    }

    override suspend fun loadAllEvents(): List<EventModel> {
        return eventDao.loadAllEvents()
    }

    override fun loadAllWorkspaceEvents(workspaceId: String): Flow<List<EventModel>> {
        return eventDao.loadAllEvents(workspaceId)
    }

    override fun loadAllEventInstances(workspaceId: String): Flow<List<EventInstanceModel>> {
        return eventInstanceDao.loadAllWorkspaceInstances(workspaceId)
    }

    override suspend fun deleteEvent(event: EventModel) {
        return withContext(ioDispatcher) {
            eventInstanceDao.deleteAllEventInstance(event.eventId)
            eventDao.deleteEvent(event)
        }
    }

    override suspend fun deleteAllWorkspaceEvent(workspaceId: String) {
        return withContext(ioDispatcher) {
            eventDao.deleteAllWorkspaceEvents(workspaceId)
            eventInstanceDao.deleteAllWorkspaceInstance(workspaceId)
        }
    }
}