package com.omnipad.ui.state

import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrenceColorModel

data class EventState(
    val isAllEventsLoading: Boolean = true,
    val allEvent: List<EventModel> = emptyList(),
    val allEventInstances: List<EventInstanceModel> = emptyList(),
    val eventExceptions: List<EventExceptionModel> = emptyList(),
    val eventOccurrenceColors: List<EventOccurrenceColorModel> = emptyList()
)
