package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.ErrorDialogContent
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

fun loadProject(
    scope: CoroutineScope,
    file: File,
    appState: AppState
) {
    scope.launch(Dispatchers.IO) {
        if (file.exists().not()) {
            appState.snackbarHostState.showSnackbar(string(Strings.StarterRecentDeleted))
            return@launch
        }
        appState.showProgress()
        val project = runCatching { parseJson<Project>(file.readText()) }
            .getOrElse {
                appState.openEmbeddedDialog(ErrorDialogContent.FailedToParseProject)
                Log.error(it)
                appState.hideProgress()
                return@launch
            }
        val existingLabelerConf = appState.availableLabelerConfs.find { it.name == project.labelerConf.name }
        val labelerConf = when {
            existingLabelerConf == null -> {
                CustomLabelerDir.resolve(project.labelerConf.fileName).writeText(toJson(project.labelerConf))
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
        Log.info("Project loaded: $project")
        appState.openEditor(project.copy(labelerConf = labelerConf))
        appState.addRecentProject(project.projectFile)
        appState.scrollFitViewModel.emitNext()
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
        appState.openEditor(project)
        appState.addRecentProject(file)
        appState.scrollFitViewModel.emitNext()
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
    val workingDirectory = File(project.workingDirectory)
    if (!workingDirectory.exists()) {
        workingDirectory.mkdir()
    }
    val projectContent = toJson(project)
    project.projectFile.writeText(projectContent)
    Log.debug("Project saved to ${project.projectFile}")
    project.projectFile
}

suspend fun autoSaveProjectFile(project: Project): File = withContext(Dispatchers.IO) {
    val file = RecordDir.resolve("_" + project.projectFile.name)
    val projectContent = toJson(project)
    file.writeText(projectContent)
    Log.debug("Project auto-saved to $file")
    file
}
