package com.sdercolin.vlabeler.ui.dialog.plugin

import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.ParamMap

class LabelerDialogState(
    val labeler: LabelerConf,
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val submit: (ParamMap?) -> Unit,
    override val save: (ParamMap) -> Unit,
) : BasePluginDialogState(paramMap) {

    override val project: Project? = null

    override val basePlugin: BasePlugin
        get() = labeler
}
