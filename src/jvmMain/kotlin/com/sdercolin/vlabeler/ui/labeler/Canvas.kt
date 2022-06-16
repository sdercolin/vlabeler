package com.sdercolin.vlabeler.ui.labeler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
                    Spectrogram(canvasParams, it)
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
    val dataDensity = appConf.painter.amplitude.unitSize
    val data = channel.data
    val waveformsColor = MaterialTheme.colors.onBackground
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val imageBitmap = remember(channel, appConf) { mutableStateOf<ImageBitmap?>(null) }
    val width = data.size / dataDensity
    val maxRawY = data.maxOfOrNull { it.absoluteValue } ?: 0f
    val height = 100
    val size = Size(width.toFloat(), height.toFloat())

    LaunchedEffect(Unit) {
        val newBitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
            val yScale = maxRawY / height * 2 * (1 + appConf.painter.amplitude.yAxisBlankRate)
            val points = data
                .map { height / 2 - it / yScale }
                .withIndex().map { Offset(it.index.toFloat() / dataDensity, it.value) }
            drawPoints(points, pointMode = PointMode.Polygon, color = waveformsColor)
        }
        imageBitmap.value = newBitmap
    }


    imageBitmap.value?.let {
        Image(
            modifier = Modifier.fillMaxHeight()
                .width(canvasParams.canvasWidthInDp)
                .background(MaterialTheme.colors.background),
            contentScale = ContentScale.FillBounds,
            bitmap = it,
            contentDescription = null
        )
    }
}

@Composable
private fun Spectrogram(
    canvasParams: CanvasParams,
    spectrogram: Array<DoubleArray>
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val imageBitmap = remember(spectrogram) { mutableStateOf<ImageBitmap?>(null) }
    val width = spectrogram.size.toFloat()
    val height = spectrogram.first().size.toFloat()
    val size = Size(width, height)

    LaunchedEffect(Unit) {
        val newBitmap = ImageBitmap(width.toInt(), height.toInt())
        CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
            spectrogram.forEachIndexed { xIndex, yArray ->
                yArray.forEachIndexed { yIndex, z ->
                    val color = Color.White.copy(alpha = z.toFloat())
                    drawRect(
                        color = color,
                        topLeft = Offset(xIndex.toFloat(), height - yIndex.toFloat()),
                        size = Size(1f, 1f)
                    )
                }
            }
        }
        imageBitmap.value = newBitmap
    }

    imageBitmap.value?.let {
        Image(
            modifier = Modifier.fillMaxHeight()
                .width(canvasParams.canvasWidthInDp)
                .background(MaterialTheme.colors.background),
            contentScale = ContentScale.FillBounds,
            bitmap = it,
            contentDescription = null
        )
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
