package com.sdercolin.vlabeler.tracking.event

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CreateProjectEvent(
    val labelerName: String,
    val labelerNameVer: String,
    val params: String,
    val autoExport: Boolean,
    val byIpcRequest: Boolean,
    val byQuickEdit: Boolean,
) : TrackingEvent() {

    @Transient
    override val name: String = "Create Project"
}
