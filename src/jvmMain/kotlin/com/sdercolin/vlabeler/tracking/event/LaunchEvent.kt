package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LaunchEvent(
    val appVersion: String,
    val runtime: String,
    val os: OsInfo,
    val isDebug: Boolean,
    val locale: LocaleInfo,
) : TrackingEvent() {

    @Transient
    override val name: String = "Launch App"
}
