@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.editor.labeler.Labeler

@Composable
fun Editor(state: EditorState) {
    val keyboardState by state.keyboardViewModel.keyboardStateFlow.collectAsState()

    LaunchedEffect(state.project.currentSampleName) {
        state.loadSampleFile()
    }
    LaunchedEffect(Unit) {
        state.updateResolution()
    }

    Box(
        Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) {
                state.handlePointerEvent(it, keyboardState)
            }
    ) {
        Labeler(
            sample = state.sample,
            project = state.project,
            entry = state.editedEntry.entry,
            editEntry = state::updateEntry,
            submitEntry = state::submitEntry,
            labelerState = state.labelerState,
            appState = state.appState
        )
    }
    if (state.isLoading) {
        CircularProgress()
    }
}
