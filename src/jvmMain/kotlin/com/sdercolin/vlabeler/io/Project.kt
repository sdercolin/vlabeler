package com.sdercolin.vlabeler.io

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.saveProjectFile
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun openProject(
    scope: CoroutineScope,
    file: File,
    labelerConfs: List<LabelerConf>,
    appState: MutableState<AppState>,
    appRecord: MutableState<AppRecord>,
    snackbarHostState: SnackbarHostState,
    scrollFitViewModel: ScrollFitViewModel
) {
    scope.launch(Dispatchers.IO) {
        if (file.exists().not()) {
            snackbarHostState.showSnackbar(string(Strings.StarterRecentDeleted))
            return@launch
        }
        val project = parseJson<Project>(file.readText())
        val existingLabelerConf = labelerConfs.find { it.name == project.labelerConf.name }
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
        appState.update { openProject(project.copy(labelerConf = labelerConf)) }
        appRecord.update { addRecent(file.absolutePath) }
        scrollFitViewModel.emitNext()
    }
}

fun openCreatedProject(
    mainScope: CoroutineScope,
    project: Project,
    appState: MutableState<AppState>,
    appRecord: MutableState<AppRecord>,
    scrollFitViewModel: ScrollFitViewModel
) {
    mainScope.launch(Dispatchers.IO) {
        val file = saveProjectFile(project)
        appState.update { openProject(project) }
        appRecord.update { addRecent(file.absolutePath) }
        scrollFitViewModel.emitNext()
    }
}
