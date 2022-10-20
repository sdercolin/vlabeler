package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class InitializeEvent(val id: String) : TrackingEvent() {

    @Transient
    override val name: String = "Initialize Tracking"
}
