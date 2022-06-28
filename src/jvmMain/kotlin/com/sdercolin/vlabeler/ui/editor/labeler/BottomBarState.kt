package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogArgs

class BottomBarState(
    val currentEntryIndexInTotal: Int,
    val totalEntryCount: Int,
    val resolution: Int,
    val onChangeResolution: (Int) -> Unit,
    val openSetResolutionDialog: () -> Unit,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val goNextEntry: () -> Unit,
    val goPreviousEntry: () -> Unit,
    val goNextSample: () -> Unit,
    val goPreviousSample: () -> Unit,
    val openJumpToEntryDialog: () -> Unit,
    val scrollFit: () -> Unit,
    val appConf: AppConf
) {
    private val resolutionRange = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    val canIncrease get() = resolutionRange.canIncrease(resolution)
    fun increase() = onChangeResolution(resolutionRange.increaseFrom(resolution))
    val canDecrease get() = resolutionRange.canDecrease(resolution)
    fun decrease() = onChangeResolution(resolutionRange.decreaseFrom(resolution))
}

@Composable
fun rememberBottomBarState(
    project: Project,
    appState: AppState,
    labelerState: LabelerState
) = remember(
    project.currentEntryIndexInTotal,
    project.totalEntryCount,
    labelerState.canvasResolution,
    appState.appConf,
    appState.canGoNextEntryOrSample,
    appState.canGoPreviousEntryOrSample,
    appState.appConf
) {
    BottomBarState(
        currentEntryIndexInTotal = project.currentEntryIndexInTotal,
        totalEntryCount = project.totalEntryCount,
        resolution = labelerState.canvasResolution,
        onChangeResolution = { labelerState.changeResolution(it) },
        openSetResolutionDialog = {
            appState.openEmbeddedDialog(
                SetResolutionDialogArgs(
                    current = labelerState.canvasResolution,
                    min = appState.appConf.painter.canvasResolution.min,
                    max = appState.appConf.painter.canvasResolution.max
                )
            )
        },
        canGoNext = appState.canGoNextEntryOrSample,
        canGoPrevious = appState.canGoPreviousEntryOrSample,
        goNextEntry = { appState.nextEntry() },
        goPreviousEntry = { appState.previousEntry() },
        goNextSample = { appState.nextSample() },
        goPreviousSample = { appState.previousSample() },
        openJumpToEntryDialog = { appState.openJumpToEntryDialog() },
        scrollFit = { appState.scrollFitViewModel.emit() },
        appConf = appState.appConf
    )
}
