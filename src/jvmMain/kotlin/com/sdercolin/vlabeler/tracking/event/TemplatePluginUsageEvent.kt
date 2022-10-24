package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TemplatePluginUsageEvent(
    val pluginName: String,
    val pluginNameVer: String,
    val params: String,
) : TrackingEvent() {

    @Transient
    override val name: String = "Template Plugin Usage"
}
