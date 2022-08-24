package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.ProjectParseException
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
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
        if (file.exists().not()) {
            appState.snackbarHostState.showSnackbar(string(Strings.StarterRecentDeleted))
            return@launch
        }
        appState.showProgress()
        val project = runCatching { file.readText().parseJson<Project>() }
            .getOrElse {
                appState.showError(ProjectParseException(it))
                appState.hideProgress()
                return@launch
            }
        val existingLabelerConf = appState.availableLabelerConfs.find { it.name == project.labelerConf.name }
        val labelerConf = when {
            existingLabelerConf == null -> {
                CustomLabelerDir.resolve(project.labelerConf.fileName).writeText(project.labelerConf.stringifyJson())
                project.labelerConf
            }
            existingLabelerConf.version >= project.labelerConf.version -> {
                existingLabelerConf
            }
            else -> {
                // TODO: notifying user about updating local labelers
                project.labelerConf
            }
        }
        val (workingDirectory, projectName) = when {
            autoSaved -> project.workingDirectory to project.projectName
            project.projectFile.absolutePath != file.absolutePath -> {
                Log.info("Redirect project path to ${file.absolutePath}")
                file.absoluteFile.parentFile.absolutePath to file.nameWithoutExtension
            }
            else -> project.workingDirectory to project.projectName
        }

        Log.info("Project loaded: ${project.projectFile.absolutePath}")
        val fixedProject = project.copy(
            labelerConf = labelerConf,
            workingDirectory = workingDirectory,
            projectName = projectName,
        )
        if (fixedProject != project) {
            Log.info("Loaded project is modified to: $fixedProject")
        }
        appState.openEditor(fixedProject)
        if (!autoSaved) {
            appState.discardAutoSavedProjects()
        }
        appState.addRecentProject(fixedProject.projectFile)
        if (appState.appConf.editor.autoScroll.let { it.onSwitched || it.onLoadedNewSample }) {
            appState.scrollFitViewModel.emitNext()
        }
        appState.hideProgress()
    }
}

fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: AppState,
) {
    mainScope.launch(Dispatchers.IO) {
        val file = saveProjectFile(project)
        project.getCacheDir().deleteRecursively()
        appState.openEditor(project)
        appState.discardAutoSavedProjects()
        appState.addRecentProject(file)
        if (appState.appConf.editor.autoScroll.let { it.onSwitched || it.onLoadedNewSample }) {
            appState.scrollFitViewModel.emitNext()
        }
    }
}

@Suppress("RedundantSuspendModifier")
suspend fun exportProject(
    project: Project,
    outputFile: File,
) {
    val outputText = project.toRawLabels()
    val charset = project.encoding?.let { Charset.forName(it) } ?: Charsets.UTF_8
    outputFile.writeText(outputText, charset)
    Log.debug("Project exported to ${outputFile.absolutePath}")
}

private var saveFileJob: Job? = null

suspend fun saveProjectFile(project: Project, allowAutoExport: Boolean = false): File = withContext(Dispatchers.IO) {
    saveFileJob?.join()
    saveFileJob = launch {
        val workingDirectory = project.workingDirectory.toFile()
        if (!workingDirectory.exists()) {
            workingDirectory.mkdir()
        }
        val projectContent = project.stringifyJson()
        project.projectFile.writeText(projectContent)
        Log.debug("Project saved to ${project.projectFile}")

        if (allowAutoExport && project.autoExportTargetPath != null) {
            exportProject(project, project.autoExportTargetPath.toFile())
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
