package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalLanguage
import com.sdercolin.vlabeler.util.toColor

@Immutable
private data class FieldLabelModel(
    val entryAbsoluteIndex: Int,
    val position: Float,
    val field: LabelerConf.Field,
    val isActive: Boolean,
)

@Immutable
private data class FieldLabelModelChunk(
    val models: List<FieldLabelModel>,
)

@Composable
fun FieldLabels(
    state: MarkerState,
    chunkCount: Int,
    chunkLength: Float,
    chunkVisibleList: List<Boolean>,
) {
    val labelIndexes = remember(state.entriesInPixel.indices, state.labelerConf.fields) {
        state.entriesInPixel.indices.flatMap { entryIndex ->
            state.labelerConf.fields.indices.map { fieldIndex ->
                entryIndex to fieldIndex
            }
        }
    }
    val chunks = remember(
        labelIndexes,
        state.entriesInPixel,
        state.labelerConf,
        state.cursorState.value.pointIndex,
        chunkCount,
        chunkLength,
    ) {
        val fields = state.labelerConf.fields
        val models = labelIndexes.map { (entryIndex, fieldIndex) ->
            val pointIndex = fieldIndex + entryIndex * (fields.size + 1)
            FieldLabelModel(
                entryAbsoluteIndex = state.entriesInPixel[entryIndex].index,
                position = state.entriesInPixel[entryIndex].points[fieldIndex],
                field = fields[fieldIndex],
                isActive = state.cursorState.value.pointIndex == pointIndex,
            )
        }
        val groups = models.groupBy { (it.position / chunkLength).toInt().coerceAtMost(chunkCount - 1) }
        List(chunkCount) { FieldLabelModelChunk(groups[it]?.toList() ?: listOf()) }
    }

    val lengthBias = chunkLength - chunkLength.toInt()
    Row {
        repeat(chunkCount) { index ->
            val biasFix = ((index + 1) * lengthBias).toInt() - (index * lengthBias).toInt()
            val actualLengthDp = with(LocalDensity.current) {
                (chunkLength.toInt() + biasFix).toDp()
            }
            if (chunkVisibleList[index]) {
                FieldLabelsChunk(
                    modifier = Modifier.fillMaxHeight().requiredWidth(actualLengthDp),
                    offset = index * chunkLength,
                    modelChunk = chunks[index],
                    waveformsHeightRatio = state.waveformsHeightRatio,
                )
            } else {
                Box(Modifier.fillMaxHeight().requiredWidth(actualLengthDp))
            }
        }
    }
}

@Composable
private fun FieldLabelsChunk(
    modifier: Modifier,
    offset: Float,
    modelChunk: FieldLabelModelChunk,
    waveformsHeightRatio: Float,
) {
    val labelShiftUp = with(LocalDensity.current) { LabelShiftUp.toPx() }
    Layout(
        modifier = modifier,
        content = {
            modelChunk.models
                .filterNot { it.field.replaceStart || it.field.replaceEnd }
                .forEach { model ->
                    Box(
                        modifier = Modifier.requiredSize(LabelSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        FieldLabelText(model)
                    }
                }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val model = modelChunk.models[index]
                val position = model.position
                val x = position - (constraints.maxWidth) / 2 - offset
                val canvasHeight = constraints.maxHeight.toFloat()
                val waveformsHeight = canvasHeight * waveformsHeightRatio
                val restCanvasHeight = canvasHeight - waveformsHeight
                val height = waveformsHeight * model.field.height + restCanvasHeight
                val y = canvasHeight - height - labelShiftUp - canvasHeight / 2
                placeable.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun FieldLabelText(model: FieldLabelModel) {
    val alpha = if (model.isActive) 1f else IDLE_LINE_ALPHA
    val fontSize = if (LocalLanguage.current in listOf(Language.ChineseSimplified)) {
        // Show Chinese characters in bigger size
        16.sp
    } else {
        14.sp
    }
    Text(
        text = model.field.label.get(),
        textAlign = TextAlign.Center,
        color = model.field.color.toColor().copy(alpha = alpha),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.caption.copy(fontSize = fontSize),
        overflow = TextOverflow.Visible,
    )
}
