package com.sdercolin.vlabeler.tracking.event

import com.sdercolin.vlabeler.util.ParamTypedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MacroPluginUsageEvent(
    val plugin: BasePluginBox,
    val params: ParamTypedMap,
    val quickLaunch: Boolean,
) : TrackingEvent() {

    @Transient
    override val name: String = "Macro Plugin Usage"
}
