package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.EntryList

data class JumpToEntryDialogArgs(val project: Project) : EmbeddedDialogArgs {
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
    EntryList(
        pinned = false,
        project = args.project,
        jumpToEntry = { index -> finish(JumpToEntryDialogResult(index)) },
        onFocusedChanged = {},
    )
}
