package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.autoSaveProjectFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.savedMutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

interface ProjectStore {
    val project: Project?
    val hasProject: Boolean
    fun requireProject(): Project
    val history: ProjectHistory
    fun newProject(newProject: Project)
    fun clearProject()
    fun editProject(editor: Project.() -> Project)
    fun updateProjectOnLoadedSample(sampleInfo: SampleInfo)
    fun editEntries(editedEntries: List<IndexedEntry>)
    fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?)
    val canUndo: Boolean
    fun undo()
    val canRedo: Boolean
    fun redo()
    val canGoNextEntryOrSample: Boolean
    val canGoPreviousEntryOrSample: Boolean
    fun nextEntry()
    fun previousEntry()
    fun nextSample()
    fun previousSample()
    fun jumpToEntry(index: Int)
    fun renameEntry(index: Int, newName: String)
    fun duplicateEntry(index: Int, newName: String)
    val canRemoveCurrentEntry: Boolean
    fun removeCurrentEntry()
    fun createDefaultEntry(sampleName: String)
    fun isCurrentEntryTheLast(): Boolean
    fun toggleMultipleEditMode(on: Boolean)
    fun changeSampleDirectory(directory: File)
    fun getAutoSavedProjectFile(): File?
    fun discardAutoSavedProjects()
    fun enableAutoSaveProject(intervalSecond: Int?, scope: CoroutineScope, unsavedChangesState: AppUnsavedChangesState)
}

class ProjectStoreImpl(
    private val appConf: State<AppConf>,
    private val screenState: AppScreenState,
    private val scrollFitViewModel: ScrollFitViewModel
) : ProjectStore {

    override var project: Project? by savedMutableStateOf(null) {
        if (it != null) screenState.editor?.updateProject(it)
    }

    override val hasProject get() = project != null
    override fun requireProject(): Project = requireNotNull(project)

    override var history: ProjectHistory by mutableStateOf(ProjectHistory())
        private set

    override fun newProject(newProject: Project) {
        project?.let { discardAutoSavedProject(it) }
        project = newProject
        newProject.requireValid()
        history = ProjectHistory.new(newProject)
    }

    override fun clearProject() {
        project?.let { discardAutoSavedProject(it) }
        project = null
        history = ProjectHistory()
    }

    override fun editProject(editor: Project.() -> Project) {
        val edited = requireProject().editor()
        edited.requireValid()
        project = edited
        history = history.push(edited)
    }

    override fun updateProjectOnLoadedSample(sampleInfo: SampleInfo) {
        val updated = requireProject().updateOnLoadedSample(sampleInfo)
        if (updated == requireProject()) return
        project = updated
        history = history.replaceTop(updated)
    }

    private fun editNonNullProject(editor: Project.() -> Project?) {
        val edited = requireProject().editor() ?: return
        edited.requireValid()
        project = edited
        history = history.push(edited)
    }

    override fun editEntries(editedEntries: List<IndexedEntry>) = editProject { updateEntries(editedEntries) }
    override fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?) {
        if (appConf.value.editor.autoScroll.onSwitchedInMultipleEditMode && targetEntryIndex != project?.currentIndex) {
            scrollFitViewModel.emitNext()
        }
        editProject { cutEntry(index, position, rename, newName, targetEntryIndex) }
    }

    override val canUndo get() = history.canUndo
    override fun undo() {
        history = history.undo()
        project = history.current
    }

    override val canRedo get() = history.canRedo
    override fun redo() {
        history = history.redo()
        project = history.current
    }

    override val canGoNextEntryOrSample get() = project?.run { currentIndex < entryCount - 1 } == true
    override val canGoPreviousEntryOrSample get() = project?.run { currentIndex > 0 } == true

    private fun scrollIfNeededWhenSwitchedEntry(previousProject: Project?) {
        val autoScrollConf = appConf.value.editor.autoScroll
        if ((autoScrollConf.onLoadedNewSample && requireProject().hasSwitchedSample(previousProject)) ||
            (autoScrollConf.onSwitchedInMultipleEditMode && requireProject().multipleEditMode)
        ) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun nextEntry() {
        val previousProject = project
        editNonNullProject { nextEntry() }
        scrollIfNeededWhenSwitchedEntry(previousProject)
    }

    override fun previousEntry() {
        val previousProject = project
        editNonNullProject { previousEntry() }
        scrollIfNeededWhenSwitchedEntry(previousProject)
    }

    private fun scrollIfNeededWhenSwitchedSample() {
        val autoScrollConf = appConf.value.editor.autoScroll
        if (autoScrollConf.onLoadedNewSample ||
            (autoScrollConf.onSwitchedInMultipleEditMode && requireProject().multipleEditMode)
        ) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun nextSample() {
        editNonNullProject { nextSample() }
        scrollIfNeededWhenSwitchedSample()
    }

    override fun previousSample() {
        editNonNullProject { previousSample() }
        scrollIfNeededWhenSwitchedSample()
    }

    override fun jumpToEntry(index: Int) {
        editProject {
            requireProject().copy(currentIndex = index)
        }
        if (appConf.value.editor.autoScroll.onJumpedToEntry) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun renameEntry(index: Int, newName: String) = editProject {
        renameEntry(index, newName)
    }

    override fun duplicateEntry(index: Int, newName: String) = editProject {
        duplicateEntry(index, newName).copy(currentIndex = index + 1)
    }

    override val canRemoveCurrentEntry
        get() = requireProject().entries.size > 1

    override fun removeCurrentEntry() {
        val previousProject = requireProject()

        editProject { removeCurrentEntry() }
        if (requireProject().hasSwitchedSample(previousProject) && appConf.value.editor.autoScroll.onLoadedNewSample) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun createDefaultEntry(sampleName: String) {
        val project = requireProject()
        val newEntry = Entry.fromDefaultValues(sampleName, sampleName, project.labelerConf)
        editProject { copy(entries = project.entries + newEntry) }
    }

    override fun isCurrentEntryTheLast(): Boolean {
        val project = requireProject()
        return project.entries.count { it.sample == project.currentSampleName } == 1
    }

    override fun toggleMultipleEditMode(on: Boolean) = editProject { copy(multipleEditMode = on) }

    override fun changeSampleDirectory(directory: File) {
        editProject { copy(sampleDirectory = directory.absolutePath) }
    }

    private fun listAutoSavedProjectFiles() = RecordDir.listFiles().orEmpty()
        .filter { it.extension == Project.ProjectFileExtension && it.name.startsWith("_") }

    override fun getAutoSavedProjectFile(): File? = listAutoSavedProjectFiles().firstOrNull()

    override fun discardAutoSavedProjects() {
        autoSaveJob?.cancel()
        autoSaveJob = null
        listAutoSavedProjectFiles().forEach { it.delete() }
    }

    private fun discardAutoSavedProject(project: Project) {
        RecordDir.resolve("_" + project.projectFile.name).takeIf { it.exists() }?.delete()
    }

    private var autoSaveJob: Job? = null
    private var autoSavedProject: Project? = null

    override fun enableAutoSaveProject(
        intervalSecond: Int?,
        scope: CoroutineScope,
        unsavedChangesState: AppUnsavedChangesState
    ) {
        autoSaveJob?.cancel()
        autoSaveJob = null
        if (intervalSecond == null || intervalSecond <= 0) return
        autoSaveJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalSecond * 1000L)
                val project = project ?: continue
                if (unsavedChangesState.hasUnsavedChanges && autoSavedProject != project) {
                    autoSaveProjectFile(project)
                    autoSavedProject = project
                }
            }
        }
    }
}
