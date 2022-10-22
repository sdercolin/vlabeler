package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.util.ParamTypedMap
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class BasePluginPreset(val pluginName: String, val pluginVersion: Int, val params: ParamTypedMap?)
