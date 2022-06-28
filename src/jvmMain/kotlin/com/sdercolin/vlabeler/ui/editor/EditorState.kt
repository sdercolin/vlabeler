package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.shouldDecreaseResolution
import com.sdercolin.vlabeler.env.shouldIncreaseResolution
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorState(
    project: Project,
    private val appState: AppState
) {
    private val sampleState: MutableState<Sample?> = mutableStateOf(null)
    val sample get() = sampleState.value
    val isLoading get() = sample == null
    var project: Project by mutableStateOf(project)
    var editedEntry: EditedEntry by mutableStateOf(project.getEntryForEditing())
    private val isActive get() = appState.isEditorActive
    private val appConf = appState.appConf
    val keyboardViewModel = appState.keyboardViewModel
    val scrollFitViewModel = appState.scrollFitViewModel
    private val player = appState.player

    var canvasResolution: Int by mutableStateOf(appState.appConf.painter.canvasResolution.default)
        private set

    /**
     * Called from upstream
     */
    fun updateProject(newProject: Project) {
        val previous = project
        project = newProject
        if (newProject.currentEntryIndexInTotal != previous.currentEntryIndexInTotal ||
            newProject.currentEntry != previous.currentEntry
        ) {
            loadNewEntry()
        }
    }

    fun submitEntry() {
        if (editedEntry.entry != project.currentEntry) {
            Log.info("Submit entry: $editedEntry")
            appState.editEntry(editedEntry)
        }
    }

    fun updateEntry(entry: Entry) {
        editedEntry = editedEntry.edit(entry)
    }

    private fun loadNewEntry() {
        val newValue = project.getEntryForEditing()
        if (newValue != editedEntry) {
            Log.info("Load new entry: $newValue")
            editedEntry = newValue
        }
    }

    suspend fun loadSampleFile() {
        withContext(Dispatchers.IO) {
            val sample = com.sdercolin.vlabeler.io.loadSampleFile(project.currentSampleFile, appConf)
            sampleState.value = sample
            player.load(sample.info.file)
        }
    }

    fun changeResolution(resolution: Int) {
        canvasResolution = resolution
    }

    suspend fun updateResolution() {
        keyboardViewModel.keyboardEventFlow.collect {
            if (appState.isEditorActive.not()) return@collect
            updateResolutionByKeyEvent(it, appConf)
        }
    }

    private fun updateResolutionByKeyEvent(
        event: KeyEvent,
        appConf: AppConf
    ) {
        val resolution = canvasResolution
        val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
        val updatedResolution = if (event.shouldIncreaseResolution) range.increaseFrom(resolution)
        else if (event.shouldDecreaseResolution) range.decreaseFrom(resolution)
        else null
        if (updatedResolution != null) changeResolution(updatedResolution)
    }

    fun handlePointerEvent(
        event: PointerEvent,
        keyboardState: KeyboardState
    ) {
        if (isActive.not()) return
        if (switchEntryByPointerEvent(event, keyboardState)) {
            return
        }
        changeResolutionByPointerEvent(event, keyboardState)
    }

    private fun switchEntryByPointerEvent(
        event: PointerEvent,
        keyboardState: KeyboardState
    ): Boolean {
        val yDelta = event.changes.first().scrollDelta.y
        val shouldSwitchSample = keyboardState.isCtrlPressed
        val updatedProject = when {
            yDelta > 0 -> if (shouldSwitchSample) project.nextSample() else project.nextEntry()
            yDelta < 0 -> if (shouldSwitchSample) project.previousSample() else project.previousEntry()
            else -> null
        }

        if (updatedProject != null) {
            val previousProject = project
            appState.editProject { updatedProject }
            if (project.hasSwitchedSample(previousProject)) {
                scrollFitViewModel.emitNext()
            }
            return true
        }
        return false
    }

    private fun changeResolutionByPointerEvent(
        event: PointerEvent,
        keyboardState: KeyboardState
    ) {
        if (!keyboardState.isCtrlPressed) return
        val xDelta = event.changes.first().scrollDelta.x
        val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
        val resolution = canvasResolution
        val updatedResolution = when {
            xDelta > 0 -> range.decreaseFrom(resolution).takeIf { (range.canDecrease(resolution)) }
            xDelta < 0 -> range.increaseFrom(resolution).takeIf { (range.canIncrease(resolution)) }
            else -> null
        }
        if (updatedResolution != null) changeResolution(updatedResolution)
    }

    fun openEditEntryNameDialog(duplicate: Boolean, scope: CoroutineScope) {
        appState.openEditEntryNameDialog(duplicate, scope)
    }
}
