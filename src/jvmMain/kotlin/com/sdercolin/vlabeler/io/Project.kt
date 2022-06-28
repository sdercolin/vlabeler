package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun openProject(
    scope: CoroutineScope,
    file: File,
    appState: AppState
) {
    scope.launch(Dispatchers.IO) {
        if (file.exists().not()) {
            appState.snackbarHostState.showSnackbar(string(Strings.StarterRecentDeleted))
            return@launch
        }
        appState.startProcess()
        val project = parseJson<Project>(file.readText())
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
        appState.openProject(project.copy(labelerConf = labelerConf))
        appState.addRecentProject(file)
        appState.scrollFitViewModel.emitNext()
        appState.finishProcess()
    }
}

fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: AppState
) {
    mainScope.launch(Dispatchers.IO) {
        val file = saveProjectFile(project)
        appState.openProject(project)
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
        appState.startProcess()
        val outputText = appState.project!!.toRawLabels()
        File(parent, name).writeText(outputText)
        appState.finishProcess()
    }
}

suspend fun saveProjectFile(project: Project): File {
    return withContext(Dispatchers.IO) {
        val workingDirectory = File(project.workingDirectory)
        if (!workingDirectory.exists()) {
            workingDirectory.mkdir()
        }
        val projectContent = toJson(project)
        project.projectFile.writeText(projectContent)
        Log.debug("Project saved to ${project.projectFile}")
        project.projectFile
    }
}
