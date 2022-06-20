package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.update

@Composable
fun ProjectStateListener(projectState: State<Project?>, appState: MutableState<AppState>) {
    val project = projectState.value
    var previousProjectPath by remember { mutableStateOf(project?.projectFile?.absolutePath) }
    LaunchedEffect(project) {
        if (previousProjectPath != project?.projectFile?.absolutePath) {
            previousProjectPath = project?.projectFile?.absolutePath
            appState.update { copy(projectWriteStatus = AppState.ProjectWriteStatus.Updated) }
            return@LaunchedEffect
        }
        appState.update { copy(projectWriteStatus = AppState.ProjectWriteStatus.Changed) }
    }
}