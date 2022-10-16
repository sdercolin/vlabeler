package com.sdercolin.vlabeler.ui.dialog.plugin

import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.ParamMap

class PluginDialogState(
    val plugin: Plugin,
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val project: Project?,
    override val submit: (ParamMap?) -> Unit,
    override val save: (ParamMap) -> Unit,
    val executable: Boolean,
) : BasePluginDialogState(paramMap) {

    override val basePlugin: BasePlugin
        get() = plugin
}
