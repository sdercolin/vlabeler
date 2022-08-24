@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull

@Immutable
private data class NameLabelEntryChunk(
    val leftEntry: EntryInPixel?,
    val entries: List<EntryInPixel>,
    val rightEntry: EntryInPixel?,
)

@Composable
fun NameLabels(
    state: MarkerState,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
    chunkCount: Int,
    chunkLength: Float,
    chunkLengthDp: Dp,
    chunkVisibleList: List<Boolean>,
) {
    val leftEntry = remember(state.entriesInSample, state.entries.first().index) {
        val entry = state.entriesInSample.getPreviousOrNull { it.index == state.entries.first().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }
    val rightEntry = remember(state.entriesInSample, state.entries.last().index) {
        val entry = state.entriesInSample.getNextOrNull { it.index == state.entries.last().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }

    val chunks = remember(leftEntry, rightEntry, state.entriesInPixel, chunkCount, chunkLength, chunkVisibleList) {
        val totalChunk = NameLabelEntryChunk(leftEntry, state.entriesInPixel, rightEntry)
        List(chunkCount) { chunkIndex ->
            val chunkStart = chunkIndex * chunkLength
            val chunkEnd = chunkStart + chunkLength
            NameLabelEntryChunk(
                leftEntry = leftEntry?.takeIf { it.start >= chunkStart && it.start < chunkEnd },
                entries = totalChunk.entries.filter { it.start >= chunkStart && it.start < chunkEnd },
                rightEntry = rightEntry?.takeIf { it.start >= chunkStart && it.start < chunkEnd },
            )
        }
    }

    val modifier = Modifier.fillMaxHeight().requiredWidth(chunkLengthDp)
    Row {
        repeat(chunkCount) { index ->
            if (chunkVisibleList[index]) {
                NameLabelsChunk(
                    modifier = modifier,
                    index = index,
                    entryChunk = chunks[index],
                    offset = index * chunkLength,
                    requestRename = requestRename,
                    jumpToEntry = jumpToEntry,
                    onHovered = onHovered,
                )
            } else {
                Box(modifier)
            }
        }
    }
}

@Composable
private fun NameLabel(
    index: Int,
    name: String,
    color: Color,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
) {
    Log.info("NameLabel of entry $index composed")
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
            .padding(vertical = 2.dp, horizontal = 5.dp),
        maxLines = 1,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption,
    )
}

@Composable
private fun NameLabelsChunk(
    modifier: Modifier,
    index: Int,
    entryChunk: NameLabelEntryChunk,
    offset: Float,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
) {
    Log.info("NameLabelsChunk $index composed")
    val items = remember(entryChunk) {
        listOfNotNull(entryChunk.leftEntry) + entryChunk.entries + listOfNotNull(entryChunk.rightEntry)
    }
    val colors = remember(entryChunk) {
        listOfNotNull(entryChunk.leftEntry).map { Black } +
            entryChunk.entries.map { DarkYellow } +
            listOfNotNull(entryChunk.rightEntry).map { Black }
    }

    Layout(
        modifier = modifier,
        content = {
            items.indices.forEach { itemIndex ->
                val item = items[itemIndex]
                val color = colors[itemIndex]
                NameLabel(item.index, item.name, color, requestRename, jumpToEntry, onHovered)
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val x = if (entryChunk.rightEntry != null && index == placeables.lastIndex) {
                    // should only happen in continuous mode
                    // so use `start` of the previous one to get immediate update
                    items[index - 1].end - offset
                } else {
                    items[index].start - offset
                }
                placeable.place(x.toInt(), 0)
            }
        }
    }
}
