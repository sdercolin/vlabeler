package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

@Serializable
data class SaveAppConfEvent(
    val conf: JsonObject,
) : TrackingEvent() {

    @Transient
    override val name: String = "Save App Conf"
}
