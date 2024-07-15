package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarDuration
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.ProjectImportException
import com.sdercolin.vlabeler.exception.ProjectParseException
import com.sdercolin.vlabeler.exception.RequiredLabelerNotFoundException
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.injectLabelerParams
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.ui.dialog.importentries.ImportEntriesDialogArgs
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.clearCache
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

/**
 * Load project from file asynchronously.
 *
 * @param scope The scope to launch the coroutine.
 * @param file The file to load.
 * @param appState The app state.
 * @param autoSaved Whether the project is a temporary auto-saved project.
 */
fun loadProject(
    scope: CoroutineScope,
    file: File,
    appState: AppState,
    autoSaved: Boolean = false,
) {
    scope.launch(Dispatchers.IO) {
        runCatching {
            awaitLoadProject(file, appState, autoSaved)
        }.onFailure {
            appState.showError(it)
            appState.hideProgress()
        }
    }
}

/**
 * Load project from file synchronously.
 *
 * @param file The file to load.
 * @param appState The app state.
 * @param autoSaved Whether the project is a temporary auto-saved project.
 */
suspend fun awaitLoadProject(
    file: File,
    appState: AppState,
    autoSaved: Boolean = false,
) {
    suspend fun showSnackbar(message: String) {
        appState.showSnackbar(
            message,
            duration = SnackbarDuration.Indefinite,
        )
    }

    if (file.exists().not()) {
        showSnackbar(stringStatic(Strings.StarterRecentDeleted))
        return
    }
    appState.showProgress()
    val project = runCatching { file.readText().parseJson<Project>() }
        .getOrElse {
            appState.showError(ProjectParseException(it))
            appState.hideProgress()
            return
        }
        .run { copy(labelerConf = this.labelerConf.migrate()) }
    val existingLabelerConf = appState.availableLabelerConfs.find { it.name == project.labelerConf.name }
    val originalLabelerConf = when {
        existingLabelerConf == null -> {
            if (project.labelerConf.resourceFiles.isNotEmpty()) {
                throw RequiredLabelerNotFoundException(
                    project.labelerConf.displayedName.getCertain(currentLanguage),
                    project.labelerConf.version.toString(),
                )
            } else {
                project.labelerConf.install(CustomLabelerDir)
                    .onFailure { Log.error(it) }
                    .onSuccess {
                        showSnackbar(
                            stringStatic(
                                Strings.LoadProjectWarningLabelerCreated,
                                project.labelerConf.displayedName.getCertain(currentLanguage),
                            ),
                        )
                    }
            }
            project.labelerConf
        }
        existingLabelerConf.version >= project.labelerConf.version -> {
            existingLabelerConf
        }
        else -> {
            if (project.labelerConf.resourceFiles.isNotEmpty()) {
                throw RequiredLabelerNotFoundException(
                    project.labelerConf.displayedName.getCertain(currentLanguage),
                    project.labelerConf.version.toString(),
                )
            } else {
                project.labelerConf.install(CustomLabelerDir)
                    .onFailure { Log.error(it) }
                    .onSuccess {
                        showSnackbar(
                            stringStatic(
                                Strings.LoadProjectWarningLabelerUpdated,
                                project.labelerConf.displayedName.getCertain(currentLanguage),
                                project.labelerConf.version,
                            ),
                        )
                    }
            }
            project.labelerConf
        }
    }
    val labelerConf = if (project.labelerParams == null) {
        originalLabelerConf
    } else {
        runCatching { originalLabelerConf.injectLabelerParams(project.labelerParams.resolve(originalLabelerConf)) }
            .getOrElse {
                appState.showError(ProjectParseException(it))
                appState.hideProgress()
                return
            }
    }
    val (workingDirectory, projectName) = when {
        autoSaved -> {
            project.workingDirectory.absolutePath to project.projectName
        }
        project.projectFile.absolutePath != file.absolutePath -> {
            Log.info("Redirect project path to ${file.absolutePath}")
            file.absoluteFile.parentFile.absolutePath to file.nameWithoutExtension
        }
        else -> project.workingDirectoryPath to project.projectName
    }

    val rootSampleDirectory =
        if (project.workingDirectoryPath.toFile().isAbsolute.not() &&
            workingDirectory != project.workingDirectoryPath
        ) {
            // redirect root sample directory when the project file is moved together with the sample directory
            val originalWorkingDirectory = project.workingDirectoryPath.toFile().let {
                if (it.isAbsolute) it else project.rootSampleDirectory.resolve(it)
            }
            val relativeRootSampleDirectory = project.rootSampleDirectory.relativeTo(originalWorkingDirectory)
            val newWorkingDirectory = workingDirectory.toFile()
            require(newWorkingDirectory.isAbsolute)
            newWorkingDirectory.resolve(relativeRootSampleDirectory).absolutePath.also {
                Log.info("Redirect root sample directory to $it")
            }
        } else {
            project.rootSampleDirectoryPath
        }

    val cacheDirectory = if (project.cacheDirectory.parentFile?.exists() != true) {
        Project.getDefaultCacheDirectory(workingDirectory, projectName).also {
            showSnackbar(stringStatic(Strings.LoadProjectWarningCacheDirReset))
            Log.info("Reset cache directory to $it")
        }
    } else {
        project.cacheDirectoryPath
    }

    Log.debug("Project loaded: ${file.absolutePath}, original path is ${project.projectFile.absolutePath}")
    val fixedProject = project.copy(
        labelerConf = labelerConf,
        originalLabelerConf = originalLabelerConf,
        rootSampleDirectoryPath = rootSampleDirectory,
        workingDirectoryPath = workingDirectory,
        projectName = projectName,
        cacheDirectoryPath = cacheDirectory,
    ).makeRelativePathsIfPossible()
    appState.openEditor(fixedProject)
    if (!autoSaved) {
        appState.discardAutoSavedProjects()
    }
    appState.addRecentProject(fixedProject.projectFile)
    if (appState.appConf.editor.autoScroll.let { it.onSwitched || it.onLoadedNewSample }) {
        appState.scrollFitViewModel.emitNext()
    }
    if (fixedProject != project) {
        appState.saveProjectFile(fixedProject, allowAutoExport = false)
    }
    appState.hideProgress()
}

/**
 * Open a project that has just been created asynchronously.
 *
 * @param mainScope The main coroutine scope.
 * @param project The project that has just been created.
 * @param appState The app state.
 */
fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: AppState,
) {
    mainScope.launch(Dispatchers.IO) {
        awaitOpenCreatedProject(project, appState)
    }
}

/**
 * Open a project that has just been created synchronously.
 *
 * @param project The project that has just been created.
 * @param appState The app state.
 */
suspend fun awaitOpenCreatedProject(
    project: Project,
    appState: AppState,
) {
    val file = appState.saveProjectFile(project)
    project.clearCache()
    appState.openEditor(project)
    appState.discardAutoSavedProjects()
    appState.addRecentProject(file)
    if (appState.appConf.editor.autoScroll.let { it.onSwitched || it.onLoadedNewSample }) {
        appState.scrollFitViewModel.emitNext()
    }
}

/**
 * Export the all the modules of a project to the pre-defined output files. If the output file of a module is not
 * defined, it will be skipped.
 *
 * @param project The project to export.
 */
suspend fun ProjectStore.exportProject(project: Project) {
    val allModules = project.modules.withIndex()
    val groups = allModules.filter { it.value.rawFilePath != null }.groupBy { it.value.getRawFile(project) }
        .map { it.value }

    for (group in groups) {
        exportProjectModule(project, group.first().index, requireNotNull(group.first().value.getRawFile(project)))
    }
}

/**
 * Export a module of a project to the given output file.
 *
 * @param project The project to export.
 * @param moduleIndex The index of the module to export.
 * @param outputFile The output file to export to.
 */
suspend fun ProjectStore.exportProjectModule(
    project: Project,
    moduleIndex: Int,
    outputFile: File,
) {
    val inEntryScope = project.labelerConf.writer.scope == LabelerConf.Scope.Entry
    val outputModuleNames = mutableListOf<String>()
    val module = project.modules[moduleIndex]
    runCatching {
        if (outputFile.parentFile.exists().not()) {
            outputFile.parentFile.mkdirs()
        }
        val outputText = if (inEntryScope) {
            outputModuleNames.add(module.name)
            project.singleModuleToRawLabels(moduleIndex)
        } else {
            val relatedModules = project.modules.withIndex().filter {
                it.value == module || it.value.isParallelTo(module)
            }
            outputModuleNames.addAll(relatedModules.map { it.value.name })
            project.modulesToRawLabels(relatedModules.map { it.index })
        }

        val charset = project.encoding.let { Charset.forName(it) } ?: Charsets.UTF_8
        withExporting {
            outputFile.writeText(outputText, charset)
            outputFile
        }
        Log.debug(
            "Project module ${outputModuleNames.joinToString { "\"$it\"" }} " +
                "exported to ${outputFile.absolutePath}",
        )
    }.getOrElse {
        Log.error(it)
    }
}

private var saveFileJob: Job? = null

/**
 * Save a project to its project file.
 *
 * @param project The project to save.
 * @param allowAutoExport Whether to allow auto export.
 * @return The saved project file.
 */
suspend fun ProjectStore.saveProjectFile(project: Project, allowAutoExport: Boolean = false): File =
    withContext(Dispatchers.IO) {
        saveFileJob?.join()
        saveFileJob = launch {
            val workingDirectory = project.workingDirectory
            if (!workingDirectory.exists()) {
                workingDirectory.mkdir()
            }
            val projectContent = project.copy(version = Project.PROJECT_VERSION).stringifyJson()
            project.projectFile.writeText(projectContent)
            Log.debug("Project saved to ${project.projectFile}")

            if (allowAutoExport && project.autoExport) {
                exportProject(project)
            }
        }
        saveFileJob?.join()
        saveFileJob = null
        project.projectFile
    }

/**
 * Save a project to a temporary file.
 *
 * @param project The project to save.
 * @return The saved temporary file.
 */
suspend fun autoSaveTemporaryProjectFile(project: Project): File = withContext(Dispatchers.IO) {
    val file = RecordDir.resolve("_" + project.projectFile.name)
    val projectContent = project.stringifyJson()
    file.writeText(projectContent)
    Log.debug("Project auto-saved temporarily: $file")
    file
}

/**
 * Import content from a project file to the current project.
 *
 * @param scope The coroutine scope.
 * @param file The project file to import.
 * @param appState The app state.
 */
fun importProjectFile(
    scope: CoroutineScope,
    file: File,
    appState: AppState,
) {
    scope.launch(Dispatchers.IO) {
        runCatching {
            appState.showProgress()

            val text = file.readText()
            val modules = importModulesFromProject(text)

            appState.hideProgress()
            appState.openImportEntriesDialog(ImportEntriesDialogArgs(modules))
        }.onFailure {
            appState.showError(ProjectImportException(it))
            appState.hideProgress()
        }
    }
}
