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
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.editor.labeler.Labeler
import com.sdercolin.vlabeler.util.asNormalizedFileName
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

    LaunchedEffect(
        state.project.currentModuleIndex,
        state.project.currentSampleName.asNormalizedFileName(),
        state.project.currentModule.sampleDirectoryPath,
        state.project.rootSampleDirectory,
        appState.isShowingPrerenderDialog,
        state,
    ) {
        if (appState.isShowingPrerenderDialog) {
            state.cancelLoading()
        } else {
            state.loadSample(appState.appConf)
            appState.videoState.currentSampleRate = state.getSampleInfo()?.sampleRate
        }
    }
    LaunchedEffect(state) {
        state.updateResolution()
    }
    DisposableEffect(state) {
        onDispose { state.clear() }
    }
    LaunchedEffect(appState.appConf.autoReload.behavior, state.project.currentModule.name) {
        appState.autoReloadLabel(
            behavior = appState.appConf.autoReload.behavior,
            moduleName = state.project.currentModule.name,
        )
    }

    val labelerFocusRequester = remember { FocusRequester() }

    val labelerBox = @Composable {
        Box(
            Modifier.fillMaxSize()
                .focusRequester(labelerFocusRequester)
                .focusTarget()
                .plainClickable {
                    labelerFocusRequester.requestFocus()
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    state.handlePointerEvent(it, keyboardState)
                },
        ) {
            Labeler(
                editorState = state,
                appState = appState,
            )
        }
    }

    val entryListCard = @Composable {
        if (appState.isEntryListPinned) {
            val jumpToEntry: (Int) -> Unit = remember {
                { index ->
                    appState.jumpToEntry(index)
                    labelerFocusRequester.requestFocus()
                }
            }
            val onFocusedChanged: (Boolean) -> Unit = remember(state) {
                { state.isPinnedEntryListInputFocused = it }
            }
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = 10.dp,
                shape = RoundedCornerShape(0.dp),
            ) {
                EntryList(
                    editorConf = appState.appConf.editor,
                    viewConf = appState.appConf.view,
                    pinned = true,
                    filterState = state.pinnedEntryListFilterState,
                    project = state.project,
                    jumpToEntry = jumpToEntry,
                    onFocusedChanged = onFocusedChanged,
                    dialogState = appState,
                )
            }
        }
    }

    val position = appState.appConf.view.pinnedEntryListPosition
    val isHorizontal = position == AppConf.ViewSidePosition.Right || position == AppConf.ViewSidePosition.Left
    val isLabelerFirst = position == AppConf.ViewSidePosition.Right || position == AppConf.ViewSidePosition.Bottom
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
            moveEnabled = !locked && appState.isEntryListPinned,
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
                        .fillMaxHeight(),
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
        CircularProgress(blocking = false, darkenBackground = false)
    }
}
