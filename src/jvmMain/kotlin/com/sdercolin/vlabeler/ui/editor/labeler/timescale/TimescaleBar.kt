package com.sdercolin.vlabeler.ui.editor.labeler.timescale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.getTimeText
import kotlin.math.ceil
import kotlin.math.floor

class TimescaleBarState(
    sampleRate: Float,
    resolution: Int,
    screenRange: FloatRange,
) {

    val scalePositions: List<Pair<Float, String?>>
    val scalePositionsWithTexts: List<Pair<Float, String>>

    init {
        val converter = EntryConverter(sampleRate, resolution)
        val timescale = Timescale.find { converter.convertToPixel(it) }
        val offset = screenRange.start
        val majorStepPx = converter.convertToPixel(timescale.major.toFloat())
        val minorStepPx = converter.convertToPixel(timescale.minor.toFloat())
        val firstMajorPointIndex = ceil(offset / majorStepPx).toInt()
        val lastMajorPointIndex = floor(screenRange.endInclusive / majorStepPx).toInt()
        val majorIndexes = (firstMajorPointIndex..lastMajorPointIndex)

        val majorResults = majorIndexes.map { index ->
            val text = getTimeText(index * timescale.major)
            val position = index * majorStepPx - offset
            position to text
        }
        val firstMinorPointIndex = ceil(offset / minorStepPx).toInt()
        val lastMinorPointIndex = floor(screenRange.endInclusive / minorStepPx).toInt()
        val minorPoints = (firstMinorPointIndex..lastMinorPointIndex).map { it * minorStepPx }
        val minorResults = minorPoints.map { it - offset }
        val result = majorResults.toMap<Float, String?>().toMutableMap()
        minorResults.forEach {
            if (!result.containsKey(it)) {
                result[it] = null as String?
            }
        }
        scalePositions = result.toList().sortedBy { it.first }
        scalePositionsWithTexts = majorResults.sortedBy { it.first }
    }
}

@Composable
private fun rememberTimescaleBarState(
    editorState: EditorState,
    horizontalScrollState: ScrollState,
): TimescaleBarState? {
    val sampleInfo = editorState.getSampleInfo()
    val resolution = editorState.canvasResolution
    val screenRange = if (sampleInfo != null) {
        val canvasParams = CanvasParams(sampleInfo.length, sampleInfo.chunkCount, resolution)
        editorState.getScreenRange(canvasParams.lengthInPixel, horizontalScrollState)
    } else {
        null
    }
    val sampleRate = sampleInfo?.sampleRate
    return remember(sampleRate, resolution, screenRange) {
        if (sampleRate != null && screenRange != null) {
            TimescaleBarState(
                sampleRate,
                resolution,
                screenRange,
            )
        } else {
            null
        }
    }
}

private val TotalHeight = 40.dp
private val MajorScaleHeight = 16.dp
private val MinorScaleHeight = 8.dp
private val TextBottomMargin = 5.dp

@Composable
fun TimescaleBar(
    editorState: EditorState,
    horizontalScrollState: ScrollState,
    state: TimescaleBarState? = rememberTimescaleBarState(editorState, horizontalScrollState),
) {
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxWidth().height(TotalHeight)) {
        val majorScaleLength = with(density) { MajorScaleHeight.toPx().toInt() }
        val minorScaleLength = with(density) { MinorScaleHeight.toPx().toInt() }
        if (state?.scalePositions != null) {
            Canvas(Modifier.fillMaxSize()) {
                val height = size.height
                try {
                    for ((position, text) in state.scalePositions) {
                        val isMajor = text != null
                        val y = height - (if (isMajor) majorScaleLength else minorScaleLength)
                        drawLine(
                            color = Color.White,
                            start = Offset(position, y),
                            end = Offset(position, height),
                            strokeWidth = 3f,
                        )
                    }
                } catch (t: Throwable) {
                    if (isDebug) throw t
                    Log.debug(t)
                }
            }
        }
        if (state?.scalePositionsWithTexts != null) {
            val yBottom = with(density) { (TotalHeight - MajorScaleHeight - TextBottomMargin).toPx() }
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    state.scalePositionsWithTexts.forEach { (_, text) ->
                        BasicText(
                            text = text,
                            style = MaterialTheme.typography.caption.copy(color = Color.White, fontSize = 10.sp),
                        )
                    }
                },
            ) { measureables, constraints ->
                val placeables = measureables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEachIndexed { index, placeable ->
                        val x = state.scalePositionsWithTexts[index].first - placeable.width / 2
                        val y = yBottom - placeable.height
                        placeable.placeRelative(x = x.toInt(), y = y.toInt())
                    }
                }
            }
        }
    }
}
