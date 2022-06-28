package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogArgs
import com.sdercolin.vlabeler.ui.theme.Black50
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun Labeler(
    sample: Sample?,
    project: Project,
    entry: Entry,
    editEntry: (Entry) -> Unit,
    submitEntry: () -> Unit,
    labelerState: LabelerState,
    appState: AppState,
) {
    val isBusy = sample == null

    val scope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState(0)
    val currentResolution = labelerState.canvasResolution

    LaunchedEffect(Unit) {
        appState.scrollFitViewModel.eventFlow.collectLatest {
            horizontalScrollState.animateScrollTo(it)
        }
    }

    Column(Modifier.fillMaxSize()) {
        EntryTitleBar(
            entryName = entry.name,
            sampleName = project.currentSampleName,
            openEditEntryNameDialog = {
                appState.openEditEntryNameDialog(
                    duplicate = false,
                    showSnackbar = {
                        scope.launch { appState.snackbarHostState.showSnackbar(it) }
                    }
                )
            }
        )
        Box(Modifier.fillMaxWidth().weight(1f).border(width = 0.5.dp, color = Black50)) {
            Canvas(
                sample = sample,
                entry = entry,
                entriesInSample = project.entriesInCurrentSample,
                currentIndexInSample = project.currentEntryIndex,
                isBusy = isBusy,
                editEntry = editEntry,
                submitEntry = submitEntry,
                labelerConf = project.labelerConf,
                resolution = currentResolution,
                horizontalScrollState = horizontalScrollState,
                appState = appState
            )
        }
        HorizontalScrollbar(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            adapter = rememberScrollbarAdapter(horizontalScrollState)
        )
        BottomBar(
            currentEntryIndexInTotal = project.currentEntryIndexInTotal,
            totalEntryCount = project.totalEntryCount,
            resolution = currentResolution,
            onChangeResolution = { labelerState.changeResolution(it) },
            openSetResolutionDialog = {
                appState.openEmbeddedDialog(
                    SetResolutionDialogArgs(
                        current = currentResolution,
                        min = appState.appConf.painter.canvasResolution.min,
                        max = appState.appConf.painter.canvasResolution.max
                    )
                )
            },
            canGoNext = appState.canGoNextEntryOrSample,
            canGoPrevious = appState.canGoPreviousEntryOrSample,
            goNextEntry = {
                if (appState.nextEntry()) {
                    appState.scrollFitViewModel.emitNext()
                }
            },
            goPreviousEntry = {
                if (appState.previousEntry()) {
                    appState.scrollFitViewModel.emitNext()
                }
            },
            goNextSample = {
                appState.nextSample()
                appState.scrollFitViewModel.emitNext()
            },
            goPreviousSample = {
                appState.previousSample()
                appState.scrollFitViewModel.emitNext()
            },
            openJumpToEntryDialog = { appState.openJumpToEntryDialog() },
            scrollFit = { appState.scrollFitViewModel.emit() },
            appConf = appState.appConf
        )
    }
}

@Composable
private fun EntryTitleBar(entryName: String, sampleName: String, openEditEntryNameDialog: () -> Unit) {
    Surface {
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .background(color = MaterialTheme.colors.surface)
                .padding(horizontal = 20.dp)
        ) {
            Row(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    modifier = Modifier.alignByBaseline()
                        .clickable { openEditEntryNameDialog() },
                    text = entryName,
                    style = MaterialTheme.typography.h3,
                    maxLines = 1
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "（$sampleName）",
                    style = MaterialTheme.typography.h5,
                    maxLines = 1
                )
            }
        }
    }
}
