package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.EntryList
import com.sdercolin.vlabeler.ui.editor.EntryListFilterState

data class JumpToEntryDialogArgs(val project: Project, val editorConf: AppConf.Editor) : EmbeddedDialogArgs {
    override val customMargin: Boolean
        get() = true

    override val cancellableOnClickOutside: Boolean
        get() = true
}

data class JumpToEntryDialogResult(val index: Int) : EmbeddedDialogResult<JumpToEntryDialogArgs>

@Composable
fun JumpToEntryDialog(
    args: JumpToEntryDialogArgs,
    finish: (JumpToEntryDialogResult?) -> Unit,
) {
    val filterState = remember { EntryListFilterState() }
    EntryList(
        editorConf = args.editorConf,
        pinned = false,
        filterState = filterState,
        project = args.project,
        jumpToEntry = { index -> finish(JumpToEntryDialogResult(index)) },
        onFocusedChanged = {},
    )
}
