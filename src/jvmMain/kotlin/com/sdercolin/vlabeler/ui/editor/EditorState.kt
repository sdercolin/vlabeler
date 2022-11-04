package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.MissingSampleDirectoryException
import com.sdercolin.vlabeler.io.getPropertyValue
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.runIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EditorState(
    project: Project,
    private val appState: AppState,
) {
    private val sampleInfoState: MutableState<Result<SampleInfo>?> = mutableStateOf(null)
    val sampleInfoResult get() = sampleInfoState.value
    val isLoading get() = sampleInfoState.value == null
    var project: Project by mutableStateOf(project)
    var editedEntries: List<IndexedEntry> by mutableStateOf(project.getEntriesForEditing().second)
    private val isActive get() = appState.isEditorActive
    private val appConf get() = appState.appConf
    val keyboardViewModel = appState.keyboardViewModel
    val scrollFitViewModel = appState.scrollFitViewModel
    private val player get() = appState.player
    var tool: Tool by mutableStateOf(Tool.Cursor)

    var isPinnedEntryListInputFocused: Boolean by mutableStateOf(false)
    var isEditingTag: Boolean by mutableStateOf(false)

    private var _renderProgress: Pair<Int, Int> by mutableStateOf(0 to 0)
    val renderProgress get() = _renderProgress
    private val renderProgressMutex = Mutex()

    var canvasResolution: Int by mutableStateOf(appState.appConf.painter.canvasResolution.default)
        private set

    val scrollOnResolutionChangeViewModel = ScrollOnResolutionChangeViewModel()

    val entryTitle: String
        get() = project.currentEntry.name

    @Composable
    fun getEntrySubTitle(): String {
        val currentSampleName = project.currentSampleName.runIf(appConf.view.hideSampleExtension) {
            substringBeforeLast('.')
        }
        return if (editedEntries.size == 1) {
            currentSampleName
        } else {
            string(Strings.EditorSubTitleMultiple, editedEntries.size, currentSampleName)
        }
    }

    val entryStar: Boolean
        get() = project.currentEntry.notes.star

    val entryDone: Boolean
        get() = project.currentEntry.notes.done

    val entryTag: String
        get() = project.currentEntry.notes.tag

    val tagOptions
        get() = project.currentModule.entries
            .mapNotNull { it.notes.tag.ifEmpty { null } }
            .distinct()
            .sorted()

    val chartStore = ChartStore()

    val pinnedEntryListFilterState = LinkableEntryListFilterState(
        project = project,
        submitFilter = {
            appState.editProject { updateEntryFilter(it) }
        },
    )

    private val editedIndexes = mutableSetOf<Int>()

    /**
     * Called from upstream
     */
    fun updateProject(newProject: Project) {
        val previous = project
        project = newProject
        pinnedEntryListFilterState.updateProject(newProject)
        if (newProject.getEntriesForEditing() != previous.getEntriesForEditing()) {
            loadNewEntries()
        }
    }

    fun submitEntries() {
        val changedEntries = editedEntries - project.getEntriesForEditing().second.toSet()
        if (changedEntries.isNotEmpty()) {
            Log.info("Submit entries: $changedEntries")
            appState.editEntries(changedEntries, editedIndexes)
            editedIndexes.clear()
        }
    }

    fun submitEntries(editedEntries: List<IndexedEntry>, editedIndexes: Set<Int>) {
        updateEntries(editedEntries, editedIndexes)
        submitEntries()
    }

    fun updateEntries(editedEntries: List<IndexedEntry>, editedIndexes: Set<Int>) {
        this.editedIndexes.addAll(editedIndexes)
        this.editedEntries = editedEntries
    }

    fun cutEntry(index: Int, position: Float) {
        val sample = sampleInfoResult?.getOrNull() ?: return
        appState.requestCutEntry(index, position, player, sample)
    }

    private fun loadNewEntries() {
        val newValues = project.getEntriesForEditing().second
        if (newValues != editedEntries) {
            Log.info("Load new entries: $newValues")
            editedIndexes.clear()
            editedEntries = newValues
        }
    }

    suspend fun loadSample(appConf: AppConf) {
        withContext(Dispatchers.IO) {
            val moduleName = project.currentModule.name
            val sampleDirectory = project.currentModule.getSampleDirectory(project)
            if (!sampleDirectory.exists()) {
                sampleInfoState.value = Result.failure(MissingSampleDirectoryException())
                appState.confirmIfRedirectSampleDirectory(sampleDirectory)
                return@withContext
            }

            val previousSampleInfo = sampleInfoState.value?.getOrNull()
            if (previousSampleInfo?.getFile(project)?.absolutePath != project.currentSampleFile.absolutePath) {
                sampleInfoState.value = null
            }
            val sampleInfo = SampleInfoRepository.load(project, project.currentSampleFile, moduleName, appConf)
            sampleInfoState.value = sampleInfo
            sampleInfo.getOrElse {
                Log.error(it)
                null
            }?.let {
                val updated = chartStore.prepareForNewLoading(project, appConf, it)
                appState.updateProjectOnLoadedSample(it, moduleName)
                if (updated) {
                    val renderProgressTotal = it.chunkCount * (it.channels + if (it.hasSpectrogram) 1 else 0)
                    _renderProgress = 0 to renderProgressTotal
                }
                player.load(it.getFile(project))
            }
        }
    }

    fun cancelLoading() {
        sampleInfoState.value = null
        _renderProgress = 0 to 0
    }

    fun renderCharts(
        scope: CoroutineScope,
        sampleInfo: SampleInfo,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        Log.info("Render charts")
        chartStore.clear()
        val chunkCount = sampleInfo.chunkCount
        val chunkSizeInMilliSec = sampleInfo.lengthMillis / chunkCount
        val startingChunkIndex = (project.currentEntry.start / chunkSizeInMilliSec).toInt().coerceAtMost(chunkCount - 1)

        val onRenderProgress = suspend {
            renderProgressMutex.withLock {
                _renderProgress = _renderProgress.copy(first = _renderProgress.first + 1)
            }
        }
        chartStore.load(
            scope,
            project,
            sampleInfo,
            appConf,
            density,
            layoutDirection,
            startingChunkIndex,
            onRenderProgress,
        )
    }

    fun changeResolution(resolution: Int) {
        canvasResolution = resolution
    }

    suspend fun updateResolution() {
        keyboardViewModel.keyboardActionFlow.collect {
            if (appState.isEditorActive.not()) return@collect
            updateResolutionByKeyAction(it, appConf)
        }
    }

    private fun updateResolutionByKeyAction(
        action: KeyAction,
        appConf: AppConf,
    ) {
        val resolution = canvasResolution
        val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
        val updatedResolution = when (action) {
            KeyAction.IncreaseResolution -> range.increaseFrom(resolution)
            KeyAction.DecreaseResolution -> range.decreaseFrom(resolution)
            else -> return
        }
        changeResolution(updatedResolution)
    }

    fun handlePointerEvent(
        event: PointerEvent,
        keyboardState: KeyboardState,
    ) {
        if (isActive.not()) return
        when (keyboardState.getEnabledMouseScrollAction(event)) {
            MouseScrollAction.GoToNextSample -> switchEntryByPointerEvent(
                shouldSwitchSample = true,
                positive = true,
            )
            MouseScrollAction.GoToPreviousSample -> switchEntryByPointerEvent(
                shouldSwitchSample = true,
                positive = false,
            )
            MouseScrollAction.GoToNextEntry -> switchEntryByPointerEvent(
                shouldSwitchSample = false,
                positive = true,
            )
            MouseScrollAction.GoToPreviousEntry -> switchEntryByPointerEvent(
                shouldSwitchSample = false,
                positive = false,
            )
            MouseScrollAction.ZoomInCanvas -> changeResolutionByPointerEvent(true)
            MouseScrollAction.ZoomOutCanvas -> changeResolutionByPointerEvent(false)
            else -> Unit
        }
    }

    private fun switchEntryByPointerEvent(
        shouldSwitchSample: Boolean,
        positive: Boolean,
    ): Boolean {
        when {
            positive -> if (shouldSwitchSample) appState.nextSample() else appState.nextEntry()
            else -> if (shouldSwitchSample) appState.previousSample() else appState.previousEntry()
        }
        return true
    }

    private fun changeResolutionByPointerEvent(positive: Boolean) {
        val range = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
        val resolution = canvasResolution
        val updatedResolution = when {
            positive -> range.decreaseFrom(resolution).takeIf { (range.canDecrease(resolution)) }
            else -> range.increaseFrom(resolution).takeIf { (range.canIncrease(resolution)) }
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

    fun toggleEntryDone(index: Int) {
        appState.toggleEntryDone(index)
    }

    fun toggleEntryStar(index: Int) {
        appState.toggleEntryStar(index)
    }

    fun editEntryTag(index: Int, tag: String) {
        appState.editEntryTag(index, tag)
    }

    fun jumpToModule(index: Int) {
        appState.jumpToModule(index)
    }

    fun jumpToModule(name: String, targetEntryIndex: Int? = null) {
        val index = project.modules.indexOfFirst { it.name == name }
        appState.jumpToModule(index, targetEntryIndex)
    }

    fun handleSetPropertyKeyAction(action: KeyAction): Boolean {
        val propertyShortcutIndex = when (action) {
            KeyAction.SetProperty1 -> 0
            KeyAction.SetProperty2 -> 1
            KeyAction.SetProperty3 -> 2
            KeyAction.SetProperty4 -> 3
            KeyAction.SetProperty5 -> 4
            KeyAction.SetProperty6 -> 5
            KeyAction.SetProperty7 -> 6
            KeyAction.SetProperty8 -> 7
            KeyAction.SetProperty9 -> 8
            KeyAction.SetProperty10 -> 9
            else -> return false
        }
        val property = project.labelerConf.properties
            .find { it.shortcutIndex == propertyShortcutIndex }
            ?: return false
        val js = JavaScript()
        val currentValue = project.labelerConf.getPropertyValue(
            property,
            project.currentEntry,
            js,
        )
        js.close()
        if (currentValue == null) return false
        val propertyIndex = project.labelerConf.properties.indexOf(property)
        appState.openSetPropertyValueDialog(propertyIndex, currentValue.toFloat())
        return true
    }

    fun clear() {
        Log.info("EditorState clear()")
        chartStore.clear()
        player.close()
    }
}
