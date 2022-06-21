package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.toJson
import com.sdercolin.vlabeler.util.update
import java.io.File

@Composable
fun ProjectWriter(appState: MutableState<AppState>) {
    val writtenStatus = appState.value.projectWriteStatus
    val hasEditedEntry = appState.value.hasEditedEntry
    LaunchedEffect(writtenStatus, hasEditedEntry) {
        if (writtenStatus != AppState.ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        if (hasEditedEntry) return@LaunchedEffect
        val project = appState.value.project ?: return@LaunchedEffect
        saveProjectFile(project)
        appState.update { saved() }
    }
}

fun saveProjectFile(project: Project) {
    val workingDirectory = File(project.workingDirectory)
    if (!workingDirectory.exists()) {
        workingDirectory.mkdir()
    }
    val projectContent = toJson(project)
    project.projectFile.writeText(projectContent)
    println("Saved project")
}
