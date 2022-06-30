@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.EntryList

data class JumpToEntryDialogArgs(val project: Project) : EmbeddedDialogArgs {
    override val customMargin: Boolean
        get() = true

    override val cancellableOnClickOutside: Boolean
        get() = true
}

data class JumpToEntryDialogArgsResult(val sampleName: String, val index: Int) : EmbeddedDialogResult

@Composable
fun JumpToEntryDialog(
    args: JumpToEntryDialogArgs,
    finish: (EmbeddedDialogResult?) -> Unit,
) {
    EntryList(
        pinned = false,
        project = args.project,
        jumpToEntry = { sampleName, index -> finish(JumpToEntryDialogArgsResult(sampleName, index)) }
    )
}
