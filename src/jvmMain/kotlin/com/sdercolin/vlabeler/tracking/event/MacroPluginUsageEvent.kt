package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MacroPluginUsageEvent(
    val pluginName: String,
    val pluginNameVer: String,
    val params: String,
    val quickLaunch: Boolean,
) : TrackingEvent() {

    @Transient
    override val name: String = "Macro Plugin Usage"
}
