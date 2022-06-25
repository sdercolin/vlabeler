package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.Spectrogram
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCanvas
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.ui.theme.Yellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.ceil

@Composable
fun Canvas(
    sample: Sample?,
    entry: Entry,
    entriesInSample: List<Entry>,
    currentIndexInSample: Int,
    isBusy: Boolean,
    editEntry: (Entry) -> Unit,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    appConf: AppConf,
    labelerConf: LabelerConf,
    resolution: Int,
    playerState: PlayerState,
    horizontalScrollState: ScrollState,
    keyboardViewModel: KeyboardViewModel,
    scrollFitViewModel: ScrollFitViewModel
) {
    val currentDensity = LocalDensity.current

    if (sample != null) {
        val chunkCount = remember(sample, appConf) {
            ceil(sample.wave.length.toFloat() / appConf.painter.maxDataChunkSize).toInt()
        }
        val canvasParams = CanvasParams(sample.wave.length, resolution, currentDensity)
        if (canvasParams.lengthInPixel > CanvasParams.MaxCanvasLengthInPixel) {
            LengthOverflowError()
        } else {
            Box(Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
                Column(Modifier.fillMaxSize()) {
                    val weightOfEachChannel = 1f / sample.wave.channels.size
                    sample.wave.channels.forEach { channel ->
                        Box(Modifier.weight(weightOfEachChannel).fillMaxWidth()) {
                            Waveforms(appConf, canvasParams, channel, chunkCount)
                        }
                    }
                    sample.spectrogram?.let {
                        Box(Modifier.weight(appConf.painter.spectrogram.heightWeight).fillMaxWidth()) {
                            Spectrogram(canvasParams, it, chunkCount)
                        }
                    }
                }
                MarkerCanvas(
                    entry = entry,
                    entriesInSample = entriesInSample,
                    currentIndexInSample = currentIndexInSample,
                    sampleLengthMillis = sample.info.lengthMillis,
                    isBusy = isBusy,
                    editEntry = editEntry,
                    submitEntry = submitEntry,
                    playSampleSection = playSampleSection,
                    appConf = appConf,
                    labelerConf = labelerConf,
                    canvasParams = canvasParams,
                    sampleRate = sample.info.sampleRate,
                    horizontalScrollState = horizontalScrollState,
                    keyboardViewModel = keyboardViewModel,
                    scrollFitViewModel = scrollFitViewModel
                )
                if (playerState.isPlaying) {
                    PlayerCursor(canvasParams, playerState)
                }
            }
        }
    }
}

@Composable
private fun Waveforms(
    appConf: AppConf,
    canvasParams: CanvasParams,
    channel: Wave.Channel,
    chunkCount: Int
) {
    Log.info("Waveforms: composed")
    val chunkSize = remember(channel, chunkCount) { channel.data.size / chunkCount }
    val dataChunks = remember(channel, chunkSize) { channel.data.chunked(chunkSize) }
    val waveformsColor = MaterialTheme.colors.onBackground
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val imageBitmaps = remember(channel, appConf) { List(chunkSize) { mutableStateOf<ImageBitmap?>(null) } }

    LaunchedEffect(channel) {
        withContext(Dispatchers.IO) {
            repeat(chunkCount) { i ->
                val data = dataChunks[i]
                val dataDensity = appConf.painter.amplitude.unitSize
                val width = data.size / dataDensity
                val maxRawY = data.maxOfOrNull { it.absoluteValue } ?: 0f
                val height = appConf.painter.amplitude.intensityAccuracy
                val size = Size(width.toFloat(), height.toFloat())
                val newBitmap = ImageBitmap(width, height)
                CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
                    Log.info("Waveforms chunk $i: draw bitmap")
                    val yScale = maxRawY / height * 2 * (1 + appConf.painter.amplitude.yAxisBlankRate)
                    val points = data
                        .map { height / 2 - it / yScale }
                        .withIndex().map { Offset(it.index.toFloat() / dataDensity, it.value) }
                    drawPoints(points, pointMode = PointMode.Polygon, color = waveformsColor)
                }
                imageBitmaps[i].value = newBitmap
            }
        }
    }
    Row(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        repeat(chunkCount) { i ->
            println("WaveformsChunk $i: composed")
            ChunkImage(canvasParams, chunkCount, imageBitmaps[i], i, "Waveforms")
        }
    }
}

@Composable
private fun Spectrogram(
    canvasParams: CanvasParams,
    spectrogram: Spectrogram,
    chunkCount: Int
) {
    Log.info("Spectrogram: composed")
    val data = spectrogram.data
    val chunkSize = remember(spectrogram, chunkCount) { data.size / chunkCount }
    val dataChunks = remember(spectrogram, chunkSize) { data.toList().chunked(chunkSize) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val imageBitmaps = remember(spectrogram) { List(chunkSize) { mutableStateOf<ImageBitmap?>(null) } }

    LaunchedEffect(spectrogram) {
        withContext(Dispatchers.IO) {
            repeat(chunkCount) { i ->
                val chunk = dataChunks[i]
                val width = chunk.size.toFloat()
                val height = chunk.first().size.toFloat()
                val size = Size(width, height)
                val newBitmap = ImageBitmap(width.toInt(), height.toInt())
                println("Spectrogram chunk $i: draw bitmap")
                CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
                    chunk.forEachIndexed { xIndex, yArray ->
                        yArray.forEachIndexed { yIndex, z ->
                            val color = White.copy(alpha = z.toFloat())
                            drawRect(
                                color = color,
                                topLeft = Offset(xIndex.toFloat(), height - yIndex.toFloat()),
                                size = Size(1f, 1f)
                            )
                        }
                    }
                }
                imageBitmaps[i].value = newBitmap
            }
        }
    }
    Row(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .background(MaterialTheme.colors.background)
    ) {
        repeat(chunkCount) { i ->
            ChunkImage(canvasParams, chunkCount, imageBitmaps[i], i, "Spectrogram")
        }
    }
}

@Composable
private fun ChunkImage(
    canvasParams: CanvasParams,
    chunkCount: Int,
    bitmap: State<ImageBitmap?>,
    index: Int,
    chunkType: String
) {
    Log.info("$chunkType chunk $index: composed")
    bitmap.value?.let {
        Image(
            modifier = Modifier.fillMaxHeight()
                .width(canvasParams.canvasWidthInDp / chunkCount),
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
            color = Yellow,
            start = Offset(x, 0f),
            end = Offset(x, center.y * 2),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun LengthOverflowError() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.error.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.widthIn(max = 600.dp),
            text = string(Strings.CanvasLengthOverflowError),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground
        )
    }
}
