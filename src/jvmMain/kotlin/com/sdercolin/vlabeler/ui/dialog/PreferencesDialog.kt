package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditor
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPage

data class PreferencesDialogArgs(
    val currentConf: AppConf,
    val initialPage: PreferencesPage?,
    val onViewPage: (PreferencesPage) -> Unit
) : EmbeddedDialogArgs {
    override val customMargin: Boolean
        get() = true
}

data class PreferencesDialogResult(val newConf: AppConf?) : EmbeddedDialogResult<PreferencesDialogArgs>

@Composable
fun PreferencesDialog(
    args: PreferencesDialogArgs,
    finish: (PreferencesDialogResult?) -> Unit
) {
    PreferencesEditor(
        currentConf = args.currentConf,
        submit = { newConf ->
            finish(PreferencesDialogResult(newConf))
        },
        initialPage = args.initialPage,
        onViewPage = args.onViewPage
    )
}
