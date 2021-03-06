package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.ErrorDialogContent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

fun loadProject(
    scope: CoroutineScope,
    file: File,
    appState: AppState,
    autoSaved: Boolean = false
) {
    scope.launch(Dispatchers.IO) {
        if (file.exists().not()) {
            appState.snackbarHostState.showSnackbar(string(Strings.StarterRecentDeleted))
            return@launch
        }
        appState.showProgress()
        val project = runCatching { file.readText().parseJson<Project>() }
            .getOrElse {
                appState.openEmbeddedDialog(ErrorDialogContent.FailedToParseProject)
                Log.error(it)
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
        val workingDirectory = when {
            autoSaved -> project.workingDirectory
            project.projectFile.absolutePath != file.absolutePath -> {
                Log.info("Reset project's workingDirectory to ${file.absolutePath}")
                file.absoluteFile.parentFile.absolutePath
            }
            else -> project.workingDirectory
        }

        Log.info("Project loaded: ${project.projectFile.absolutePath}")
        val fixedProject = project.copy(labelerConf = labelerConf, workingDirectory = workingDirectory)
        if (fixedProject != project) {
            Log.info("Loaded project is modified to: $fixedProject")
        }
        appState.openEditor(fixedProject)
        appState.addRecentProject(fixedProject.projectFile)
        if (appState.appConf.editor.autoScroll.onLoadedNewSample) {
            appState.scrollFitViewModel.emitNext()
        }
        appState.hideProgress()
    }
}

fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: AppState
) {
    mainScope.launch(Dispatchers.IO) {
        val file = saveProjectFile(project)
        project.getCacheDir().deleteRecursively()
        appState.openEditor(project)
        appState.addRecentProject(file)
        if (appState.appConf.editor.autoScroll.onLoadedNewSample) {
            appState.scrollFitViewModel.emitNext()
        }
    }
}

fun exportProject(
    mainScope: CoroutineScope,
    parent: String,
    name: String,
    appState: AppState
) {
    mainScope.launch(Dispatchers.IO) {
        appState.showProgress()
        val project = appState.project!!
        val outputText = project.toRawLabels()
        val charset = project.encoding?.let { Charset.forName(it) } ?: Charsets.UTF_8
        File(parent, name).writeText(outputText, charset)
        appState.hideProgress()
    }
}

suspend fun saveProjectFile(project: Project): File = withContext(Dispatchers.IO) {
    val workingDirectory = project.workingDirectory.toFile()
    if (!workingDirectory.exists()) {
        workingDirectory.mkdir()
    }
    val projectContent = project.stringifyJson()
    project.projectFile.writeText(projectContent)
    Log.debug("Project saved to ${project.projectFile}")
    project.projectFile
}

suspend fun autoSaveTemporaryProjectFile(project: Project): File = withContext(Dispatchers.IO) {
    val file = RecordDir.resolve("_" + project.projectFile.name)
    val projectContent = project.stringifyJson()
    file.writeText(projectContent)
    Log.debug("Project auto-saved temporarily: $file")
    file
}
