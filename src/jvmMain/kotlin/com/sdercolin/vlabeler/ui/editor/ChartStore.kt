package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
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
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.SampleRepository
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import launchGcDelayed
import kotlin.math.absoluteValue

class ChartStore {

    @Immutable
    enum class BitmapLoadingStatus {
        Loading,
        Loaded
    }

    private val waveformStatusList = mutableStateMapOf<Pair<Int, Int>, BitmapLoadingStatus>()
    private val spectrogramStatusList = mutableStateMapOf<Int, BitmapLoadingStatus>()

    private var job: Job? = null

    fun getWaveformStatus(channelIndex: Int, chunkIndex: Int) = waveformStatusList[channelIndex to chunkIndex]
    fun getSpectrogramStatus(chunkIndex: Int) = spectrogramStatusList[chunkIndex]

    fun clear() {
        Log.info("ChartStore clear()")
        job?.cancel()
        job = null
        ChartRepository.clear()
        waveformStatusList.clear()
        spectrogramStatusList.clear()
        launchGcDelayed()
    }

    fun load(
        scope: CoroutineScope,
        project: Project,
        chunkCount: Int,
        sampleInfo: SampleInfo,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
        startingChunkIndex: Int
    ) {
        Log.info("ChartStore load(${sampleInfo.name})")
        ChartRepository.initCacheDirectory(project)
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            val sample = SampleRepository.retrieve(sampleInfo.name)
            val channels = sample.wave.channels
            initializeStates(chunkCount, channels)

            val waveformChannelChunks = channels.map {
                val size = channels.first().data.size / chunkCount
                it.data.toList().chunked(size)
            }
            val spectrogramDataChunks = sample.spectrogram?.let {
                val size = it.data.size / chunkCount
                it.data.toList().chunked(size)
            }

            val reorderedChunkIndexes = reorderChunks(startingChunkIndex, chunkCount)

            reorderedChunkIndexes.forEach { chunkIndex ->
                channels.indices.map { channelIndex ->
                    drawWaveform(
                        sampleInfo,
                        waveformChannelChunks,
                        channelIndex,
                        chunkIndex,
                        appConf,
                        density,
                        layoutDirection
                    )
                }
                if (spectrogramDataChunks != null) {
                    drawSpectrogram(sampleInfo, spectrogramDataChunks, chunkIndex, density, layoutDirection)
                }
            }
            launchGcDelayed()
        }
    }

    private fun initializeStates(
        chunkCount: Int,
        channels: List<Wave.Channel>
    ) {
        repeat(chunkCount) { chunkIndex ->
            repeat(channels.size) { channelIndex ->
                waveformStatusList[channelIndex to chunkIndex] = BitmapLoadingStatus.Loading
            }
            spectrogramStatusList[chunkIndex] = BitmapLoadingStatus.Loading
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
        sampleInfo: SampleInfo,
        waveformChannelChunks: List<List<List<Float>>>,
        channelIndex: Int,
        chunkIndex: Int,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val targetFile = ChartRepository.getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        if (targetFile.exists()) {
            if (targetFile.lastModified() > sampleInfo.lastModified) {
                waveformStatusList[channelIndex to chunkIndex] = BitmapLoadingStatus.Loaded
                return
            } else {
                targetFile.delete()
            }
        }
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
        ChartRepository.putWaveform(sampleInfo, channelIndex, chunkIndex, newBitmap)
        yield()
        waveformStatusList[channelIndex to chunkIndex] = BitmapLoadingStatus.Loaded
    }

    private suspend fun drawSpectrogram(
        sampleInfo: SampleInfo,
        spectrogramDataChunks: List<List<DoubleArray>>,
        chunkIndex: Int,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        val targetFile = ChartRepository.getSpectrogramImageFile(sampleInfo, chunkIndex)
        if (targetFile.exists()) {
            if (targetFile.lastModified() > sampleInfo.lastModified) {
                spectrogramStatusList[chunkIndex] = BitmapLoadingStatus.Loaded
                return
            } else {
                targetFile.delete()
            }
        }
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
        ChartRepository.putSpectrogram(sampleInfo, chunkIndex, newBitmap)
        yield()
        spectrogramStatusList[chunkIndex] = BitmapLoadingStatus.Loaded
    }
}
