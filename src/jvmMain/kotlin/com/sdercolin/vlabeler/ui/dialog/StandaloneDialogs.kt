package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.io.toRawLabels
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.update
import java.io.File

@Composable
fun StandaloneDialogs(
    labelerConfs: List<LabelerConf>,
    appState: MutableState<AppState>
) {
    when {
        appState.value.isShowingOpenProjectDialog -> OpenFileDialog(
            title = string(Strings.OpenProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension)
        ) { directory, fileName ->
            appState.update { closeOpenProjectDialog() }
            if (directory != null && fileName != null) {
                val projectContent = File(directory, fileName).readText()
                val project = parseJson<Project>(projectContent)
                val labelerConf = labelerConfs.find { it.name == project.labelerConf.name }
                    ?: throw Exception("Cannot find labeler: ${project.labelerConf.name}")
                // TODO: update or save labelerConf
                appState.update { openProject(project.copy(labelerConf = labelerConf)) }
            }
        }
        appState.value.isShowingSaveAsProjectDialog -> SaveFileDialog(
            title = string(Strings.SaveAsProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension),
            initialDirectory = appState.value.project!!.workingDirectory,
            initialFileName = appState.value.project!!.projectFile.name
        ) { directory, fileName ->
            appState.update { closeSaveAsProjectDialog() }
            if (directory != null && fileName != null) {
                appState.update {
                    editProject {
                        copy(
                            workingDirectory = directory,
                            projectName = File(fileName).nameWithoutExtension
                        )
                    }.requestSave()
                }
            }
        }
        appState.value.isShowingExportDialog -> SaveFileDialog(
            title = string(Strings.ExportDialogTitle),
            extensions = listOf(appState.value.project!!.labelerConf.extension),
            initialDirectory = appState.value.project!!.sampleDirectory,
            initialFileName = appState.value.project!!.labelerConf.defaultInputFilePath.lastPathSection
        ) { directory, fileName ->
            appState.update { closeExportDialog() }
            if (directory != null && fileName != null) {
                val outputText = appState.value.project!!.toRawLabels()
                File(directory, fileName).writeText(outputText)
            }
        }
    }
}
