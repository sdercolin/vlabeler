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
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorState(
    project: Project,
    private val appState: AppState
) {
    private val sampleState: MutableState<Result<Sample>?> = mutableStateOf(null)
    val sampleResult get() = sampleState.value
    val isLoading get() = sampleState.value == null
    var project: Project by mutableStateOf(project)
    var editedEntries: List<IndexedEntry> by mutableStateOf(project.getEntriesForEditing())
    private val isActive get() = appState.isEditorActive
    private val appConf = appState.appConf
    val keyboardViewModel = appState.keyboardViewModel
    val scrollFitViewModel = appState.scrollFitViewModel
    private val player = appState.player
    var tool: Tool by mutableStateOf(Tool.Cursor)

    var canvasResolution: Int by mutableStateOf(appState.appConf.painter.canvasResolution.default)
        private set

    val entryTitle: String
        get() = project.currentEntry.name

    val entrySubTitle: String
        get() = if (editedEntries.size == 1) {
            project.currentSampleName
        } else {
            string(Strings.EditorSubTitleMultiple, editedEntries.size, project.currentSampleName)
        }

    /**
     * Called from upstream
     */
    fun updateProject(newProject: Project) {
        val previous = project
        project = newProject
        if (newProject.getEntriesForEditing() != previous.getEntriesForEditing()) {
            loadNewEntries()
        }
    }

    fun submitEntries() {
        val changedEntries = editedEntries - project.getEntriesForEditing().toSet()
        if (changedEntries.isNotEmpty()) {
            Log.info("Submit entries: $changedEntries")
            appState.editEntries(changedEntries)
        }
    }

    fun updateEntries(editedEntries: List<IndexedEntry>) {
        this.editedEntries = editedEntries
    }

    fun cutEntry(index: Int, position: Float) {
        val sample = sampleResult?.getOrNull() ?: return
        appState.requestCutEntry(index, position, player, sample.info)
    }

    private fun loadNewEntries() {
        val newValues = project.getEntriesForEditing()
        if (newValues != editedEntries) {
            Log.info("Load new entries: $newValues")
            editedEntries = newValues
        }
    }

    suspend fun loadSampleFile() {
        withContext(Dispatchers.IO) {
            val sample = com.sdercolin.vlabeler.io.loadSampleFile(project.currentSampleFile, appConf)
            sampleState.value = sample
            sample.getOrNull()?.let {
                player.load(it.info.file)
            }
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
        when {
            yDelta > 0 -> if (shouldSwitchSample) appState.nextSample() else appState.nextEntry()
            yDelta < 0 -> if (shouldSwitchSample) appState.previousSample() else appState.previousEntry()
            else -> return false
        }
        return true
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

    fun openEditEntryNameDialog(index: Int, purpose: InputEntryNameDialogPurpose) {
        appState.openEditEntryNameDialog(index, purpose)
    }
}
