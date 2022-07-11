package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.absoluteValue

class ChartStore {

    private val waveformChartsList = mutableStateMapOf<Pair<Int, Int>, MutableState<ImageBitmap?>>()
    private val spectrogramCharts = mutableStateMapOf<Int, MutableState<ImageBitmap?>>()

    private var job: Job? = null

    private fun clear() {
        waveformChartsList.clear()
        spectrogramCharts.clear()
    }

    fun getWaveform(channelIndex: Int, chunkIndex: Int) = waveformChartsList[channelIndex to chunkIndex]
    fun getSpectrogram(chunkIndex: Int) = spectrogramCharts[chunkIndex]

    fun load(
        scope: CoroutineScope,
        chunkCount: Int,
        sample: Sample,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
        startingChunkIndex: Int
    ) {
        job?.cancel()
        clear()
        val channels = sample.wave.channels
        job = scope.launch(Dispatchers.IO) {

            initializeStates(chunkCount, channels)

            val waveformChannelChunks = channels.map {
                val size = channels.first().data.size / chunkCount
                it.data.chunked(size)
            }
            val spectrogramDataChunks = sample.spectrogram?.let {
                val size = it.data.size / chunkCount
                it.data.toList().chunked(size)
            }

            val reorderedChunkIndexes = reorderChunks(startingChunkIndex, chunkCount)

            reorderedChunkIndexes.forEach { chunkIndex ->
                channels.indices.map { channelIndex ->
                    drawWaveform(waveformChannelChunks, channelIndex, chunkIndex, appConf, density, layoutDirection)
                }
                if (spectrogramDataChunks != null) {
                    drawSpectrogram(spectrogramDataChunks, chunkIndex, density, layoutDirection)
                }
            }
        }
    }

    private fun initializeStates(
        chunkCount: Int,
        channels: List<Wave.Channel>
    ) {
        repeat(chunkCount) { chunkIndex ->
            repeat(channels.size) { channelIndex ->
                waveformChartsList[channelIndex to chunkIndex] = mutableStateOf(null)
            }
            spectrogramCharts[chunkIndex] = mutableStateOf(null)
        }
    }

    private fun reorderChunks(
        startingChunkIndex: Int,
        chunkCount: Int
    ): MutableList<Int> {
        val reorderedChunkIndexes = mutableListOf(startingChunkIndex)
        var radius = 1
        while (reorderedChunkIndexes.size < chunkCount) {
            listOf(startingChunkIndex - radius, startingChunkIndex + radius)
                .filter { it in 0 until chunkCount }
                .forEach { reorderedChunkIndexes.add(it) }
            radius++
        }
        return reorderedChunkIndexes
    }

    private suspend fun drawWaveform(
        waveformChannelChunks: List<List<List<Float>>>,
        channelIndex: Int,
        chunkIndex: Int,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val data = waveformChannelChunks[channelIndex][chunkIndex]
        val dataDensity = appConf.painter.amplitude.unitSize
        val width = data.size / dataDensity
        val maxRawY = data.maxOfOrNull { it.absoluteValue } ?: 0f
        val height = appConf.painter.amplitude.intensityAccuracy
        val size = Size(width.toFloat(), height.toFloat())
        val newBitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
            Log.info("Waveforms chunk $chunkIndex in channel $channelIndex: draw bitmap")
            val yScale = maxRawY / height * 2 * (1 + appConf.painter.amplitude.yAxisBlankRate)
            val points = data
                .map { height / 2 - it / yScale }
                .withIndex().map { Offset(it.index.toFloat() / dataDensity, it.value) }
            drawPoints(points, pointMode = PointMode.Polygon, color = LightGray)
        }
        yield()
        waveformChartsList[channelIndex to chunkIndex]?.value = newBitmap
    }

    private suspend fun drawSpectrogram(
        spectrogramDataChunks: List<List<DoubleArray>>,
        chunkIndex: Int,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val chunk = spectrogramDataChunks[chunkIndex]
        val width = chunk.size.toFloat()
        val height = chunk.first().size.toFloat()
        val size = Size(width, height)
        val newBitmap = ImageBitmap(width.toInt(), height.toInt())
        Log.info("Spectrogram chunk $chunkIndex: draw bitmap")
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
        yield()
        spectrogramCharts[chunkIndex]?.value = newBitmap
    }
}