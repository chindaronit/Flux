package com.flux.ui.state

import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventExceptionModel
import com.flux.data.model.EventModel
import com.flux.data.model.EventOccurrenceColorModel

data class EventState(
    val isAllEventsLoading: Boolean = true,
    val allEvent: List<EventModel> = emptyList(),
    val allEventInstances: List<EventInstanceModel> = emptyList(),
    val eventExceptions: List<EventExceptionModel> = emptyList(),
    val eventOccurrenceColors: List<EventOccurrenceColorModel> = emptyList()
)
