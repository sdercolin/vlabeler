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
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.toRgbColor
import com.sdercolin.vlabeler.util.toRgbColorOrNull

@Immutable
private data class NameLabelEntryChunk(
    val leftEntry: EntryInPixel?,
    val entries: List<EntryInPixel>,
    val rightEntry: EntryInPixel?,
)

@Composable
fun NameLabels(
    appConf: AppConf,
    state: MarkerState,
    requestRename: (Int) -> Unit,
    jumpToEntry: (Int) -> Unit,
    onHovered: (Int, Boolean) -> Unit,
    chunkCount: Int,
    chunkLength: Float,
    chunkLengthDp: Dp,
    chunkVisibleList: List<Boolean>,
) {
    val leftEntry = remember(state.entriesInCurrentGroup, state.entries.first().index) {
        val entry = state.entriesInCurrentGroup.getPreviousOrNull { it.index == state.entries.first().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }
    val rightEntry = remember(state.entriesInCurrentGroup, state.entries.last().index) {
        val entry = state.entriesInCurrentGroup.getNextOrNull { it.index == state.entries.last().index }
        entry?.let { state.entryConverter.convertToPixel(it, state.sampleLengthMillis) }
    }

    val chunks = remember(leftEntry, rightEntry, state.entriesInPixel, chunkCount, chunkLength, chunkVisibleList) {
        val totalChunk = NameLabelEntryChunk(leftEntry, state.entriesInPixel, rightEntry)
        List(chunkCount) { chunkIndex ->
            val chunkStart = chunkIndex * chunkLength
            val chunkEnd = chunkStart + chunkLength

            fun EntryInPixel.isInChunk() = if (appConf.editor.continuousLabelNames.position.left) {
                this.start >= chunkStart && this.start < chunkEnd
            } else {
                this.end > chunkStart && this.end <= chunkEnd
            }

            NameLabelEntryChunk(
                leftEntry = leftEntry?.takeIf { it.isInChunk() },
                entries = totalChunk.entries.filter { it.isInChunk() },
                rightEntry = rightEntry?.takeIf { it.isInChunk() },
            )
        }
    }

    val modifier = Modifier.fillMaxHeight().requiredWidth(chunkLengthDp)
    Row {
        repeat(chunkCount) { index ->
            if (chunkVisibleList[index]) {
                NameLabelsChunk(
                    appConf = appConf,
                    modifier = modifier,
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
            .padding(vertical = 2.dp, horizontal = 5.dp),
        maxLines = 1,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption.copy(fontSize = fontSizeSp),
    )
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
