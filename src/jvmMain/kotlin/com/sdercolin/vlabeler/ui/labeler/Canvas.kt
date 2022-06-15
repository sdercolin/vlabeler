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

@Composable
fun Canvas(
    sample: Sample,
    entry: Entry,
    editEntry: (Entry) -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    appConf: AppConf,
    labelerConf: LabelerConf,
    canvasParams: CanvasParams,
    playerState: PlayerState,
    keyboardState: KeyboardState,
    horizontalScrollState: ScrollState
) {
    Box(Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
        Column(Modifier.fillMaxSize()) {
            val weightOfEachChannel = 1f / sample.wave.channels.size
            sample.wave.channels.forEach { channel ->
                Box(Modifier.weight(weightOfEachChannel).fillMaxWidth()) {
                    Waveforms(appConf, canvasParams, channel)
                }
            }
            sample.spectrogram?.let {
                Box(Modifier.weight(appConf.painter.spectrogram.heightWeight).fillMaxWidth()) {
                    Spectrogram(appConf, canvasParams, it)
                }
            }
        }
        MarkerCanvas(
            entry = entry,
            editEntry = editEntry,
            playSampleSection = playSampleSection,
            appConf = appConf,
            labelerConf = labelerConf,
            canvasParams = canvasParams,
            sampleRate = sample.info.sampleRate,
            keyboardState = keyboardState
        )
        if (playerState.isPlaying) {
            PlayerCursor(canvasParams, playerState)
        }
    }
}

@Composable
private fun Waveforms(
    appConf: AppConf,
    canvasParams: CanvasParams,
    channel: Wave.Channel
) {
    val step = canvasParams.resolution / appConf.painter.amplitude.dataDensity
    val actualDataDensity = canvasParams.resolution / step
    val data = channel.data
        .slice(channel.data.indices step step)
    val waveformsColor = MaterialTheme.colors.onBackground
    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        val centerY: Float = center.y
        val maxRawY = channel.data.maxOfOrNull { it.absoluteValue } ?: 0
        val yScale = maxRawY.toFloat() / centerY * (1 + appConf.painter.amplitude.yAxisBlankRate)
        val points = data
            .map { centerY - it / yScale }
            .withIndex().map { Offset(it.index.toFloat() / actualDataDensity, it.value) }
        drawPoints(points, pointMode = PointMode.Polygon, color = waveformsColor)
    }
}

@Composable
private fun Spectrogram(
    appConf: AppConf,
    canvasParams: CanvasParams,
    spectrogram: Array<DoubleArray>
) {
    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        val frameSize = appConf.painter.spectrogram.frameSize.toFloat()
        val unitWidth = frameSize / canvasParams.resolution
        val unitHeight = size.height / spectrogram.first().size
        spectrogram.forEachIndexed { xIndex, yArray ->
            yArray.forEachIndexed { yIndex, z ->
                if (z > 0.0) {
                    val left = xIndex * unitWidth
                    val top = size.height - unitHeight * yIndex - unitHeight
                    val color = Color.White.copy(alpha = z.toFloat())
                    drawRect(color = color, topLeft = Offset(left, top), Size(unitWidth, unitHeight))
                }
            }
        }
    }
}

@Composable
private fun PlayerCursor(canvasParams: CanvasParams, playerState: PlayerState) {
    Canvas(
        Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp)
    ) {
        val x = (playerState.framePosition / canvasParams.resolution).toFloat()
        drawLine(
            color = Color.Yellow,
            start = Offset(x, 0f),
            end = Offset(x, center.y * 2),
            strokeWidth = 2f
        )
    }
}
