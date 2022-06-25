package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.io.toRawLabels
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import com.sdercolin.vlabeler.util.update
import java.io.File

@Composable
fun StandaloneDialogs(
    labelerConfs: List<LabelerConf>,
    appState: MutableState<AppState>,
    scrollFitViewModel: ScrollFitViewModel
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
                appState.update { openProject(project.copy(labelerConf = labelerConf)) }
                scrollFitViewModel.emitNext()
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
        appState.value.isShowingExportDialog -> {
            val project = appState.value.project!!
            SaveFileDialog(
                title = string(Strings.ExportDialogTitle),
                extensions = listOf(project.labelerConf.extension),
                initialDirectory = project.sampleDirectory,
                initialFileName = project.labelerConf.defaultInputFilePath?.lastPathSection
                    ?: "${project.projectName}.${project.labelerConf.extension}"
            ) { directory, fileName ->
                appState.update { closeExportDialog() }
                if (directory != null && fileName != null) {
                    val outputText = appState.value.project!!.toRawLabels()
                    File(directory, fileName).writeText(outputText)
                }
            }
        }
    }
}
