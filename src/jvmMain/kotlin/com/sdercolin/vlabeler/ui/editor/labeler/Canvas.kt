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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCanvas
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerLabels
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerPointEventContainer
import com.sdercolin.vlabeler.ui.editor.labeler.marker.rememberMarkerState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Yellow
import com.sdercolin.vlabeler.util.getScreenRange
import kotlin.math.ceil

@Composable
fun Canvas(
    horizontalScrollState: ScrollState,
    editorState: EditorState,
    appState: AppState
) {
    val currentDensity = LocalDensity.current
    val sampleResult = editorState.sampleResult
    val resolution = editorState.canvasResolution

    if (sampleResult != null) {
        val sample = sampleResult.getOrNull()
        if (sample != null) {
            val chunkCount = remember(sample, appState.appConf) {
                ceil(sample.wave.length.toFloat() / appState.appConf.painter.maxDataChunkSize).toInt()
            }
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            LaunchedEffect(sample) {
                editorState.renderCharts(this, chunkCount, sample, appState.appConf, density, layoutDirection)
            }
            val canvasParams = CanvasParams(sample.wave.length, resolution, currentDensity)
            val markerState = rememberMarkerState(sample, canvasParams, editorState, appState)
            val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()
            val screenRange = horizontalScrollState.getScreenRange(markerState.canvasParams.lengthInPixel)

            MarkerPointEventContainer(
                screenRange,
                keyboardState,
                markerState,
                editorState,
                appState
            ) {
                Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
                    Row {
                        repeat(chunkCount) { chunkIndex ->
                            Chunk(
                                chunkIndex,
                                chunkCount,
                                canvasParams,
                                sample,
                                appState,
                                editorState
                            )
                        }
                    }
                    if (appState.isMarkerDisplayed) {
                        MarkerLabels(screenRange, appState, markerState)
                    }
                }
                if (appState.isMarkerDisplayed) {
                    MarkerCanvas(
                        canvasParams,
                        horizontalScrollState,
                        keyboardState,
                        markerState,
                        editorState,
                        appState
                    )
                }
                if (appState.playerState.isPlaying) {
                    PlayerCursor(canvasParams, appState.playerState, horizontalScrollState)
                }
            }
        } else {
            Error(string(Strings.FailedToLoadSampleFileError))
        }
    }
}

@Composable
private fun Chunk(
    chunkIndex: Int,
    chunkCount: Int,
    canvasParams: CanvasParams,
    sample: Sample,
    appState: AppState,
    editorState: EditorState
) {
    Box(Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp / chunkCount)) {
        Column(Modifier.fillMaxSize()) {
            val weightOfEachChannel = 1f / sample.wave.channels.size
            sample.wave.channels.indices.forEach { channelIndex ->
                Box(
                    Modifier.weight(weightOfEachChannel)
                        .fillMaxWidth()
                ) {
                    val image = editorState.chartStore.getWaveform(channelIndex, chunkIndex)
                    if (image != null) {
                        WaveformChunk(image, channelIndex, chunkIndex)
                    }
                }
            }
            sample.spectrogram?.let {
                Box(
                    Modifier.weight(appState.appConf.painter.spectrogram.heightWeight)
                        .fillMaxWidth()
                ) {
                    val image = editorState.chartStore.getSpectrogram(chunkIndex)
                    if (image != null) {
                        SpectrogramChunk(image, chunkIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformChunk(
    image: State<ImageBitmap?>,
    channelIndex: Int,
    chunkIndex: Int
) {
    Log.info("Waveform (channel $channelIndex, chunk $chunkIndex): composed")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        image.value?.let {
            ChunkImage(it)
        }
    }
}

@Composable
private fun SpectrogramChunk(
    image: State<ImageBitmap?>,
    chunkIndex: Int
) {
    Log.info("Spectrogram (chunk $chunkIndex): composed")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        image.value?.let {
            ChunkImage(it)
        }
    }
}

@Composable
private fun ChunkImage(bitmap: ImageBitmap?) {
    bitmap?.let {
        Image(
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            bitmap = it,
            contentDescription = null
        )
    }
}

@Composable
private fun PlayerCursor(canvasParams: CanvasParams, playerState: PlayerState, scrollState: ScrollState) {
    val screenRange = scrollState.getScreenRange(canvasParams.lengthInPixel)
    Canvas(Modifier.fillMaxSize()) {
        val actualPosition = (playerState.framePosition / canvasParams.resolution).toFloat()
        if (screenRange != null && actualPosition in screenRange) {
            val position = actualPosition - screenRange.start
            drawLine(
                color = Yellow,
                start = Offset(position, 0f),
                end = Offset(position, center.y * 2),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun Error(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.error.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.widthIn(max = 600.dp),
            text = text,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground
        )
    }
}
