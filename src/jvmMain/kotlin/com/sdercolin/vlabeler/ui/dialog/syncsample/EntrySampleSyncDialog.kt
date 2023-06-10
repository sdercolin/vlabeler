package com.sdercolin.vlabeler.ui.dialog.syncsample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.SmallDialogContainer
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Composable
fun EntrySampleSyncDialog(
    appConf: AppConf,
    projectStore: ProjectStore,
    onError: (Throwable) -> Unit,
    finish: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var progress by remember {
        mutableStateOf(
            EntrySampleSyncer.Progress(
                0,
                projectStore.requireProject().modules.size,
                0,
                0,
            ),
        )
    }

    LaunchedEffect(Unit) {
        EntrySampleSyncer(
            scope,
            projectStore,
            onError,
            onProgress = { progress = it },
        ).sync(
            project = projectStore.requireProject(),
            appConf = appConf,
        )
    }

    SmallDialogContainer(wrapHeight = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 40.dp)) {
            Spacer(Modifier.height(20.dp))
            Content(progress)
            Spacer(Modifier.height(30.dp))
            ButtonBar(isCompleted = progress.finished, finish = finish)
        }
    }
}

@Composable
private fun Content(progress: EntrySampleSyncer.Progress) {
    val moduleProgressText = if (progress.finished) {
        string(Strings.EntrySampleSyncerModuleTextFinished, progress.finishedModules, progress.totalModules)
    } else {
        string(Strings.EntrySampleSyncerModuleText, progress.finishedModules, progress.totalModules)
    }
    val sampleProgressText = if (progress.finishedInModule) {
        string(Strings.EntrySampleSyncerSampleTextFinished, progress.finishedFiles, progress.totalFiles)
    } else {
        string(Strings.EntrySampleSyncerSampleText, progress.finishedFiles, progress.totalFiles)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (progress.totalModules > 1) {
            Text(text = moduleProgressText, maxLines = 1, style = MaterialTheme.typography.body2)
            LinearProgressIndicator(
                progress = progress.moduleProgress,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(text = sampleProgressText, maxLines = 1, style = MaterialTheme.typography.body2)
        LinearProgressIndicator(
            progress = progress.fileProgress,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ButtonBar(isCompleted: Boolean, finish: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { finish() },
        ) {
            Text(string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(onClick = finish, enabled = isCompleted)
    }
}
