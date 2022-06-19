package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun ProjectWriter(projectState: State<Project?>, appState: MutableState<AppState>) {
    val writtenStatus = appState.value.projectWriteStatus
    val hasEditedEntry = appState.value.hasEditedEntry
    LaunchedEffect(writtenStatus, hasEditedEntry) {
        if (writtenStatus != AppState.ProjectWriteStatus.UpdateRequested) return@LaunchedEffect
        if (hasEditedEntry) return@LaunchedEffect
        val project = projectState.value ?: return@LaunchedEffect
        saveProjectFile(project)
        appState.update { copy(projectWriteStatus = AppState.ProjectWriteStatus.Updated) }
    }
}

suspend fun saveProjectFile(project: Project) {
    withContext(Dispatchers.IO) {
        println("Save project")
        val workingDirectory = File(project.workingDirectory)
        if (!workingDirectory.exists()) {
            workingDirectory.mkdir()
        }
        val projectContent = Json.encodeToString(project)
        project.projectFile.writeText(projectContent)
    }
}
