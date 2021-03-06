package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.io.exportProject
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.toFileOrNull
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
                loadProject(mainScope, File(parent, name), appState)
            }
        }
        appState.isShowingSaveAsProjectDialog -> SaveFileDialog(
            title = string(Strings.SaveAsProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension),
            initialDirectory = appState.project!!.workingDirectory,
            initialFileName = appState.project.projectFile.name
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
            val project = appState.requireProject()
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
        appState.isShowingSampleDirectoryRedirectDialog -> {
            val project = appState.requireProject()
            OpenFileDialog(
                title = string(Strings.ChooseSampleDirectoryDialogTitle),
                initialDirectory = project.sampleDirectory.toFileOrNull()?.absolutePath,
                extensions = null,
                directoryMode = true
            ) { parent, name ->
                appState.closeSampleDirectoryRedirectDialog()
                if (parent != null && name != null) {
                    val newDirectory = File(parent, name)
                    if (newDirectory.exists() && newDirectory.isDirectory) {
                        appState.changeSampleDirectory(newDirectory)
                    }
                }
            }
        }
    }
}
