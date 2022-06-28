package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.io.exportProject
import com.sdercolin.vlabeler.io.openProject
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.lastPathSection
import kotlinx.coroutines.CoroutineScope
import java.io.File

@Composable
fun StandaloneDialogs(
    mainScope: CoroutineScope,
    appState: AppState
) {
    when {
        appState.isShowingOpenProjectDialog -> OpenFileDialog(
            title = string(Strings.OpenProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension)
        ) { parent, name ->
            appState.closeOpenProjectDialog()
            if (parent != null && name != null) {
                openProject(mainScope, File(parent, name), appState)
            }
        }
        appState.isShowingSaveAsProjectDialog -> SaveFileDialog(
            title = string(Strings.SaveAsProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension),
            initialDirectory = appState.project!!.workingDirectory,
            initialFileName = appState.project!!.projectFile.name
        ) { directory, fileName ->
            appState.closeSaveAsProjectDialog()
            if (directory != null && fileName != null) {
                appState.editProject {
                    copy(
                        workingDirectory = directory,
                        projectName = File(fileName).nameWithoutExtension
                    )
                }
                appState.requestSave()
            }
        }
        appState.isShowingExportDialog -> {
            val project = appState.project!!
            SaveFileDialog(
                title = string(Strings.ExportDialogTitle),
                extensions = listOf(project.labelerConf.extension),
                initialDirectory = project.sampleDirectory,
                initialFileName = project.labelerConf.defaultInputFilePath?.lastPathSection
                    ?: "${project.projectName}.${project.labelerConf.extension}"
            ) { parent, name ->
                appState.closeExportDialog()
                if (parent != null && name != null) {
                    exportProject(mainScope, parent, name, appState)
                }
            }
        }
    }
}
