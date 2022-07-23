package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black80

@Composable
private fun rememberPreferencesEditorState(currentConf: AppConf, submit: (AppConf?) -> Unit) =
    remember(currentConf, submit) {
        PreferencesEditorState(currentConf, submit)
    }

@Composable
fun PreferencesEditor(
    currentConf: AppConf,
    submit: (AppConf?) -> Unit,
    state: PreferencesEditorState = rememberPreferencesEditorState(currentConf, submit)
) {
    Column(Modifier.fillMaxSize(0.8f).plainClickable()) {
        Content(state)
        Divider(Modifier.height(1.dp), color = Black80)
        ButtonBar(
            cancel = { state.finish(false) },
            canApply = state.canSave,
            apply = { state.save() },
            finish = { state.finish(true) }
        )
    }
}

@Composable
private fun ColumnScope.Content(state: PreferencesEditorState) {
    Row(Modifier.fillMaxWidth().weight(1f)) {
        PageSelector(state)
        Divider(Modifier.width(1.dp), color = Black80)
        Page(state)
    }
}

@Composable
private fun RowScope.PageSelector(state: PreferencesEditorState) {
}

@Composable
private fun RowScope.Page(state: PreferencesEditorState) {
}

@Composable
private fun ButtonBar(
    cancel: () -> Unit,
    canApply: Boolean,
    apply: () -> Unit,
    finish: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { cancel() }) {
            Text(string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        TextButton(
            enabled = canApply,
            onClick = { apply() }
        ) {
            Text(string(Strings.CommonApply))
        }
        Spacer(Modifier.width(25.dp))
        Button(
            onClick = { finish() }
        ) {
            Text(string(Strings.CommonOkay))
        }
    }
}
