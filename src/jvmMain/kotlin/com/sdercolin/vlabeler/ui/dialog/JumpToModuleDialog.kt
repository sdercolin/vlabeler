package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.ModuleList

data class JumpToModuleDialogArgs(
    val project: Project,
    val editorConf: AppConf.Editor,
    val viewConf: AppConf.View,
) : EmbeddedDialogArgs {
    override val customMargin: Boolean
        get() = true

    override val cancellableOnClickOutside: Boolean
        get() = true
}

data class JumpToModuleDialogResult(val index: Int) : EmbeddedDialogResult<JumpToModuleDialogArgs>

@Composable
fun JumpToModuleDialog(
    args: JumpToModuleDialogArgs,
    finish: (JumpToModuleDialogResult?) -> Unit,
) {
    ModuleList(
        viewConf = args.viewConf,
        project = args.project,
        jumpToModule = { index -> finish(JumpToModuleDialogResult(index)) },
    )
}
