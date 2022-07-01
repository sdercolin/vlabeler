package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ProjectChangesListener(appState: AppState) {
    val project = appState.project
    var previousProjectPath by remember { mutableStateOf(project?.projectFile?.absolutePath) }
    LaunchedEffect(project) {
        val currentPath = project?.projectFile?.absolutePath
        if (previousProjectPath != currentPath) {
            previousProjectPath = currentPath
            appState.projectPathChanged()
            return@LaunchedEffect
        }
        if (project == null) return@LaunchedEffect
        appState.projectContentChanged()
    }
}
