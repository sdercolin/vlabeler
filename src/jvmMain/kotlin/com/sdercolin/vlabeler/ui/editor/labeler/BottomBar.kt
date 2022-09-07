package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.AppState

@Composable
fun BottomBar(state: BottomBarState, appState: AppState) {
    LaunchedEffect(appState.keyboardViewModel.keyboardActionFlow) {
        appState.keyboardViewModel.keyboardActionFlow.collect {
            if (appState.isEditorActive.not()) return@collect
            if (it == KeyAction.InputResolution) {
                state.openSetResolutionDialog()
            }
        }
    }
    Surface {
        Row(
            modifier = Modifier.fillMaxWidth().height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canGoPrevious,
                        onClick = state.goPreviousSample,
                    )
                    .padding(start = 8.dp),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "<<",
                    style = MaterialTheme.typography.caption,
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canGoPrevious,
                        onClick = state.goPreviousEntry,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "<",
                    style = MaterialTheme.typography.caption,
                )
            }
            Box(
                Modifier.fillMaxHeight()
                    .widthIn(min = 85.dp)
                    .clickable { state.openJumpToEntryDialog() }
                    .padding(horizontal = 5.dp),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "${state.currentEntryIndexInTotal + 1} / ${state.totalEntryCount}",
                    style = MaterialTheme.typography.caption,
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canGoNext,
                        onClick = state.goNextEntry,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = ">",
                    style = MaterialTheme.typography.caption,
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canGoNext,
                        onClick = state.goNextSample,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = ">>",
                    style = MaterialTheme.typography.caption,
                )
            }

            Spacer(Modifier.weight(0.8f))

            Box(
                Modifier.fillMaxHeight()
                    .clickable { state.scrollFit() }
                    .padding(horizontal = 15.dp),
            ) {
                Icon(
                    modifier = Modifier.size(15.dp).align(Alignment.Center),
                    imageVector = Icons.Default.CenterFocusWeak,
                    contentDescription = null,
                )
            }
            if (state.isMultipleEditModeEnabled) {
                Box(
                    Modifier.fillMaxHeight()
                        .clickable { state.toggleMultipleEditMode() }
                        .padding(horizontal = 15.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(15.dp).align(Alignment.Center).rotate(90f),
                        imageVector = if (state.isMultipleEditMode) {
                            Icons.Default.UnfoldLess
                        } else {
                            Icons.Default.Expand
                        },
                        contentDescription = null,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canIncrease,
                        onClick = state::increase,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "-",
                    style = MaterialTheme.typography.caption,
                )
            }
            Box(
                Modifier.fillMaxHeight()
                    .clickable(onClick = state.openSetResolutionDialog),
            ) {
                Text(
                    modifier = Modifier.defaultMinSize(minWidth = 55.dp).align(Alignment.Center),
                    text = "1/${state.resolution}",
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = state.canDecrease,
                        onClick = state::decrease,
                    )
                    .padding(end = 8.dp),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "+",
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}
