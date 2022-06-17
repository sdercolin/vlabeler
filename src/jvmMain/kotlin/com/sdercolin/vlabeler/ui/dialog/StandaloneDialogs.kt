package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun StandaloneDialogs(
    labelerConfs: List<LabelerConf>,
    projectState: MutableState<Project?>,
    dialogState: MutableState<DialogState>
) {
    when {
        dialogState.value.openProject -> FileDialog(
            title = string(Strings.OpenProjectDialogTitle),
            extensions = listOf(Project.ProjectFileExtension)
        ) { directory, fileName ->
            dialogState.update { copy(openProject = false) }
            if (directory != null && fileName != null) {
                val projectContent = File(directory, fileName).readText()
                val project = Json.decodeFromString<Project>(projectContent)
                val labelerConf = labelerConfs.find { it.name == project.labelerConf.name }
                    ?: throw Exception("Cannot find labeler: ${project.labelerConf.name}")
                // TODO: update or save labelerConf
                projectState.update { project.copy(labelerConf = labelerConf) }
            }
        }
    }
}
