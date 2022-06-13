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
import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerCanvas
import kotlin.math.absoluteValue

@Composable
fun Scroller(
    sample: Sample,
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
                    WaveChannelCanvas(canvasParams, channel)
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
    canvasParams: CanvasParams,
    channel: Wave.Channel
) {
    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        val data = channel.data
        val maxY = data.maxOfOrNull { it.absoluteValue } ?: 0
        val yScale = maxY.toFloat() / center.y * 1.5f
        var i = 0
        var oldX = 0f
        var oldY = center.y
        var xIndex = 0f
        while (i < data.size) {
            val rawY = data[i]
            val y = center.y - rawY / yScale
            drawLine(
                Color.Black,
                start = Offset(oldX, oldY),
                end = Offset(xIndex, y),
                strokeWidth = 1f
            )
            i += canvasParams.resolution
            oldX = xIndex
            oldY = y
            xIndex++
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
