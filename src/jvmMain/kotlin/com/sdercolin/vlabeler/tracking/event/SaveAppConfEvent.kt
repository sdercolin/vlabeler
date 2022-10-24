package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SaveAppConfEvent(
    val conf: String,
) : TrackingEvent() {

    @Transient
    override val name: String = "Save App Conf"
}
