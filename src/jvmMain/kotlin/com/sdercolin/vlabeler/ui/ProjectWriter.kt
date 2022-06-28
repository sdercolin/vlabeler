package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.io.saveProjectFile
import com.sdercolin.vlabeler.util.update

@Composable
fun ProjectWriter(appState: MutableState<AppState>) {
    val writtenStatus = appState.value.projectWriteStatus
    LaunchedEffect(writtenStatus) {
        if (writtenStatus != AppState.ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        val project = appState.value.project ?: return@LaunchedEffect
        saveProjectFile(project)
        appState.update { saved() }
    }
}
