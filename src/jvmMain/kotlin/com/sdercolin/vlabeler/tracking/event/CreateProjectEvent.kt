package com.sdercolin.vlabeler.tracking.event

import com.sdercolin.vlabeler.util.ParamTypedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CreateProjectEvent(
    val labeler: BasePluginBox,
    val params: ParamTypedMap,
    val autoExport: Boolean,
    val byIpcRequest: Boolean,
) : TrackingEvent() {

    @Transient
    override val name: String = "Create Project"
}
