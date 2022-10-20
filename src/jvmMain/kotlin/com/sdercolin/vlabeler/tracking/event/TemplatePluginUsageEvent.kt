package com.sdercolin.vlabeler.tracking.event

import com.sdercolin.vlabeler.util.ParamTypedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TemplatePluginUsageEvent(
    val plugin: BasePluginBox,
    val params: ParamTypedMap,
) : TrackingEvent() {

    @Transient
    override val name: String = "Template Plugin Usage"
}
