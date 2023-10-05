package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogArgs
import com.sdercolin.vlabeler.ui.editor.EditorState

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
    val isMultipleEditModeEnabled: Boolean,
    val isMultipleEditMode: Boolean,
    val toggleMultipleEditMode: () -> Unit,
    val appConf: AppConf,
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
    editorState: EditorState,
) = remember(
    project,
    project.labelerConf,
    project.currentModuleIndex,
    project.currentModule.currentIndexInFiltered,
    project.currentModule.filteredEntryCount,
    project.multipleEditMode,
    editorState,
    editorState.canvasResolution,
    appState,
    appState.appConf,
    appState.canGoNextEntryOrSample,
    appState.canGoPreviousEntryOrSample,
) {
    BottomBarState(
        currentEntryIndexInTotal = project.currentModule.currentIndexInFiltered,
        totalEntryCount = project.currentModule.filteredEntryCount,
        resolution = editorState.canvasResolution,
        onChangeResolution = { editorState.changeResolution(it) },
        openSetResolutionDialog = {
            appState.openEmbeddedDialog(
                SetResolutionDialogArgs(
                    current = editorState.canvasResolution,
                    min = appState.appConf.painter.canvasResolution.min,
                    max = appState.appConf.painter.canvasResolution.max,
                ),
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
        isMultipleEditModeEnabled = project.labelerConf.continuous,
        isMultipleEditMode = project.multipleEditMode,
        toggleMultipleEditMode = { appState.toggleMultipleEditMode(!project.multipleEditMode) },
        appConf = appState.appConf,
    )
}
