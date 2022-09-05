package com.sdercolin.vlabeler.ui.dialog.updater

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import java.awt.Desktop
import java.net.URI

@Composable
private fun rememberUpdaterDialogState(
    update: Update,
    appRecordStore: AppRecordStore,
    onError: (Throwable) -> Unit,
    finish: () -> Unit,
): UpdaterDialogState {
    val scope = rememberCoroutineScope()
    return remember(update) {
        UpdaterDialogState(update, scope, appRecordStore, onError, finish)
    }
}

@Composable
fun UpdaterDialog(
    update: Update,
    appRecordStore: AppRecordStore,
    onError: (Throwable) -> Unit,
    finish: () -> Unit,
    state: UpdaterDialogState = rememberUpdaterDialogState(update, appRecordStore, onError, finish),
) {
    Dialog(
        title = string(Strings.UpdaterDialogTitle),
        icon = painterResource("icon.ico"),
        onCloseRequest = { state.cancel() },
        state = rememberDialogState(width = 800.dp, height = 400.dp),
        resizable = false,
    ) {
        AppTheme {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: UpdaterDialogState) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 45.dp, vertical = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = string(Strings.UpdaterDialogCurrentVersionLabel, appVersion.toString()),
                    style = MaterialTheme.typography.body2,
                )
                Text(
                    text = string(
                        Strings.UpdaterDialogLatestVersionLabel,
                        state.update.version.toString(),
                        state.update.date,
                    ),
                    style = MaterialTheme.typography.body2,
                )
                Spacer(modifier = Modifier.height(10.dp))
                SummaryBox(state)
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(20.dp))
                ButtonBar(state)
            }
        }
    }
}

@Composable
private fun ColumnScope.SummaryBox(state: UpdaterDialogState) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier.weight(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(15.dp),
    ) {
        val (text, tag) = state.getDiffSummary(
            textColor = MaterialTheme.colors.onBackground,
            linkColor = MaterialTheme.colors.primary,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            ClickableText(
                text = text,
                style = MaterialTheme.typography.caption,
                onClick = { offset ->
                    text.getStringAnnotations(
                        tag = tag,
                        start = offset,
                        end = offset,
                    ).firstOrNull()?.let { annotation ->
                        val url = URI(annotation.item)
                        Desktop.getDesktop().browse(url)
                    }
                },
            )
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ButtonBar(state: UpdaterDialogState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { state.cancelIgnored() },
        ) {
            Text(string(Strings.UpdaterDialogIgnoreButton))
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { state.cancel() },
        ) {
            Text(string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        Button(
            enabled = state.isDownloading.not(),
            onClick = { state.startDownload() },
        ) {
            Text(string(Strings.UpdaterDialogStartDownloadButton))
        }
    }
}
