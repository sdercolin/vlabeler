package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LaunchEvent(
    val appVersion: String,
    val runtime: String,
    val osName: String,
    val osInfo: String,
    val isDebug: Boolean,
    val locale: String,
) : TrackingEvent() {

    @Transient
    override val name: String = "Launch App"
}
