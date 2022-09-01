package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.AsyncImage
import com.sdercolin.vlabeler.ui.editor.ChartStore
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCanvas
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerLabels
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerPointEventContainer
import com.sdercolin.vlabeler.ui.editor.labeler.marker.rememberMarkerState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.util.getScreenRange
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toColorOrNull
import kotlinx.coroutines.CancellationException

@Composable
fun Canvas(
    horizontalScrollState: ScrollState,
    editorState: EditorState,
    appState: AppState,
) {
    val currentDensity = LocalDensity.current
    val sampleInfoResult = editorState.sampleInfoResult
    val resolution = editorState.canvasResolution

    if (sampleInfoResult != null) {
        val sampleInfo = sampleInfoResult.getOrNull()
        if (sampleInfo != null) {
            val chunkCount = sampleInfo.chunkCount
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            LaunchedEffect(sampleInfo, appState.appConf) {
                editorState.renderCharts(this, chunkCount, sampleInfo, appState.appConf, density, layoutDirection)
            }
            val canvasParams = CanvasParams(sampleInfo.length, resolution, currentDensity)
            editorState.scrollOnResolutionChangeViewModel.updateCanvasParams(canvasParams, sampleInfo)
            val markerState = rememberMarkerState(sampleInfo, canvasParams, editorState, appState)
            val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()
            val screenRange = horizontalScrollState.getScreenRange(markerState.canvasParams.lengthInPixel)

            MarkerPointEventContainer(
                screenRange,
                keyboardState,
                horizontalScrollState,
                markerState,
                editorState,
                appState,
            ) {
                Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
                    Row {
                        repeat(chunkCount) { chunkIndex ->
                            Chunk(
                                chunkIndex,
                                chunkCount,
                                canvasParams,
                                sampleInfo,
                                appState,
                                editorState,
                            )
                        }
                    }
                    if (appState.isMarkerDisplayed) {
                        // TODO: merge into marker canvas because it should displayed over the markers
                        MarkerLabels(screenRange, appState, markerState)
                    }
                }
                if (appState.isMarkerDisplayed) {
                    MarkerCanvas(
                        canvasParams,
                        horizontalScrollState,
                        markerState,
                        editorState,
                        appState,
                    )
                }
                if (appState.playerState.isPlaying) {
                    PlayerCursor(
                        canvasParams,
                        appState.playerState,
                        horizontalScrollState,
                        appState.appConf.editor.playerCursorColor.toColor(),
                    )
                }
            }
        } else {
            val exception = sampleInfoResult.exceptionOrNull()
            if (exception != null && exception !is CancellationException) {
                Error(string(Strings.FailedToLoadSampleFileError))
            }
        }
    }
}

@Composable
private fun Chunk(
    chunkIndex: Int,
    chunkCount: Int,
    canvasParams: CanvasParams,
    sampleInfo: SampleInfo,
    appState: AppState,
    editorState: EditorState,
) {
    Box(
        Modifier.fillMaxHeight()
            .requiredWidth(canvasParams.canvasWidthInDp / chunkCount)
            .runIf(DebugState.isShowingChunkBorder) { border(1.dp, DarkYellow) },
    ) {
        Column(Modifier.fillMaxSize()) {
            val weightOfEachChannel = 1f / sampleInfo.channels
            val backgroundColor = appState.appConf.painter.amplitude.backgroundColor.toColorOrNull()
                ?: AppConf.Amplitude.DefaultBackgroundColor.toColor()
            repeat(sampleInfo.channels) { channelIndex ->
                Box(Modifier.weight(weightOfEachChannel).fillMaxWidth()) {
                    val imageStatus = editorState.chartStore.getWaveformStatus(channelIndex, chunkIndex)
                    if (imageStatus == ChartStore.ChartLoadingStatus.Loaded) {
                        WaveformChunk(sampleInfo, channelIndex, chunkIndex, backgroundColor)
                    }
                }
            }
            if (sampleInfo.hasSpectrogram && appState.appConf.painter.spectrogram.enabled) {
                Box(
                    Modifier.weight(appState.appConf.painter.spectrogram.heightWeight)
                        .fillMaxWidth(),
                ) {
                    val imageStatus = editorState.chartStore.getSpectrogramStatus(chunkIndex)
                    if (imageStatus == ChartStore.ChartLoadingStatus.Loaded) {
                        SpectrogramChunk(sampleInfo, chunkIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformChunk(sampleInfo: SampleInfo, channelIndex: Int, chunkIndex: Int, backgroundColor: Color) {
    Log.info("Waveform (channel $channelIndex, chunk $chunkIndex): composed")
    Box(Modifier.fillMaxSize().background(backgroundColor)) {
        ChunkAsyncImage(
            load = { ChartRepository.getWaveform(sampleInfo, channelIndex, chunkIndex) },
            sampleInfo,
            channelIndex,
            chunkIndex,
        )
    }
}

@Composable
private fun SpectrogramChunk(sampleInfo: SampleInfo, chunkIndex: Int) {
    Log.info("Spectrogram (chunk $chunkIndex): composed")
    println(sampleInfo.toString())
    Box(Modifier.fillMaxSize()) {
        ChunkAsyncImage(
            load = { ChartRepository.getSpectrogram(sampleInfo, chunkIndex) },
            sampleInfo,
            chunkIndex,
        )
    }
}

@Composable
private fun ChunkAsyncImage(load: suspend () -> ImageBitmap, vararg keys: Any) {
    AsyncImage(
        load = load,
        painterFor = { remember { BitmapPainter(it) } },
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        keys = keys,
    )
}

@Composable
private fun PlayerCursor(
    canvasParams: CanvasParams,
    playerState: PlayerState,
    scrollState: ScrollState,
    color: Color,
) {
    val screenRange = scrollState.getScreenRange(canvasParams.lengthInPixel)
    Canvas(Modifier.fillMaxSize()) {
        val actualPosition = playerState.framePosition / canvasParams.resolution
        if (screenRange != null && actualPosition in screenRange) {
            val position = actualPosition - screenRange.start
            drawLine(
                color = color,
                start = Offset(position, 0f),
                end = Offset(position, center.y * 2),
                strokeWidth = 2f,
            )
        }
    }
}

@Composable
private fun Error(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.error.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.widthIn(max = 600.dp),
            text = text,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
        )
    }
}
