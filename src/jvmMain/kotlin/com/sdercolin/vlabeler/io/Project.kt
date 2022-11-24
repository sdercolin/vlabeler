package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarDuration
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.ProjectParseException
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.injectLabelerParams
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.currentLanguage
import com.sdercolin.vlabeler.ui.string.stringStatic
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
    val existingLabelerConf = appState.availableLabelerConfs.find { it.name == project.labelerConf.name }
    val originalLabelerConf = when {
        existingLabelerConf == null -> {
            val labelerConfFile = CustomLabelerDir.resolve(project.labelerConf.fileName)
            Log.info("Wrote labeler ${project.labelerConf.name} to ${labelerConfFile.absolutePath}")
            labelerConfFile.writeText(project.labelerConf.stringifyJson())
            showSnackbar(
                stringStatic(
                    Strings.LoadProjectWarningLabelerCreated,
                    project.labelerConf.displayedName.getCertain(currentLanguage),
                ),
            )
            project.labelerConf
        }
        existingLabelerConf.version >= project.labelerConf.version -> {
            existingLabelerConf
        }
        else -> {
            val labelerConfFile = CustomLabelerDir.resolve(project.labelerConf.fileName)
            Log.info(
                "Wrote new version ${project.labelerConf.version} of labeler ${project.labelerConf.name}" +
                    "to ${labelerConfFile.absolutePath}",
            )
            labelerConfFile.writeText(project.labelerConf.stringifyJson())
            showSnackbar(
                stringStatic(
                    Strings.LoadProjectWarningLabelerUpdated,
                    project.labelerConf.displayedName.getCertain(currentLanguage),
                    project.labelerConf.version,
                ),
            )
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
        autoSaved -> project.workingDirectory.absolutePath to project.projectName
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
            val originalWorkingDirectory = project.workingDirectory.absoluteFile
            val relativeRootSampleDirectory = project.rootSampleDirectory?.relativeToOrNull(originalWorkingDirectory)
            if (relativeRootSampleDirectory == null) {
                null
            } else {
                // keep relative root sample directory
                val newWorkingDirectory = workingDirectory.toFile()
                require(newWorkingDirectory.isAbsolute)
                newWorkingDirectory.resolve(relativeRootSampleDirectory).absolutePath.also {
                    Log.info("Redirect root sample directory to $it")
                }
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

    Log.info("Project loaded: ${file.absolutePath}, original path is ${project.projectFile.absolutePath}")
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
        saveProjectFile(fixedProject, allowAutoExport = false)
    }
    appState.hideProgress()
}

fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: AppState,
) {
    mainScope.launch(Dispatchers.IO) {
        awaitOpenCreatedProject(project, appState)
    }
}

suspend fun awaitOpenCreatedProject(
    project: Project,
    appState: AppState,
) {
    val file = saveProjectFile(project)
    project.clearCache()
    appState.openEditor(project)
    appState.discardAutoSavedProjects()
    appState.addRecentProject(file)
    if (appState.appConf.editor.autoScroll.let { it.onSwitched || it.onLoadedNewSample }) {
        appState.scrollFitViewModel.emitNext()
    }
}

suspend fun exportProject(project: Project) {
    val allModules = project.modules.withIndex()
    val groups = allModules.filter { it.value.rawFilePath != null }.groupBy { it.value.getRawFile(project) }
        .map { it.value }

    for (group in groups) {
        exportProjectModule(project, group.first().index, requireNotNull(group.first().value.getRawFile(project)))
    }
}

@Suppress("RedundantSuspendModifier")
suspend fun exportProjectModule(
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

        val charset = project.encoding?.let { Charset.forName(it) } ?: Charsets.UTF_8
        outputFile.writeText(outputText, charset)
        Log.debug(
            "Project module ${outputModuleNames.joinToString { "\"$it\"" }} " +
                "exported to ${outputFile.absolutePath}",
        )
    }.getOrElse {
        Log.error(it)
        return
    }
}

private var saveFileJob: Job? = null

suspend fun saveProjectFile(project: Project, allowAutoExport: Boolean = false): File = withContext(Dispatchers.IO) {
    saveFileJob?.join()
    saveFileJob = launch {
        val workingDirectory = project.workingDirectory
        if (!workingDirectory.exists()) {
            workingDirectory.mkdir()
        }
        val projectContent = project.stringifyJson()
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

suspend fun autoSaveTemporaryProjectFile(project: Project): File = withContext(Dispatchers.IO) {
    val file = RecordDir.resolve("_" + project.projectFile.name)
    val projectContent = project.stringifyJson()
    file.writeText(projectContent)
    Log.debug("Project auto-saved temporarily: $file")
    file
}
