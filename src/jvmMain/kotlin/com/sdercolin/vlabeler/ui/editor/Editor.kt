@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalSplitPaneApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.editor.labeler.Labeler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.SplitPaneScope
import org.jetbrains.compose.splitpane.SplitPaneState
import org.jetbrains.compose.splitpane.VerticalSplitPane
import java.awt.Cursor

@Composable
fun Editor(state: EditorState, appState: AppState) {
    val keyboardState by state.keyboardViewModel.keyboardStateFlow.collectAsState()

    LaunchedEffect(state.project.currentSampleName, state.project.sampleDirectory, appState.player, state) {
        state.loadSample()
    }
    LaunchedEffect(state) {
        state.updateResolution()
    }
    DisposableEffect(state) {
        onDispose { state.clear() }
    }

    val labelerFocusRequester = remember { FocusRequester() }

    val labelerBox = @Composable {
        Box(
            Modifier.fillMaxSize()
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
    }

    val entryListCard = @Composable {
        if (appState.isEntryListPinned) {
            Card(
                modifier = Modifier.fillMaxSize(),
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

    val position = appState.appConf.view.pinnedEntryListPosition
    val isHorizontal = position == AppConf.ViewPosition.Right || position == AppConf.ViewPosition.Left
    val isLabelerFirst = position == AppConf.ViewPosition.Right || position == AppConf.ViewPosition.Bottom
    val appRecord = appState.appRecordFlow.collectAsState().value
    val locked = appRecord.pinnedEntryListSplitPanePositionLocked
    val splitPaneState = remember(locked, position, appState.isEntryListPinned) {
        val percentage = if (appState.isEntryListPinned) {
            appRecord.getPinnedEntryListSplitPanePosition(position)
        } else {
            if (isLabelerFirst) 1f else 0f
        }
        SplitPaneState(
            initialPositionPercentage = percentage,
            moveEnabled = !locked && appState.isEntryListPinned
        )
    }

    val splitPaneContent: SplitPaneScope.() -> Unit = {
        if (isLabelerFirst) {
            first {
                labelerBox()
            }
            second {
                entryListCard()
            }
        } else {
            first {
                entryListCard()
            }
            second {
                labelerBox()
            }
        }
        splitter {
            handle {
                val cursor = if (isHorizontal) Cursor.E_RESIZE_CURSOR else Cursor.S_RESIZE_CURSOR
                Box(
                    Modifier
                        .markAsHandle()
                        .pointerHoverIcon(PointerIcon(Cursor(cursor)))
                        .width(1.dp)
                        .fillMaxHeight()
                )
            }
        }
    }

    LaunchedEffect(splitPaneState) {
        snapshotFlow { splitPaneState.positionPercentage }
            .onEach {
                if (appState.isEntryListPinned) {
                    appState.appRecordStore.update { setPinnedEntryListSplitPanePosition(position, it) }
                }
            }
            .launchIn(this)
    }
    if (isHorizontal) {
        HorizontalSplitPane(splitPaneState = splitPaneState, content = splitPaneContent)
    } else {
        VerticalSplitPane(splitPaneState = splitPaneState, content = splitPaneContent)
    }

    if (state.isLoading) {
        CircularProgress()
    }
}
