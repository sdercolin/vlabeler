package com.sdercolin.vlabeler.ui.editor.labeler.timescale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.getScreenRange
import com.sdercolin.vlabeler.util.getTimeText
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine
import kotlin.math.ceil
import kotlin.math.floor

class TimescaleBarState(
    sampleRate: Float?,
    resolution: Int,
    screenRange: FloatRange?,
) {

    val scalePositionsWithTexts: List<Pair<Float, String?>>?

    init {
        if (sampleRate == null || screenRange == null) {
            scalePositionsWithTexts = null
        } else {
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
            scalePositionsWithTexts = result.toList().sortedBy { it.first }
        }
    }
}

@Composable
private fun rememberTimescaleBarState(
    editorState: EditorState,
    horizontalScrollState: ScrollState,
): TimescaleBarState {
    val sampleInfo = editorState.sampleInfoResult?.getOrNull()
    val resolution = editorState.canvasResolution
    val screenRange = if (sampleInfo != null) {
        val canvasParams = CanvasParams(sampleInfo.length, sampleInfo.chunkCount, resolution, LocalDensity.current)
        horizontalScrollState.getScreenRange(canvasParams.lengthInPixel)
    } else {
        null
    }
    return remember(sampleInfo, resolution, screenRange) {
        TimescaleBarState(
            sampleInfo?.sampleRate,
            resolution,
            screenRange,
        )
    }
}

@Composable
fun TimescaleBar(
    editorState: EditorState,
    horizontalScrollState: ScrollState,
    state: TimescaleBarState = rememberTimescaleBarState(editorState, horizontalScrollState),
) {
    if (state.scalePositionsWithTexts != null) {
        Canvas(Modifier.fillMaxWidth().height(35.dp)) {
            val height = size.height
            try {
                for ((position, text) in state.scalePositionsWithTexts) {
                    val isMajor = text != null
                    val y = if (isMajor) height * 0.3f else height * 0.85f
                    drawLine(
                        color = Color.White,
                        start = Offset(position, y),
                        end = Offset(position, height),
                        strokeWidth = 3f,
                    )
                    if (text != null) {
                        drawIntoCanvas {
                            it.nativeCanvas.drawTextLine(
                                TextLine.Companion.make(text, Font(null, 20f)),
                                position + 10,
                                35f,
                                Paint().apply {
                                    color = Color.White.toArgb()
                                },
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                if (isDebug) throw t
                Log.debug(t)
            }
        }
    }
}
