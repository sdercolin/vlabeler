@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor

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
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.shouldDecreaseResolution
import com.sdercolin.vlabeler.env.shouldIncreaseResolution
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.Labeler
import com.sdercolin.vlabeler.ui.editor.labeler.LabelerState
import com.sdercolin.vlabeler.ui.editor.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class EditedEntry(
    val entry: Entry,
    val sampleName: String,
    val index: Int
) {
    fun edit(entry: Entry) = copy(entry = entry)
}

@Composable
fun Editor(
    project: Project,
    labelerState: LabelerState,
    appState: AppState
) {
    val sampleState = produceState(initialValue = null as Sample?, project.currentSampleName, appState.appConf) {
        value = withContext(Dispatchers.IO) {
            loadSampleFile(project.currentSampleFile, appState.appConf)
        }
    }
    val isLoading by remember { derivedStateOf { sampleState.value == null } }
    val editedEntryState = remember { mutableStateOf(project.getEntryForEditing()) }
    val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()
    val sample = sampleState.value

    val editProject: (Project) -> Unit = { appState.editProject { it } }
    val submitEntry = {
        if (editedEntryState.value.entry != project.currentEntry) {
            Log.info("Submit entry: ${editedEntryState.value}")
            appState.editEntry(editedEntryState.value)
        }
    }

    LaunchSwitchEntryFromUpstreamState(project, editedEntryState)
    LaunchChangeResolutionByKeyEvent(appState.keyboardViewModel, appState, labelerState)
    LaunchLoadSampleByPlayer(sample, appState.player)

    Box(
        Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) {
                if (appState.isEditorActive.not()) return@onPointerEvent
                if (switchEntryByPointerEvent(it, keyboardState, project, editProject, appState.scrollFitViewModel)) {
                    return@onPointerEvent
                }
                changeResolutionByPointerEvent(it, appState.appConf, keyboardState, labelerState)
            }
    ) {
        Labeler(
            sample = sample,
            project = project,
            entry = editedEntryState.value.entry,
            editEntry = { editedEntryState.update { edit(it) } },
            submitEntry = submitEntry,
            labelerState = labelerState,
            appState = appState
        )
    }
    if (isLoading) {
        CircularProgress()
    }
}

@Composable
private fun LaunchSwitchEntryFromUpstreamState(
    project: Project,
    editedEntryState: MutableState<EditedEntry>
) {
    LaunchedEffect(project.currentEntryIndexInTotal, project.currentEntry) {
        val newValue = project.getEntryForEditing()
        if (newValue != editedEntryState.value) {
            Log.info("Load new entry: $newValue")
            editedEntryState.value = newValue
        }
    }
}

private fun switchEntryByPointerEvent(
    event: PointerEvent,
    keyboardState: KeyboardState,
    project: Project,
    editProject: (Project) -> Unit,
    scrollFitViewModel: ScrollFitViewModel
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
        if (updatedProject.hasSwitchedSample(project)) {
            scrollFitViewModel.emitNext()
        }
        return true
    }
    return false
}

private fun changeResolutionByPointerEvent(
    event: PointerEvent,
    appConf: AppConf,
    keyboardState: KeyboardState,
    labelerState: LabelerState
) {
    if (!keyboardState.isCtrlPressed) return
    val xDelta = event.changes.first().scrollDelta.x
    val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    val resolution = labelerState.canvasResolution
    val updatedResolution = when {
        xDelta > 0 -> range.decreaseFrom(resolution).takeIf { (range.canDecrease(resolution)) }
        xDelta < 0 -> range.increaseFrom(resolution).takeIf { (range.canIncrease(resolution)) }
        else -> null
    }
    if (updatedResolution != null) labelerState.changeResolution(updatedResolution)
}

@Composable
private fun LaunchChangeResolutionByKeyEvent(
    keyboardViewModel: KeyboardViewModel,
    appState: AppState,
    labelerState: LabelerState
) {
    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect {
            if (appState.isEditorActive.not()) return@collect
            changeResolutionByKeyEvent(it, appState.appConf, labelerState)
        }
    }
}

private fun changeResolutionByKeyEvent(
    event: KeyEvent,
    appConf: AppConf,
    labelerState: LabelerState
) {
    val resolution = labelerState.canvasResolution
    val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    val updatedResolution = if (event.shouldIncreaseResolution) range.increaseFrom(resolution)
    else if (event.shouldDecreaseResolution) range.decreaseFrom(resolution)
    else null
    if (updatedResolution != null) labelerState.changeResolution(updatedResolution)
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
