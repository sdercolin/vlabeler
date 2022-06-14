package com.sdercolin.vlabeler.ui.labeler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerCanvas
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun Scroller(
    sample: Sample,
    appConf: AppConf,
    labelerConf: LabelerConf,
    playerState: PlayerState,
    playSampleSection: (Float, Float) -> Unit,
    keyboardState: KeyboardState,
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState
) {
    val dummyEntry = Entry("i あ", 2615f, 3315f, listOf(3055f, 2915f, 2715f))
    var entry: Entry? by remember { mutableStateOf(dummyEntry) }
    Box(Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
        Column(Modifier.fillMaxSize()) {
            sample.wave.channels.forEach { channel ->
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    WaveChannelCanvas(appConf, canvasParams, channel)
                }
            }
            sample.spectrogram?.let {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    SpectrogramCanvas(canvasParams, it)
                }
            }
        }
        entry?.let {
            MarkerCanvas(
                canvasParams = canvasParams,
                labelerConf = labelerConf,
                keyboardState = keyboardState,
                sampleRate = sample.info.sampleRate,
                entry = it,
                editEntry = { newClip -> entry = newClip },
                playSampleSection = playSampleSection
            )
        }
        if (playerState.isPlaying) {
            PlayerCursorCanvas(canvasParams, playerState)
        }
    }
}

@Composable
private fun WaveChannelCanvas(
    appConf: AppConf,
    canvasParams: CanvasParams,
    channel: Wave.Channel
) {
    val step = canvasParams.resolution / appConf.painter.dataDensity
    val actualDataDensity = canvasParams.resolution / step
    val data = channel.data
        .slice(channel.data.indices step step)

    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        val centerY: Float = center.y
        val maxRawY = channel.data.maxOfOrNull { it.absoluteValue } ?: 0
        val yScale = maxRawY.toFloat() / centerY * 1.2f
        val points = data
            .map { centerY - it / yScale }
            .withIndex().map { Offset(it.index.toFloat() / actualDataDensity, it.value) }
        drawPoints(points, pointMode = PointMode.Polygon, color = Color(0xFF050505))
    }
}

@Composable
private fun SpectrogramCanvas(
    canvasParams: CanvasParams,
    spectrogram: Array<DoubleArray>
) {
    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        val unitWidth = 512f / canvasParams.resolution
        val unitHeight = size.height * 2 / 512f
        spectrogram.forEachIndexed { xIndex, yArray ->
            yArray.forEachIndexed { yIndex, z ->
                val left = xIndex * unitWidth
                val top = size.height - unitHeight * yIndex - unitHeight
                val gray = ((1 - z) * 255).toInt()
                val color = Color(gray, gray, gray)
                drawRect(color = color, topLeft = Offset(left, top), Size(unitWidth, unitHeight))
            }
        }
    }
}

@Composable
private fun PlayerCursorCanvas(canvasParams: CanvasParams, playerState: PlayerState) {
    Canvas(
        Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp)
    ) {
        val x = (playerState.framePosition / canvasParams.resolution).toFloat()
        drawLine(
            color = Color.Green,
            start = Offset(x, 0f),
            end = Offset(x, center.y * 2),
            strokeWidth = 2f
        )
    }
}
