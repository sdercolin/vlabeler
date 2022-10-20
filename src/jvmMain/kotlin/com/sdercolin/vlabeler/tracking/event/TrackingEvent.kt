package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable

@Serializable
sealed class TrackingEvent {
    abstract val name: String
}
