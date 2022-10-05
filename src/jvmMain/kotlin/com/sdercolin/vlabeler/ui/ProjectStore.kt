package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.InvalidEditedProjectException
import com.sdercolin.vlabeler.exception.ProjectUpdateOnSampleException
import com.sdercolin.vlabeler.io.autoSaveTemporaryProjectFile
import com.sdercolin.vlabeler.io.exportProjectModule
import com.sdercolin.vlabeler.io.saveProjectFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.savedMutableStateOf
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
    fun editEntries(editedEntries: List<IndexedEntry>, editedIndexes: Set<Int>)
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
    fun jumpToModuleAndEntry(moduleIndex: Int, entryIndex: Int)
    fun jumpToModuleByNameAndEntry(moduleName: String, entryIndex: Int)
    fun jumpToModuleByNameAndEntryName(moduleName: String, entryName: String)
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
    fun enableAutoSaveProject(
        autoSave: AppConf.AutoSave,
        unsavedChangesState: AppUnsavedChangesState,
    )

    suspend fun terminateAutoSaveProject()

    fun toggleEntryDone(index: Int)
    fun toggleCurrentEntryDone()
    fun toggleEntryStar(index: Int)
    fun toggleCurrentEntryStar()
    fun editEntryTag(index: Int, tag: String)
    fun editCurrentEntryTag(tag: String)
    fun selectModule(index: Int)
    fun canOverwriteExportCurrentModule(): Boolean
    fun shouldShowOverwriteExportAllModules(): Boolean
    fun canOverwriteExportAllModules(): Boolean
    fun overwriteExportCurrentModule()
    fun overwriteExportAllModules()
}

class ProjectStoreImpl(
    private val scope: CoroutineScope,
    private val appConf: State<AppConf>,
    private val screenState: AppScreenState,
    private val scrollFitViewModel: ScrollFitViewModel,
    private val errorState: AppErrorState,
    private val progressState: AppProgressState,
) : ProjectStore {

    override var project: Project? by savedMutableStateOf(null) {
        if (it != null) screenState.editor?.updateProject(it)
    }

    override val hasProject get() = project != null
    override fun requireProject(): Project = requireNotNull(project)

    override val history: ProjectHistory = ProjectHistory(appConf)

    override fun newProject(newProject: Project) {
        project?.let { discardAutoSavedProject(it) }
        val validatedProject = newProject.validate()
        project = validatedProject
        history.new(validatedProject)
    }

    override fun clearProject() {
        project?.let { discardAutoSavedProject(it) }
        project = null
        history.clear()
    }

    override fun editProject(editor: Project.() -> Project) {
        val edited = runCatching { requireProject().editor().validate() }.getOrElse {
            errorState.showError(InvalidEditedProjectException(it))
            return
        }
        project = edited
        history.push(edited)
    }

    private fun editCurrentProjectModule(editor: Project.Module.() -> Project.Module) {
        editProject { updateCurrentModule { editor() } }
    }

    override fun updateProjectOnLoadedSample(sampleInfo: SampleInfo) {
        val updated = runCatching {
            requireProject()
                .updateModule(sampleInfo.moduleName) { updateOnLoadedSample(sampleInfo) }
                .validate()
        }
            .getOrElse {
                errorState.showError(ProjectUpdateOnSampleException(it), AppErrorState.ErrorPendingAction.CloseEditor)
                return
            }
        if (updated == requireProject()) return
        project = updated
        history.replaceTop(updated)
    }

    override fun editEntries(editedEntries: List<IndexedEntry>, editedIndexes: Set<Int>) = editProject {
        updateCurrentModule {
            updateEntries(editedEntries, labelerConf)
                .runIf(appConf.value.editor.autoDone) {
                    markEntriesAsDone(editedIndexes)
                }
        }
    }

    override fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?) {
        val autoScrollConf = appConf.value.editor.autoScroll
        if (autoScrollConf.onSwitched ||
            (autoScrollConf.onSwitchedInMultipleEditMode && targetEntryIndex != project?.currentModule?.currentIndex)
        ) {
            scrollFitViewModel.emitNext()
        }
        editCurrentProjectModule { cutEntry(index, position, rename, newName, targetEntryIndex) }
    }

    override val canUndo get() = history.canUndo
    override fun undo() {
        history.undo()
        project = history.current
    }

    override val canRedo get() = history.canRedo
    override fun redo() {
        history.redo()
        project = history.current
    }

    override val canGoNextEntryOrSample: Boolean
        get() = project?.currentModule?.run { currentIndex < entryCount - 1 } == true

    override val canGoPreviousEntryOrSample: Boolean
        get() = project?.currentModule?.run { currentIndex > 0 } == true

    private fun scrollIfNeededWhenSwitchedEntry(previousProject: Project?) {
        val autoScrollConf = appConf.value.editor.autoScroll
        if (autoScrollConf.onSwitched ||
            (autoScrollConf.onLoadedNewSample && requireProject().hasSwitchedSample(previousProject)) ||
            (autoScrollConf.onSwitchedInMultipleEditMode && requireProject().multipleEditMode)
        ) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun nextEntry() {
        val previousProject = project
        editCurrentProjectModule { nextEntry() }
        scrollIfNeededWhenSwitchedEntry(previousProject)
    }

    override fun previousEntry() {
        val previousProject = project
        editCurrentProjectModule { previousEntry() }
        scrollIfNeededWhenSwitchedEntry(previousProject)
    }

    private fun scrollIfNeededWhenSwitchedSample() {
        val autoScrollConf = appConf.value.editor.autoScroll
        if (autoScrollConf.onLoadedNewSample || autoScrollConf.onSwitched ||
            (autoScrollConf.onSwitchedInMultipleEditMode && requireProject().multipleEditMode)
        ) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun nextSample() {
        editCurrentProjectModule { nextSample() }
        scrollIfNeededWhenSwitchedSample()
    }

    override fun previousSample() {
        editCurrentProjectModule { previousSample() }
        scrollIfNeededWhenSwitchedSample()
    }

    override fun jumpToModuleAndEntry(moduleIndex: Int, entryIndex: Int) {
        editProject {
            copy(currentModuleIndex = moduleIndex)
                .updateCurrentModule { copy(currentIndex = entryIndex) }
        }
        if (appConf.value.editor.autoScroll.let { it.onJumpedToEntry || it.onSwitched }) {
            scrollFitViewModel.emitNext()
        }
    }

    private fun findModuleIndexByName(moduleName: String): Int? {
        val moduleIndex = project?.modules?.indexOfFirst { it.name == moduleName }?.takeIf { it >= 0 }
        if (moduleIndex == null) {
            Log.error("Module not found: $moduleName")
            return null
        }
        return moduleIndex
    }

    override fun jumpToModuleByNameAndEntry(moduleName: String, entryIndex: Int) {
        val moduleIndex = findModuleIndexByName(moduleName) ?: return
        jumpToModuleAndEntry(moduleIndex, entryIndex)
    }

    override fun jumpToModuleByNameAndEntryName(moduleName: String, entryName: String) {
        val moduleIndex = findModuleIndexByName(moduleName) ?: return
        val entryIndex = project?.modules?.get(moduleIndex)?.entries
            ?.indexOfFirst { it.name == entryName }?.takeIf { it >= 0 }
        if (entryIndex == null) {
            Log.error("Entry not found in module $moduleName: $entryName")
            return
        }
        jumpToModuleAndEntry(moduleIndex, entryIndex)
    }

    override fun jumpToEntry(index: Int) {
        editCurrentProjectModule {
            copy(currentIndex = index)
        }
        if (appConf.value.editor.autoScroll.let { it.onJumpedToEntry || it.onSwitched }) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun renameEntry(index: Int, newName: String) = editProject {
        updateCurrentModule {
            renameEntry(index, newName, labelerConf)
        }
    }

    override fun duplicateEntry(index: Int, newName: String) = editProject {
        updateCurrentModule {
            duplicateEntry(index, newName, labelerConf).copy(currentIndex = index + 1)
        }
    }

    override val canRemoveCurrentEntry
        get() = requireProject().currentModule.entries.size > 1

    override fun removeCurrentEntry() {
        val previousProject = requireProject()

        editProject { updateCurrentModule { removeCurrentEntry(labelerConf) } }
        val autoScrollConf = appConf.value.editor.autoScroll
        if ((requireProject().hasSwitchedSample(previousProject) && autoScrollConf.onLoadedNewSample) ||
            autoScrollConf.onSwitched
        ) {
            scrollFitViewModel.emitNext()
        }
    }

    override fun createDefaultEntry(sampleName: String) {
        val project = requireProject()
        val newEntry = Entry.fromDefaultValues(sampleName, sampleName, project.labelerConf)
        editCurrentProjectModule { copy(entries = entries + newEntry) }
    }

    override fun isCurrentEntryTheLast(): Boolean {
        val project = requireProject()
        return project.currentModule.entries.count { it.sample == project.currentSampleName } == 1
    }

    override fun toggleMultipleEditMode(on: Boolean) = editProject { copy(multipleEditMode = on) }

    override fun changeSampleDirectory(directory: File) {
        editCurrentProjectModule {
            copy(sampleDirectory = directory.absolutePath)
        }
    }

    private fun listAutoSavedProjectFiles() = RecordDir.getChildren()
        .filter { it.extension == Project.ProjectFileExtension && it.name.startsWith("_") }

    override fun getAutoSavedProjectFile(): File? = listAutoSavedProjectFiles().firstOrNull()

    override fun discardAutoSavedProjects() {
        listAutoSavedProjectFiles().forEach { it.delete() }
    }

    override suspend fun terminateAutoSaveProject() {
        autoSaveJob?.cancelAndJoin()
        autoSaveJob = null
    }

    private fun discardAutoSavedProject(project: Project) {
        RecordDir.resolve("_" + project.projectFile.name).takeIf { it.exists() }?.delete()
    }

    private var autoSaveJob: Job? = null
    private var savedTemporaryProject: Project? = null

    override fun enableAutoSaveProject(
        autoSave: AppConf.AutoSave,
        unsavedChangesState: AppUnsavedChangesState,
    ) {
        autoSaveJob?.cancel()
        autoSaveJob = null
        if (autoSave.target == AppConf.AutoSave.Target.None || autoSave.intervalSec <= 0) return
        autoSaveJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(autoSave.intervalSec * 1000L)
                val project = project ?: continue
                when (autoSave.target) {
                    AppConf.AutoSave.Target.Project -> if (unsavedChangesState.hasUnsavedChanges) {
                        saveProjectFile(project, allowAutoExport = true)
                        unsavedChangesState.projectSaved()
                    }
                    AppConf.AutoSave.Target.Record ->
                        if (unsavedChangesState.hasUnsavedChanges && savedTemporaryProject != project) {
                            autoSaveTemporaryProjectFile(project)
                            savedTemporaryProject = project
                        }
                    else -> Unit
                }
            }
        }
    }

    override fun toggleEntryDone(index: Int) {
        editCurrentProjectModule { toggleEntryDone(index) }
    }

    override fun toggleCurrentEntryDone() {
        editCurrentProjectModule { toggleEntryDone(currentIndex) }
    }

    override fun toggleEntryStar(index: Int) {
        editCurrentProjectModule { toggleEntryStar(index) }
    }

    override fun toggleCurrentEntryStar() {
        editCurrentProjectModule { toggleEntryStar(currentIndex) }
    }

    override fun editEntryTag(index: Int, tag: String) {
        editCurrentProjectModule { editEntryTag(index, tag) }
    }

    override fun editCurrentEntryTag(tag: String) {
        editCurrentProjectModule { editEntryTag(currentIndex, tag) }
    }

    override fun selectModule(index: Int) {
        val previousProject = requireProject()
        editProject { copy(currentModuleIndex = index) }
        scrollIfNeededWhenSwitchedEntry(previousProject)
    }

    override fun canOverwriteExportCurrentModule(): Boolean {
        val project = project ?: return false
        val module = project.currentModule
        return module.rawFilePath != null
    }

    override fun shouldShowOverwriteExportAllModules(): Boolean {
        val project = project ?: return false
        return project.modules.size > 1
    }

    override fun canOverwriteExportAllModules(): Boolean {
        val project = project ?: return false
        return project.modules.any { it.rawFilePath != null }
    }

    override fun overwriteExportCurrentModule() {
        scope.launch(Dispatchers.IO) {
            progressState.showProgress()
            val project = requireProject()
            val targetFile = requireNotNull(project.currentModule.rawFilePath).toFile()
            exportProjectModule(project, project.currentModuleIndex, targetFile)
            progressState.hideProgress()
        }
    }

    override fun overwriteExportAllModules() {
        scope.launch(Dispatchers.IO) {
            progressState.showProgress()
            val project = requireProject()
            project.modules.forEachIndexed { index, module ->
                val targetFile = module.rawFilePath?.toFile() ?: return@forEachIndexed
                exportProjectModule(project, index, targetFile)
            }
            progressState.hideProgress()
        }
    }
}
