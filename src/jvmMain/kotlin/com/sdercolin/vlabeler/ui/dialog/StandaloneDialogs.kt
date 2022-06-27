package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.io.openProject
import com.sdercolin.vlabeler.io.toRawLabels
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Composable
fun StandaloneDialogs(
    mainScope: CoroutineScope,
    labelerConfs: List<LabelerConf>,
    appState: MutableState<AppState>,
    appRecord: MutableState<AppRecord>,
    snackbarHostState: SnackbarHostState,
    scrollFitViewModel: ScrollFitViewModel
) {
    when {
        appState.value.isShowingOpenProjectDialog -> OpenFileDialog(
            title = string(Strings.OpenProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension)
        ) { parent, name ->
            appState.update { closeOpenProjectDialog() }
            if (parent != null && name != null) {
                openProject(
                    mainScope,
                    File(parent, name),
                    labelerConfs,
                    appState,
                    appRecord,
                    snackbarHostState,
                    scrollFitViewModel
                )
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
            ) { parent, name ->
                appState.update { closeExportDialog() }
                if (parent != null && name != null) {
                    val outputText = appState.value.project!!.toRawLabels()
                    File(parent, name).writeText(outputText)
                }
            }
        }
    }
}
