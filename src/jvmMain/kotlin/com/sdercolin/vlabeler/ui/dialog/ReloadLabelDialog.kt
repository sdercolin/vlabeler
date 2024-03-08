package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiff

data class ReloadLabelDialogArgs(
    val moduleName: String,
    val entries: List<Entry>,
    val diff: EntryListDiff,
)

@Composable
fun ReloadLabelDialog(
    args: ReloadLabelDialogArgs,
    finish: (Boolean) -> Unit,
) {
}
