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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.asPathRelativeToHome

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
    appConf: AppConf,
    update: Update,
    appRecordStore: AppRecordStore,
    onError: (Throwable) -> Unit,
    finish: () -> Unit,
    state: UpdaterDialogState = rememberUpdaterDialogState(update, appRecordStore, onError, finish),
) {
    DialogWindow(
        title = string(Strings.UpdaterDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = { state.cancel() },
        state = rememberDialogState(width = 800.dp, height = 400.dp),
        resizable = false,
    ) {
        AppTheme(appConf.view) {
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
                DownloadPositionRow(state)
                Spacer(modifier = Modifier.height(10.dp))
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
    if (state.isShowingChoosingDownloadPositionDialog) {
        OpenFileDialog(
            title = string(Strings.UpdaterDialogChooseDownloadPositionDialogTitle),
            initialDirectory = state.downloadDirectory.absolutePath,
            directoryMode = true,
        ) { parent, name ->
            state.handleChoosingDownloadPositionDialogResult(parent, name)
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
                        Url.open(annotation.item)
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
private fun DownloadPositionRow(state: UpdaterDialogState) {
    Row(Modifier.fillMaxWidth()) {
        BasicText(
            modifier = Modifier.padding(vertical = 5.dp).alignByBaseline(),
            text = string(Strings.UpdaterDialogDownloadPositionLabel),
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onBackground,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(5.dp))
        BasicTextField(
            modifier = Modifier.alignByBaseline()
                .weight(1f)
                .background(color = White20, shape = RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            value = state.downloadDirectory.absolutePath.asPathRelativeToHome(),
            onValueChange = {},
            textStyle = MaterialTheme.typography.caption.copy(
                color = if (state.isDownloadPositionValid) {
                    MaterialTheme.colors.onBackground
                } else {
                    MaterialTheme.colors.error
                },
            ),
            readOnly = true,
        )
        Spacer(Modifier.width(20.dp))
        SingleClickableText(
            modifier = Modifier.alignByBaseline(),
            text = string(Strings.UpdaterDialogChangeDownloadPositionButton),
            style = MaterialTheme.typography.caption,
            onClick = { state.openChooseDownloadPositionDialog() },
            enabled = state.isDownloading.not(),
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
            enabled = state.isDownloading.not() && state.isDownloadPositionValid,
            onClick = { state.startDownload() },
        ) {
            Text(string(Strings.UpdaterDialogStartDownloadButton))
        }
    }
}
