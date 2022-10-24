package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FatalErrorEvent(
    val appVersion: String,
    val runtime: String,
    val osInfo: String,
    val isDebug: Boolean,
    val locale: String,
    val error: String,
) : TrackingEvent() {

    @Transient
    override val name: String = "Fatal Error"
}
