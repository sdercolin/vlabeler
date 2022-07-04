@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.editor.labeler.Labeler

@Composable
fun Editor(state: EditorState, appState: AppState) {
    val keyboardState by state.keyboardViewModel.keyboardStateFlow.collectAsState()

    LaunchedEffect(state.project.currentSampleName) {
        state.loadSampleFile()
    }
    LaunchedEffect(Unit) {
        state.updateResolution()
    }

    val labelerFocusRequester = remember { FocusRequester() }

    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxHeight()
                .weight(1f)
                .focusRequester(labelerFocusRequester)
                .focusTarget()
                .onPointerEvent(PointerEventType.Press) {
                    labelerFocusRequester.requestFocus()
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    state.handlePointerEvent(it, keyboardState)
                }
        ) {
            Labeler(
                editorState = state,
                appState = appState
            )
        }
        if (appState.isEntryListPinned) {
            Card(
                modifier = Modifier.fillMaxHeight().weight(0.4f),
                elevation = 10.dp,
                shape = RoundedCornerShape(0.dp)
            ) {
                EntryList(
                    pinned = true,
                    project = state.project,
                    jumpToEntry = { index ->
                        appState.jumpToEntry(index)
                        labelerFocusRequester.requestFocus()
                    }
                )
            }
        }
    }
    if (state.isLoading) {
        CircularProgress()
    }
}
