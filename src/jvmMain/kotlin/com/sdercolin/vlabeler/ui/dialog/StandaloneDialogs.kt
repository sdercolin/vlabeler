package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.update

@Composable
fun StandaloneDialogs(
    appConf: AppConf,
    labelerConf: LabelerConf,
    projectState: MutableState<Project?>,
    dialogState: MutableState<DialogState>
) {
    when {
        dialogState.value.openFile -> FileDialog(
            onCloseRequest = { directory, fileName ->
                dialogState.update { copy(openFile = false) }
                if (directory != null && fileName != null) {
                    projectState.update { Project.fromSingleFile(directory, fileName, appConf, labelerConf) }
                }
            }
        )
    }
}
