package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.audio.conversion.WaveConverterException
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
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
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasState
import com.sdercolin.vlabeler.ui.editor.labeler.ScreenRangeHelper
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.getDefaultNewEntryName
import com.sdercolin.vlabeler.util.groupContinuouslyBy
import com.sdercolin.vlabeler.util.runIf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class EditorState(
    project: Project,
    private val appState: AppState,
) {
    var canvasState by mutableStateOf<CanvasState>(CanvasState.Loading)
        private set
    val isLoading get() = canvasState is CanvasState.Loading
    val isError get() = canvasState is CanvasState.Error
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

    private var screenRangeHolder = ScreenRangeHelper()

    fun getSampleInfo(): SampleInfo? = (canvasState as? CanvasState.Loaded)?.sampleInfo

    private fun getCanvasParams(sampleInfo: SampleInfo) = CanvasParams(
        dataLength = sampleInfo.length,
        chunkCount = sampleInfo.chunkCount,
        resolution = canvasResolution,
    )

    fun getScreenRange(canvasLength: Float, scrollState: ScrollState): FloatRange? =
        screenRangeHolder.get(canvasLength, scrollState)

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

    private val editions = mutableMapOf<Int, Edition>()

    val onScreenScissorsState = OnScreenScissorsState(this)

    val canUseOnScreenScissors: Boolean
        get() = appConf.editor.useOnScreenScissors && project.multipleEditMode

    class OnScreenScissorsState(val editorState: EditorState) {
        var isOn: Boolean by mutableStateOf(false)
        var entryIndex: Int by mutableStateOf(-1)
        var timePosition: Float by mutableStateOf(0f)
        var pixelPosition: Float by mutableStateOf(0f)
        var text: String by mutableStateOf("")

        fun start(entryIndex: Int, timePosition: Float, pixelPosition: Float, initialText: String) {
            this.timePosition = timePosition
            this.pixelPosition = pixelPosition
            this.entryIndex = entryIndex
            text = initialText
            isOn = true
        }

        fun end() {
            isOn = false
        }
    }

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
            appState.editEntries(editions.values.toList().sortedBy { it.index })
        } else if (editions.isNotEmpty()) {
            Log.info("No entries changed, discard editions: $editions")
        }
        editions.clear()
    }

    fun submitEntries(editions: List<Edition>) {
        updateEntries(editions)
        submitEntries()
    }

    fun updateEntries(editions: List<Edition>) {
        val editedEntries = this.editedEntries.associateBy { it.index }.toMutableMap()
        val editionGroups = editions.groupContinuouslyBy { index }
        editionGroups.forEach { group ->
            group.forEach { edition ->
                editedEntries[edition.index] = edition.toIndexedEntry()
                this.editions[edition.index] = edition
            }
            if (project.labelerConf.continuous) {
                val lastEdition = group.last()
                val nextEntry = editedEntries[lastEdition.index + 1]
                if (nextEntry != null) {
                    editedEntries[nextEntry.index] =
                        nextEntry.copy(entry = nextEntry.entry.copy(start = lastEdition.newValue.end))
                }
                val firstEdition = group.first()
                val previousEntry = editedEntries[firstEdition.index - 1]
                if (previousEntry != null) {
                    editedEntries[previousEntry.index] =
                        previousEntry.copy(entry = previousEntry.entry.copy(end = firstEdition.newValue.start))
                }
            }
        }
        this.editedEntries = editedEntries.map { it.value }.sortedBy { it.index }
    }

    fun cutEntry(index: Int, position: Float, pixelPosition: Float) {
        val sample = getSampleInfo() ?: return
        if (canUseOnScreenScissors) {
            appState.playSectionByCutting(index, position, sample)
            if (appConf.editor.scissorsActions.askForName == AppConf.ScissorsActions.Target.None) {
                val name = getDefaultNewEntryName(
                    project.currentModule.entries[index].name,
                    project.currentModule.entries.map { it.name },
                    project.labelerConf.allowSameNameEntry,
                )
                val targetEntryIndex = appConf.editor.scissorsActions.getTargetEntryIndex(index)
                appState.cutEntryOnScreen(index, position, name, AppConf.ScissorsActions.Target.None, targetEntryIndex)
            } else {
                val currentEntry = project.currentModule.entries[index]
                onScreenScissorsState.start(index, position, pixelPosition, currentEntry.name)
            }
        } else {
            appState.requestCutEntry(index, position, sample)
        }
    }

    fun commitEntryCut() {
        val index = onScreenScissorsState.entryIndex
        val position = onScreenScissorsState.timePosition
        val name = onScreenScissorsState.text
        val targetEntryIndex = appConf.editor.scissorsActions.getTargetEntryIndex(index)
        if (name.isNotEmpty()) {
            appState.cutEntryOnScreen(
                index,
                position,
                name,
                appConf.editor.scissorsActions.askForName,
                targetEntryIndex,
            )
        }
        onScreenScissorsState.end()
    }

    private fun loadNewEntries() {
        val newValues = project.getEntriesForEditing().second
        if (newValues != editedEntries) {
            Log.info("Load new entries: $newValues")
            editions.clear()
            editedEntries = newValues
        }
    }

    suspend fun loadSample(appConf: AppConf) {
        withContext(Dispatchers.IO) {
            val module = project.currentModule
            val moduleName = module.name
            val sampleDirectory = module.getSampleDirectory(project)
            var needRedirect = false
            if (!sampleDirectory.exists()) {
                needRedirect = true
            }

            if (project.currentSampleFile.exists().not()) {
                // check if all the sample files are not existing,
                // if so, we can consider that the sample directory is not correct.
                val allSampleFiles = module.entries.map {
                    module.getSampleFile(project, it.sample)
                }
                if (allSampleFiles.all { it.exists().not() }) {
                    needRedirect = true
                }
            }

            if (needRedirect) {
                canvasState = CanvasState.Error
                appState.confirmIfRedirectSampleDirectory(sampleDirectory)
                return@withContext
            }

            val previousSampleInfo = getSampleInfo()
            if (previousSampleInfo?.getFile(project)?.absolutePath != project.currentSampleFile.absolutePath) {
                canvasState = CanvasState.Loading
            }
            SampleInfoRepository.load(project, project.currentSampleFile, moduleName, appConf)
                .onFailure {
                    if (it is CancellationException) return@onFailure
                    if (it is WaveConverterException) {
                        appState.showError(it, null)
                    } else {
                        Log.error(it)
                    }
                    canvasState = CanvasState.Error
                }.onSuccess {
                    val updated = chartStore.prepareForNewLoading(project, appConf, it)
                    appState.updateProjectOnLoadedSample(it, moduleName)
                    if (updated) {
                        val renderProgressTotal = it.totalChartCount
                        _renderProgress = 0 to renderProgressTotal
                    }
                    player.load(it.getFile(project))
                    val params = getCanvasParams(it)
                    canvasState = CanvasState.Loaded(params, it)
                }
        }
    }

    fun cancelLoading() {
        canvasState = CanvasState.Loading
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
        val canvasState = canvasState as? CanvasState.Loaded ?: return
        val sampleInfo = canvasState.sampleInfo
        val params = getCanvasParams(sampleInfo)
        this.canvasState = CanvasState.Loaded(params, sampleInfo)
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
    ) {
        when {
            positive -> if (shouldSwitchSample) appState.nextSample() else appState.nextEntry()
            else -> if (shouldSwitchSample) appState.previousSample() else appState.previousEntry()
        }
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

    fun createDefaultEntry(moduleName: String, sampleName: String) {
        appState.createDefaultEntry(moduleName, sampleName)
    }

    fun createDefaultEntries(moduleName: String, sampleNames: List<String>) {
        appState.createDefaultEntries(moduleName, sampleNames)
    }

    fun jumpToEntry(moduleName: String, index: Int) {
        appState.jumpToEntry(moduleName, index)
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

    fun editEntryExtra(index: Int) {
        appState.openEditEntryExtraDialog(index)
    }

    fun jumpToModule(index: Int) {
        appState.jumpToModule(index)
    }

    fun jumpToModule(name: String, targetEntryIndex: Int? = null) {
        val index = project.modules.indexOfFirst { it.name == name }
        appState.jumpToModule(index, targetEntryIndex)
    }

    fun changeSampleDirectory(moduleName: String, directory: File) {
        appState.changeSampleDirectory(moduleName, directory)
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
        if (property.valueSetter == null) return false
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
