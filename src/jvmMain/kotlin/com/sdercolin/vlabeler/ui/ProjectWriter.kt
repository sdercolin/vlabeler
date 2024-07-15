package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sdercolin.vlabeler.io.saveProjectFile

@Composable
fun ProjectWriter(appState: AppState) {
    val writtenStatus = appState.projectWriteStatus
    LaunchedEffect(writtenStatus) {
        if (writtenStatus != ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        val project = appState.project ?: return@LaunchedEffect
        appState.saveProjectFile(project, allowAutoExport = true)
        appState.notifySaved()
    }
}
