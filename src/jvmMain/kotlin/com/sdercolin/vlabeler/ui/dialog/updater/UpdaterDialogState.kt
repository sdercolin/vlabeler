package com.sdercolin.vlabeler.ui.dialog.updater

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.sdercolin.vlabeler.repository.update.UpdateRepository
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File

class UpdaterDialogState(
    val update: Update,
    private val scope: CoroutineScope,
    private val appRecordStore: AppRecordStore,
    private val onError: (Throwable) -> Unit,
    private val finish: () -> Unit,
) {

    private val repository = UpdateRepository()
    private var downloadJob: Job? = null

    private var downloadDirectory: File by mutableStateOf(appRecordStore.value.updateDownloadDirectory.toFile())

    private val downloadFile: File get() = File(downloadDirectory, update.fileName)

    var isDownloading: Boolean by mutableStateOf(false)
        private set

    var progress: Float by mutableStateOf(0f)
        private set

    fun getDiffSummary(textColor: Color, linkColor: Color): Pair<AnnotatedString, String> {
        val tag = "link"
        val text = buildAnnotatedString {
            for (summary in update.diff) {
                if (length > 0) append("\n\n")
                val versionText = summary.version.toString()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                    append(versionText)
                }
                withStyle(SpanStyle(color = textColor)) {
                    append(" (${summary.date})  ")
                }

                pushStringAnnotation(
                    tag = "link",
                    annotation = summary.pageUrl,
                )
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(string(Strings.UpdaterDialogSummaryDetailsLink))
                }
                pop()
            }
        }
        return text to tag
    }

    fun cancel() {
        scope.launch {
            downloadJob?.takeIf { it.isActive }?.let {
                it.cancelAndJoin()
                downloadFile.delete()
            }
            finish()
        }
    }

    fun cancelIgnored() {
        appRecordStore.update { versionIgnored(update.version) }
        cancel()
    }

    fun startDownload() {
        if (downloadJob != null) return
        isDownloading = true
        downloadJob = scope.launch(Dispatchers.IO) {
            repository.downloadUpdate(downloadFile, update.assetUrl, onProgress = { progress = it })
                .onFailure {
                    downloadFile.delete()
                    onError(it)
                    return@launch
                }
            Desktop.getDesktop().open(downloadDirectory)
            finish()
        }
    }
}
