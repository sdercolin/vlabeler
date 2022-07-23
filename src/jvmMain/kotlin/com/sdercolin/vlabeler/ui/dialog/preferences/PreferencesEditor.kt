package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.util.runIf

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
        Divider(Modifier.height(1.dp), color = Black50)
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
        PageList(state)
        Page(state)
    }
}

@Composable
private fun RowScope.PageList(state: PreferencesEditorState) {
    LazyColumn(
        Modifier.fillMaxHeight()
            .weight(0.25f)
            .background(color = MaterialTheme.colors.background)
            .padding(vertical = 15.dp)
    ) {
        items(state.pages) { page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .runIf(page == state.selectedPage) {
                        background(color = MaterialTheme.colors.primaryVariant)
                    }
                    .plainClickable { state.selectPage(page) }
                    .padding(vertical = 3.dp)
                    .padding(start = 10.dp + (page.level * 15).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page.canExpand) {
                    val icon = if (page.isExpanded) {
                        Icons.Default.KeyboardArrowRight
                    } else {
                        Icons.Default.KeyboardArrowDown
                    }
                    Icon(
                        imageVector = icon,
                        modifier = Modifier.size(25.dp).plainClickable { state.togglePage(page) },
                        contentDescription = null
                    )
                } else {
                    Spacer(Modifier.size(25.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = string(page.page.displayedName),
                    style = MaterialTheme.typography.body2.runIf(page.level == 0) {
                        copy(fontWeight = FontWeight.Bold)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RowScope.Page(state: PreferencesEditorState) {
    Box(Modifier.weight(0.75f))
}

@Composable
private fun ButtonBar(
    cancel: () -> Unit,
    canApply: Boolean,
    apply: () -> Unit,
    finish: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.End) {
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
