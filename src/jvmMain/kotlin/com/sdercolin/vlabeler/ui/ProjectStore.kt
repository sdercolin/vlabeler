package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.InvalidEditedProjectException
import com.sdercolin.vlabeler.exception.ProjectUpdateOnSampleException
import com.sdercolin.vlabeler.exception.PropertySetterRuntimeException
import com.sdercolin.vlabeler.exception.PropertySetterUnexpectedRuntimeException
import com.sdercolin.vlabeler.io.autoSaveTemporaryProjectFile
import com.sdercolin.vlabeler.io.exportProject
import com.sdercolin.vlabeler.io.exportProjectModule
import com.sdercolin.vlabeler.io.saveProjectFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.equalsAsFileName
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.savedMutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File

interface ProjectStore {
    val project: Project?
    val hasProject: Boolean
    fun requireProject(): Project
    val history: ProjectHistory
    fun newProject(newProject: Project)
    fun clearProject()
    fun editProject(editor: Project.() -> Project)
    fun updateProject(project: Project)
    fun updateProjectOnLoadedSample(sampleInfo: SampleInfo, moduleName: String)
    fun editEntries(editions: List<Edition>)
    fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?)
    fun cutEntryOnScreen(
        index: Int,
        position: Float,
        name: String,
        target: AppConf.ScissorsActions.Target,
        targetEntryIndex: Int?,
    )

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
    fun jumpToEntry(moduleName: String, index: Int)
    fun renameEntry(index: Int, newName: String)
    fun updateEntryExtra(index: Int, extras: List<String?>)
    fun updateCurrentModuleExtra(extras: List<String?>)
    fun duplicateEntry(index: Int, newName: String)
    val canRemoveCurrentEntry: Boolean
    fun removeCurrentEntry()
    fun createDefaultEntry(moduleName: String, sampleName: String)
    fun createDefaultEntries(moduleName: String, sampleNames: List<String>)
    val canMoveEntry: Boolean
    fun moveEntry(index: Int, targetIndex: Int)
    fun isCurrentEntryTheLast(): Boolean
    fun toggleMultipleEditMode(on: Boolean)
    fun changeSampleDirectory(directory: File)
    fun changeSampleDirectory(moduleName: String, directory: File)
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
    val canEditCurrentEntryExtra: Boolean
    val canEditCurrentModuleExtra: Boolean

    fun shouldShowModuleNavigation(): Boolean
    val canGoNextModule: Boolean
    val canGoPreviousModule: Boolean
    fun nextModule()
    fun previousModule()
    fun jumpToModule(index: Int, targetEntryIndex: Int? = null)

    fun canOverwriteExportCurrentModule(): Boolean
    fun shouldShowOverwriteExportAllModules(): Boolean
    fun canOverwriteExportAllModules(): Boolean
    fun overwriteExportCurrentModule()
    fun overwriteExportAllModules()

    fun openRootDirectory()
    fun openCurrentModuleDirectory()
    fun openProjectLocation()

    suspend fun setCurrentEntryProperty(propertyIndex: Int, value: Float)

    fun importEntries(moduleNameToEntries: List<Pair<String, List<Entry>>>, replace: Boolean)
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

    private fun editCurrentProjectModule(editor: Module.() -> Module) {
        editProject { updateCurrentModule { editor() } }
    }

    private fun editProjectModule(moduleName: String, editor: Module.() -> Module) {
        editProject { updateModule(moduleName) { editor() } }
    }

    override fun updateProject(project: Project) {
        val updated = runCatching { project.validate() }
            .getOrElse {
                errorState.showError(InvalidEditedProjectException(it))
                return
            }
        if (updated == requireProject()) return
        this.project = updated
        history.push(updated)
    }

    override fun updateProjectOnLoadedSample(sampleInfo: SampleInfo, moduleName: String) {
        val updated = runCatching {
            requireProject()
                .updateModule(moduleName) { updateOnLoadedSample(sampleInfo) }
                .validate()
        }
            .getOrElse {
                errorState.showError(ProjectUpdateOnSampleException(it), AppErrorState.ErrorPendingAction.Exit)
                return
            }
        if (updated == requireProject()) return
        project = updated
        history.replaceTop(updated)
    }

    override fun editEntries(editions: List<Edition>) {
        val previousProject = requireProject()
        editProject {
            val editedEntryMap = mutableMapOf<Int, IndexedEntry>()
            editions.forEach {
                editedEntryMap[it.index] = it.toIndexedEntry()
            }
            val editedEntries = editedEntryMap.values.toList().sortedBy { it.index }
            updateCurrentModule {
                updateEntries(editedEntries, labelerConf)
                    .takePostEditAction(
                        editions,
                        appConf.value.editor,
                        AppConf.PostEditAction.Type.Done,
                        labelerConf,
                    )
                    .takePostEditAction(
                        editions,
                        appConf.value.editor,
                        AppConf.PostEditAction.Type.Next,
                        labelerConf,
                    )
            }
        }
        val entrySwitched = previousProject.currentModuleIndex == project?.currentModuleIndex &&
            previousProject.currentModule.currentIndex != project?.currentModule?.currentIndex
        if (entrySwitched) {
            scrollIfNeededWhenSwitchedEntry(previousProject)
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

    override fun cutEntryOnScreen(
        index: Int,
        position: Float,
        name: String,
        target: AppConf.ScissorsActions.Target,
        targetEntryIndex: Int?,
    ) {
        editCurrentProjectModule {
            val rename = if (target != AppConf.ScissorsActions.Target.Former) null else name
            val newName = if (target != AppConf.ScissorsActions.Target.Former) name else entries[index].name
            cutEntry(index, position, rename, newName, targetEntryIndex)
        }
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
        jumpToEntry(requireProject().currentModule.name, index)
    }

    override fun jumpToEntry(moduleName: String, index: Int) {
        editProjectModule(moduleName) {
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

    override fun updateEntryExtra(index: Int, extras: List<String?>) = editProject {
        updateCurrentModule {
            updateEntryExtra(index, extras, labelerConf)
        }
    }

    override fun updateCurrentModuleExtra(extras: List<String?>) = editProject {
        val extrasMap = extras.mapIndexedNotNull { index, value ->
            if (value != null) {
                val key = labelerConf.moduleExtraFields[index].name
                key to value
            } else null
        }.toMap()
        updateCurrentModule {
            copy(extras = extrasMap)
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

    override fun createDefaultEntry(moduleName: String, sampleName: String) {
        createDefaultEntries(moduleName, listOf(sampleName))
    }

    override fun createDefaultEntries(moduleName: String, sampleNames: List<String>) {
        val project = requireProject()
        val newEntries = sampleNames.map {
            Entry.fromDefaultValues(it, project.labelerConf)
        }
        editProjectModule(moduleName) { copy(entries = entries + newEntries) }
    }

    override val canMoveEntry: Boolean
        get() = requireProject().labelerConf.continuous.not() && requireProject().currentModule.entries.size > 1

    override fun moveEntry(index: Int, targetIndex: Int) {
        editCurrentProjectModule {
            val list = entries.toMutableList()
            val moved = list.removeAt(index)
            list.add(targetIndex, moved)
            val newCurrentIndex = if (currentIndex == index) targetIndex else currentIndex
            copy(entries = list, currentIndex = newCurrentIndex)
        }
    }

    override fun isCurrentEntryTheLast(): Boolean {
        val project = requireProject()
        return project.currentModule.entries.count { it.sample.equalsAsFileName(project.currentSampleName) } == 1
    }

    override fun toggleMultipleEditMode(on: Boolean) = editProject { copy(multipleEditMode = on) }

    override fun changeSampleDirectory(directory: File) {
        changeSampleDirectory(requireProject().currentModule.name, directory)
    }

    override fun changeSampleDirectory(moduleName: String, directory: File) {
        val project = requireProject()
        if (project.modules.size == 1 &&
            project.modules.first().getSampleDirectory(project).absolutePath == project.rootSampleDirectory.absolutePath
        ) {
            // If the project has only one module and the module is the root directory,
            // change the root directory.
            editProject {
                val currentWorkingDirectory = workingDirectory.absolutePath
                val currentCacheDirectory = cacheDirectory.absolutePath
                copy(
                    rootSampleDirectoryPath = directory.absolutePath,
                    workingDirectoryPath = currentWorkingDirectory,
                    cacheDirectoryPath = currentCacheDirectory,
                    modules = modules.map { it.copy(sampleDirectoryPath = "") },
                ).makeRelativePathsIfPossible()
            }
        } else {
            editProjectModule(moduleName) {
                val root = requireProject().rootSampleDirectory
                copy(sampleDirectoryPath = directory.relativeTo(root).path)
            }
        }
    }

    private fun listAutoSavedProjectFiles() = RecordDir.getChildren()
        .filter { it.extension == Project.PROJECT_FILE_EXTENSION && it.name.startsWith("_") }

    override fun getAutoSavedProjectFile(): File? = listAutoSavedProjectFiles().firstOrNull()

    override fun discardAutoSavedProjects() {
        listAutoSavedProjectFiles().forEach { discardAutoSavedProjectFile(it) }
    }

    override suspend fun terminateAutoSaveProject() {
        autoSaveJob?.cancelAndJoin()
        autoSaveJob = null
    }

    private fun discardAutoSavedProject(project: Project) {
        val file = RecordDir.resolve("_" + project.projectFile.name).takeIf { it.exists() } ?: return
        discardAutoSavedProjectFile(file)
    }

    private fun discardAutoSavedProjectFile(file: File) {
        Log.debug("Discarding auto saved project: ${file.absolutePath}")
        file.delete()
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

    override val canEditCurrentEntryExtra: Boolean
        get() = project?.labelerConf?.extraFields?.any { it.isVisible } == true

    override val canEditCurrentModuleExtra: Boolean
        get() = project?.labelerConf?.moduleExtraFields?.any { it.isVisible } == true

    override fun shouldShowModuleNavigation(): Boolean {
        val project = project ?: return false
        return project.modules.size > 1
    }

    override val canGoNextModule: Boolean
        get() = project?.let { it.currentModuleIndex < it.modules.size - 1 } ?: false

    override val canGoPreviousModule: Boolean
        get() = project?.let { it.currentModuleIndex > 0 } ?: false

    override fun nextModule() {
        val project = requireProject()
        val nextIndex = (project.currentModuleIndex + 1).coerceAtMost(project.modules.lastIndex)
        jumpToModule(nextIndex)
    }

    override fun previousModule() {
        val project = requireProject()
        val previousIndex = (project.currentModuleIndex - 1).coerceAtLeast(0)
        jumpToModule(previousIndex)
    }

    override fun jumpToModule(index: Int, targetEntryIndex: Int?) {
        val previousProject = requireProject()
        if (index == previousProject.currentModuleIndex &&
            (targetEntryIndex == null || targetEntryIndex == previousProject.currentModule.currentIndex)
        ) return
        val targetModule = previousProject.modules[index]
        editProject { copy(currentModuleIndex = index) }
        if (targetEntryIndex != null) {
            editCurrentProjectModule { copy(currentIndex = targetEntryIndex) }
        }
        val isParallelSwitch = targetModule.isParallelTo(previousProject.currentModule) &&
            project?.currentSampleFile == previousProject.currentSampleFile
        if (!isParallelSwitch || targetEntryIndex != null) {
            scrollIfNeededWhenSwitchedEntry(previousProject)
        }
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
            val targetFile = requireNotNull(project.currentModule.getRawFile(project))
            exportProjectModule(project, project.currentModuleIndex, targetFile)
            progressState.hideProgress()
        }
    }

    override fun overwriteExportAllModules() {
        scope.launch(Dispatchers.IO) {
            progressState.showProgress()
            val project = requireProject()
            exportProject(project)
            progressState.hideProgress()
        }
    }

    private fun openDirectory(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            Desktop.getDesktop().open(directory)
        }
    }

    override fun openRootDirectory() {
        val project = project ?: return
        openDirectory(project.rootSampleDirectory)
    }

    override fun openCurrentModuleDirectory() {
        val project = project ?: return
        openDirectory(project.currentModule.getSampleDirectory(project))
    }

    override fun openProjectLocation() {
        val project = project ?: return
        openDirectory(project.projectFile.parentFile)
    }

    override suspend fun setCurrentEntryProperty(propertyIndex: Int, value: Float) {
        runCatching {
            val project = requireProject()
            val property = project.labelerConf.properties[propertyIndex]
            val setter = requireNotNull(property.valueSetter)
            val js = JavaScript()
            try {
                js.execResource(Resources.expectedErrorJs)
                js.setJson("entry", project.currentModule.currentEntry)
                js.set("value", value)
                js.eval(setter.getScripts(project.labelerConf.directory))
                val updatedEntry = js.getJson("entry") as? Entry
                if (updatedEntry != null) {
                    editCurrentProjectModule { updateCurrentEntry(updatedEntry, project.labelerConf) }
                }
                js.close()
            } catch (e: Exception) {
                val expected = js.getOrNull("expectedError") ?: false
                js.close()
                if (expected) {
                    throw PropertySetterRuntimeException(e, e.message?.parseJson())
                } else {
                    throw PropertySetterUnexpectedRuntimeException(e)
                }
            }
        }.onFailure {
            errorState.showError(it)
        }
    }

    override fun importEntries(moduleNameToEntries: List<Pair<String, List<Entry>>>, replace: Boolean) {
        editProject {
            moduleNameToEntries.fold(this) { acc, (moduleName, entries) ->
                acc.updateModule(moduleName) {
                    val newEntries = if (replace) {
                        entries
                    } else {
                        this.entries + entries
                    }
                    val currentEntryIndex = this.currentIndex.coerceAtMost(newEntries.lastIndex)
                    copy(entries = newEntries, currentIndex = currentEntryIndex)
                }
            }
        }
    }
}
