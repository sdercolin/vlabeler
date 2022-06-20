@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.shouldDecreaseResolution
import com.sdercolin.vlabeler.env.shouldIncreaseResolution
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class EditedEntry(
    val entry: Entry,
    val sampleName: String,
    val index: Int
)

@Composable
fun Editor(
    project: Project,
    editProject: (Project) -> Unit,
    editEntry: (EditedEntry) -> Unit,
    showDialog: (EmbeddedDialogArgs) -> Unit,
    appConf: AppConf,
    labelerState: MutableState<LabelerState>,
    appState: MutableState<AppState>,
    playerState: PlayerState,
    keyboardViewModel: KeyboardViewModel,
    player: Player
) {
    val sampleState = produceState(initialValue = null as Sample?, project.currentSampleName, appConf) {
        value = withContext(Dispatchers.IO) {
            loadSampleFile(project.currentSampleFile, appConf)
        }
    }
    val isLoading by remember { derivedStateOf { sampleState.value == null } }
    val editedEntryState = remember { mutableStateOf(project.getEntryForEditing()) }
    val keyboardState by keyboardViewModel.keyboardStateFlow.collectAsState()
    val sample = sampleState.value

    LaunchSwitchEntryFromUpstreamState(project, appState, editEntry, editedEntryState)
    LaunchSubmitEditedEntryWhenSaveRequested(appState, editEntry, editedEntryState)
    LaunchChangeResolutionByKeyEvent(keyboardViewModel, appState, appConf, labelerState)
    LaunchLoadSampleByPlayer(sample, player)

    Box(
        Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) {
                if (appState.value.isEditorActive.not()) return@onPointerEvent
                if (switchEntryByPointerEvent(it, keyboardState, project, editProject)) return@onPointerEvent
                changeResolutionByPointerEvent(it, appConf, keyboardState, labelerState)
            }
    ) {
        Labeler(
            sample = sample,
            sampleName = project.currentSampleName,
            entry = editedEntryState.value.entry,
            currentEntryIndexInTotal = project.currentEntryIndexInTotal,
            totalEntryCount = project.totalEntryCount,
            editEntry = {
                editedEntryState.update { copy(entry = it) }
                appState.update { copy(hasEditedEntry = true) }
            },
            playSampleSection = player::playSection,
            showDialog = showDialog,
            appConf = appConf,
            labelerConf = project.labelerConf,
            labelerState = labelerState,
            playerState = playerState,
            keyboardViewModel = keyboardViewModel
        )
    }
    if (isLoading) {
        CircularProgress()
    }
}

@Composable
private fun LaunchSwitchEntryFromUpstreamState(
    project: Project,
    appState: MutableState<AppState>,
    editEntry: (EditedEntry) -> Unit,
    editedEntryState: MutableState<EditedEntry>
) {
    LaunchedEffect(project.currentSampleName, project.currentEntryIndex) {
        // when switched to a new entry, merge the edited entry and load the new one
        val edited = appState.value.hasEditedEntry
        if (edited) {
            println("Previous entry merged")
            editEntry(editedEntryState.value)
        }
        println("Entry loaded")
        editedEntryState.value = project.getEntryForEditing()
        if (edited) appState.update { copy(hasEditedEntry = false) }
    }
}

@Composable
private fun LaunchSubmitEditedEntryWhenSaveRequested(
    appState: MutableState<AppState>,
    editEntry: (EditedEntry) -> Unit,
    editedEntryState: MutableState<EditedEntry>
) {
    LaunchedEffect(appState.value.projectWriteStatus) {
        if (appState.value.projectWriteStatus != AppState.ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        if (appState.value.hasEditedEntry.not()) return@LaunchedEffect
        // when saving is requested, merge the edited entry first
        println("Entry Merged")
        editEntry(editedEntryState.value)
        appState.update { copy(hasEditedEntry = false) }
    }
}

private fun switchEntryByPointerEvent(
    event: PointerEvent,
    keyboardState: KeyboardState,
    project: Project,
    editProject: (Project) -> Unit
): Boolean {
    val yDelta = event.changes.first().scrollDelta.y
    val shouldSwitchSample = keyboardState.isCtrlPressed
    val updatedProject = when {
        yDelta > 0 -> if (shouldSwitchSample) project.nextSample() else project.nextEntry()
        yDelta < 0 -> if (shouldSwitchSample) project.previousSample() else project.previousEntry()
        else -> null
    }
    if (updatedProject != null) {
        editProject(updatedProject)
        return true
    }
    return false
}

private fun changeResolutionByPointerEvent(
    event: PointerEvent,
    appConf: AppConf,
    keyboardState: KeyboardState,
    labelerState: MutableState<LabelerState>
) {
    if (!keyboardState.isCtrlPressed) return
    val xDelta = event.changes.first().scrollDelta.x
    val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    val resolution = labelerState.value.canvasResolution
    val updatedResolution = when {
        xDelta > 0 -> range.decreaseFrom(resolution).takeIf { (range.canDecrease(resolution)) }
        xDelta < 0 -> range.increaseFrom(resolution).takeIf { (range.canIncrease(resolution)) }
        else -> null
    }
    if (updatedResolution != null) labelerState.update { copy(canvasResolution = updatedResolution) }
}

@Composable
private fun LaunchChangeResolutionByKeyEvent(
    keyboardViewModel: KeyboardViewModel,
    appState: MutableState<AppState>,
    appConf: AppConf,
    labelerState: MutableState<LabelerState>
) {
    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect {
            if (appState.value.isEditorActive.not()) return@collect
            changeResolutionByKeyEvent(it, appConf, labelerState)
        }
    }
}

private fun changeResolutionByKeyEvent(
    event: KeyEvent,
    appConf: AppConf,
    labelerState: MutableState<LabelerState>
) {
    val resolution = labelerState.value.canvasResolution
    val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    val updatedResolution = if (event.shouldIncreaseResolution) range.increaseFrom(resolution)
    else if (event.shouldDecreaseResolution) range.decreaseFrom(resolution)
    else null
    if (updatedResolution != null) labelerState.update { copy(canvasResolution = updatedResolution) }
}

@Composable
private fun LaunchLoadSampleByPlayer(
    sample: Sample?,
    player: Player
) {
    LaunchedEffect(sample) {
        if (sample != null) player.load(sample.info.file)
    }
}
