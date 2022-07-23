package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.shouldDecreaseResolution
import com.sdercolin.vlabeler.env.shouldIncreaseResolution
import com.sdercolin.vlabeler.exception.MissingSampleDirectoryException
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.SampleRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class EditorState(
    project: Project,
    private val appState: AppState
) {
    private val sampleInfoState: MutableState<Result<SampleInfo>?> = mutableStateOf(null)
    val sampleInfoResult get() = sampleInfoState.value
    val isLoading get() = sampleInfoState.value == null
    var project: Project by mutableStateOf(project)
    var editedEntries: List<IndexedEntry> by mutableStateOf(project.getEntriesForEditing())
    private val isActive get() = appState.isEditorActive
    private val appConf = appState.appConf
    val keyboardViewModel = appState.keyboardViewModel
    val scrollFitViewModel = appState.scrollFitViewModel
    private val player = appState.player
    var tool: Tool by mutableStateOf(Tool.Cursor)

    private var _renderProgress: Pair<Int, Int> by mutableStateOf(0 to 0)
    val renderProgress get() = _renderProgress
    private val renderProgressMutex = Mutex()

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

    val chartStore = ChartStore()

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
        val sample = sampleInfoResult?.getOrNull() ?: return
        appState.requestCutEntry(index, position, player, sample)
    }

    private fun loadNewEntries() {
        val newValues = project.getEntriesForEditing()
        if (newValues != editedEntries) {
            Log.info("Load new entries: $newValues")
            editedEntries = newValues
        }
    }

    suspend fun loadSample() {
        withContext(Dispatchers.IO) {
            val sampleDirectory = project.sampleDirectory.toFile()
            if (!sampleDirectory.exists()) {
                sampleInfoState.value = Result.failure(MissingSampleDirectoryException())
                appState.confirmIfRedirectSampleDirectory(sampleDirectory)
                return@withContext
            }
            val sampleInfo = SampleRepository.load(project.currentSampleFile, appConf)
            sampleInfoState.value = sampleInfo
            sampleInfo.getOrElse {
                Log.error(it)
                null
            }?.let {
                player.load(File(it.file))
                appState.updateProjectOnLoadedSample(it)
                val renderProgressTotal = it.chunkCount * (it.channels + if (it.hasSpectrogram) 1 else 0)
                _renderProgress = 0 to renderProgressTotal
            }
        }
    }

    fun renderCharts(
        scope: CoroutineScope,
        chunkCount: Int,
        sampleInfo: SampleInfo,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        chartStore.clear()
        val chunkSizeInMilliSec = sampleInfo.lengthMillis / chunkCount
        val startingChunkIndex = (project.currentEntry.start / chunkSizeInMilliSec).toInt()

        val onRenderProgress = suspend {
            renderProgressMutex.withLock {
                _renderProgress = _renderProgress.copy(first = _renderProgress.first + 1)
            }
        }
        chartStore.load(
            scope,
            project,
            chunkCount,
            sampleInfo,
            appConf,
            density,
            layoutDirection,
            startingChunkIndex,
            onRenderProgress
        )
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

    fun createDefaultEntry(sampleName: String) {
        appState.createDefaultEntry(sampleName)
    }

    fun jumpToEntry(index: Int) {
        appState.jumpToEntry(index)
    }

    fun requestRedirectSampleDirectory() {
        appState.openSampleDirectoryRedirectDialog()
    }

    fun clear() {
        Log.info("EditorState clear()")
        SampleRepository.clear()
        chartStore.clear()
        player.close()
    }
}
