@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.isReleased
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.toRgbColor
import com.sdercolin.vlabeler.util.toRgbColorOrNull
import com.sdercolin.vlabeler.util.updateNonNull

@Immutable
private data class NameLabelEntryChunk(
    val leftEntry: EntryInPixel?,
    val entries: List<EntryInPixel>,
    val rightEntry: EntryInPixel?,
    val includeEditableLabel: Boolean,
)

@Composable
fun NameLabels(
    appConf: AppConf,
    editorState: EditorState,
    state: MarkerState,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
    chunkCount: Int,
    chunkLength: Float,
    chunkVisibleList: List<Boolean>,
) {
    fun convertEntry(entry: IndexedEntry?): EntryInPixel? {
        entry ?: return null
        return entry.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }

    val leftEntry = remember(state.entriesInCurrentGroup, state.entries.first().index) {
        val entry = state.entriesInCurrentGroup.getPreviousOrNull { it.index == state.entries.first().index }
        convertEntry(entry)
    }
    val rightEntry = remember(state.entriesInCurrentGroup, state.entries.last().index) {
        val entry = state.entriesInCurrentGroup.getNextOrNull { it.index == state.entries.last().index }
        convertEntry(entry)
    }

    val chunks = remember(
        leftEntry,
        rightEntry,
        state.entriesInPixel,
        chunkCount,
        chunkLength,
        chunkVisibleList,
        editorState.onScreenScissorsState.isOn,
    ) {
        val totalChunk = NameLabelEntryChunk(leftEntry, state.entriesInPixel, rightEntry, false)
        List(chunkCount) { chunkIndex ->
            val chunkStart = chunkIndex * chunkLength
            val chunkEnd = chunkStart + chunkLength

            fun EntryInPixel.isInChunk() = if (appConf.editor.continuousLabelNames.position.left) {
                this.start >= chunkStart && this.start < chunkEnd
            } else {
                this.end > chunkStart && this.end <= chunkEnd
            }

            val includeEditableLabel = editorState.onScreenScissorsState.isOn &&
                editorState.onScreenScissorsState.pixelPosition >= chunkStart &&
                editorState.onScreenScissorsState.pixelPosition < chunkEnd
            NameLabelEntryChunk(
                leftEntry = leftEntry?.takeIf { it.isInChunk() },
                entries = totalChunk.entries.filter { it.isInChunk() },
                rightEntry = rightEntry?.takeIf { it.isInChunk() },
                includeEditableLabel = includeEditableLabel,
            )
        }
    }
    val lengthBias = chunkLength - chunkLength.toInt()
    Row {
        repeat(chunkCount) { index ->
            val biasFix = ((index + 1) * lengthBias).toInt() - (index * lengthBias).toInt()
            val actualLengthDp = with(LocalDensity.current) {
                (chunkLength.toInt() + biasFix).toDp()
            }
            if (chunkVisibleList[index]) {
                Box(Modifier.fillMaxHeight().requiredWidth(actualLengthDp)) {
                    NameLabelsChunk(
                        appConf = appConf,
                        modifier = Modifier.fillMaxSize(),
                        entryChunk = chunks[index],
                        offset = index * chunkLength,
                        requestRename = requestRename,
                        jumpToEntry = jumpToEntry,
                        onHovered = onHovered,
                    )
                    if (chunks[index].includeEditableLabel) {
                        EditableNameLabel(
                            modifier = Modifier.fillMaxSize(),
                            offset = index * chunkLength,
                            name = editorState.onScreenScissorsState.text,
                            onEditName = { editorState.onScreenScissorsState.text = it },
                            color = appConf.editor.continuousLabelNames.color.toRgbColorOrNull()
                                ?: AppConf.ContinuousLabelNames.DefaultColor.toRgbColor(),
                            cutPosition = editorState.onScreenScissorsState.pixelPosition,
                            appConf = appConf,
                            commit = editorState::commitEntryCut,
                            cancel = {
                                editorState.onScreenScissorsState.end()
                                state.scissorsState.updateNonNull { copy(locked = false) }
                            },
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxHeight().requiredWidth(actualLengthDp))
            }
        }
    }
}

@Composable
private fun NameLabel(
    index: Int,
    name: String,
    color: Color,
    fontSize: AppConf.FontSize,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
) {
    val fontSizeSp = when (fontSize) {
        AppConf.FontSize.Small -> 12.sp
        AppConf.FontSize.Medium -> 14.sp
        AppConf.FontSize.Large -> 16.sp
    }
    Text(
        modifier = Modifier.widthIn(max = 100.dp)
            .wrapContentSize()
            .combinedClickable(
                onClick = { requestRename(index) },
                onLongClick = { jumpToEntry(index) },
            )
            .onPointerEvent(eventType = PointerEventType.Enter) {
                onHovered(index, true)
            }
            .onPointerEvent(eventType = PointerEventType.Exit) {
                onHovered(index, false)
            }
            .padding(vertical = 5.dp, horizontal = 5.dp),
        maxLines = 1,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption.copy(fontSize = fontSizeSp),
    )
}

@Composable
private fun EditableNameLabel(
    modifier: Modifier,
    offset: Float,
    name: String,
    onEditName: (String) -> Unit,
    color: Color,
    cutPosition: Float,
    appConf: AppConf,
    commit: () -> Unit,
    cancel: () -> Unit,
) {
    val fontSizeSp = when (appConf.editor.continuousLabelNames.size) {
        AppConf.FontSize.Small -> 12.sp
        AppConf.FontSize.Medium -> 14.sp
        AppConf.FontSize.Large -> 16.sp
    }
    val placeAtLeft = appConf.editor.scissorsActions.askForName == AppConf.ScissorsActions.Target.Former
    Layout(
        modifier = modifier,
        content = {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            BasicTextField(
                modifier = Modifier.width(IntrinsicSize.Min)
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        when {
                            event.isReleased(Key.Enter) -> {
                                commit()
                                true
                            }
                            event.isReleased(Key.Escape) -> {
                                cancel()
                                true
                            }
                            else -> {
                                false
                            }
                        }
                    },
                value = name,
                onValueChange = onEditName,
                textStyle = MaterialTheme.typography.caption.copy(fontSize = fontSizeSp, color = color),
                singleLine = true,
                cursorBrush = SolidColor(color),
                decorationBox = {
                    Box(
                        modifier = Modifier.widthIn(min = 20.dp, max = 100.dp)
                            .background(color = Color.White.copy(alpha = 0.1f))
                            .padding(vertical = 5.dp, horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        it()
                    }
                },
            )
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = if (placeAtLeft) {
                cutPosition.toInt() - placeable.width - offset
            } else {
                cutPosition.toInt() - offset
            }
            val y = if (appConf.editor.continuousLabelNames.position.top) {
                0
            } else {
                val heightRatio = appConf.painter.amplitudeHeightRatio
                (constraints.maxHeight * heightRatio).toInt() - placeable.height
            }
            placeable.place(x.toInt(), y)
        }
    }
}

@Composable
private fun NameLabelsChunk(
    appConf: AppConf,
    modifier: Modifier,
    entryChunk: NameLabelEntryChunk,
    offset: Float,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
) {
    val items = remember(entryChunk) {
        listOfNotNull(entryChunk.leftEntry) + entryChunk.entries + listOfNotNull(entryChunk.rightEntry)
    }
    val activeColor = appConf.editor.continuousLabelNames.color.toRgbColorOrNull()
        ?: AppConf.ContinuousLabelNames.DefaultColor.toRgbColor()
    val inactiveColor = Black
    val colors = remember(entryChunk, activeColor, inactiveColor) {
        listOfNotNull(entryChunk.leftEntry).map { inactiveColor } +
            entryChunk.entries.map { activeColor } +
            listOfNotNull(entryChunk.rightEntry).map { inactiveColor }
    }

    Layout(
        modifier = modifier,
        content = {
            items.indices.forEach { itemIndex ->
                val item = items[itemIndex]
                val color = colors[itemIndex]
                NameLabel(
                    index = item.index,
                    name = item.name,
                    color = color,
                    fontSize = appConf.editor.continuousLabelNames.size,
                    requestRename = requestRename,
                    jumpToEntry = jumpToEntry,
                    onHovered = onHovered,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val position = appConf.editor.continuousLabelNames.position
                val x = if (position.left) {
                    if (entryChunk.rightEntry != null && index == placeables.lastIndex && index > 0) {
                        // should only happen in continuous mode
                        // so use `end` of the previous one to get immediate update
                        items[index - 1].end - offset
                    } else {
                        items[index].start - offset
                    }
                } else {
                    if (entryChunk.leftEntry != null && index == 0 && placeables.size > 1) {
                        // should only happen in continuous mode
                        // so use `end` of the previous one to get immediate update
                        items[1].start - offset - placeable.width
                    } else {
                        items[index].end - offset - placeable.width
                    }
                }
                val y = if (position.top) {
                    0
                } else {
                    val heightRatio = appConf.painter.amplitudeHeightRatio
                    (constraints.maxHeight * heightRatio).toInt() - placeable.height
                }
                placeable.place(x.toInt(), y)
            }
        }
    }
}
