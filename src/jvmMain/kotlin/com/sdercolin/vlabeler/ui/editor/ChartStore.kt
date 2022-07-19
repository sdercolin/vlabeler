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
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.SampleRepository
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.splitAveragely
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import launchGcDelayed
import kotlin.math.absoluteValue

class ChartStore {

    @Immutable
    enum class ChartLoadingStatus {
        Loading,
        Loaded
    }

    private val waveformStatusList = mutableStateMapOf<Pair<Int, Int>, ChartLoadingStatus>()
    private val spectrogramStatusList = mutableStateMapOf<Int, ChartLoadingStatus>()

    private var job: Job? = null

    fun getWaveformStatus(channelIndex: Int, chunkIndex: Int) = waveformStatusList[channelIndex to chunkIndex]
    fun getSpectrogramStatus(chunkIndex: Int) = spectrogramStatusList[chunkIndex]

    fun clear() {
        Log.info("ChartStore clear()")
        job?.cancel()
        job = null
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
        ChartRepository.init(project, appConf, PaintingAlgorithmVersion)
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            val channelCount = sampleInfo.channels
            initializeStates(chunkCount, channelCount)
            val sample = if (needsToLoadSample(sampleInfo)) {
                SampleRepository.getSample(project.getSampleFile(sampleInfo.name), appConf)
            } else {
                null
            }
            val spectrogramDataChunks = sample?.spectrogram?.data?.toList()?.splitAveragely(chunkCount)

            val waveformChannelChunks = sample?.wave?.channels?.map { channel ->
                if (spectrogramDataChunks == null) {
                    channel.data.toList().splitAveragely(chunkCount)
                } else {
                    var taken = 0
                    val sizes = spectrogramDataChunks.map { it.size * sample.spectrogram.frameSize }
                    val data = channel.data.toList()
                    val chunks = mutableListOf<List<Float>>()
                    repeat(sizes.size) {
                        val chunk = data.subList(taken, taken + sizes[it])
                        taken += sizes[it]
                        chunks.add(chunk)
                    }
                    chunks.toList()
                }
            }

            val reorderedChunkIndexes = reorderChunks(startingChunkIndex, chunkCount)

            reorderedChunkIndexes.forEach { chunkIndex ->
                repeat(sampleInfo.channels) { channelIndex ->
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
                if (sampleInfo.hasSpectrogram && appConf.painter.spectrogram.enabled) {
                    drawSpectrogram(sampleInfo, spectrogramDataChunks, chunkIndex, density, layoutDirection)
                }
            }
            launchGcDelayed()
        }
    }

    private fun initializeStates(
        chunkCount: Int,
        channelCount: Int
    ) {
        repeat(chunkCount) { chunkIndex ->
            repeat(channelCount) { channelIndex ->
                waveformStatusList[channelIndex to chunkIndex] = ChartLoadingStatus.Loading
            }
            spectrogramStatusList[chunkIndex] = ChartLoadingStatus.Loading
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

    private fun needsToLoadSample(sampleInfo: SampleInfo): Boolean {
        repeat(sampleInfo.channels) { channelIndex ->
            repeat(sampleInfo.chunkCount) { chunkIndex ->
                if (!hasCachedWaveform(sampleInfo, channelIndex, chunkIndex)) return true
            }
        }

        if (sampleInfo.hasSpectrogram) {
            repeat(sampleInfo.chunkCount) { chunkIndex ->
                if (!hasCachedSpectrogram(sampleInfo, chunkIndex)) return true
            }
        }

        return false
    }

    private fun hasCachedWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int
    ): Boolean {
        val targetFile = ChartRepository.getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        if (targetFile.exists()) {
            if (targetFile.lastModified() > sampleInfo.lastModified) {
                return true
            }
        }
        return false
    }

    private fun deleteCachedWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int
    ) {
        val targetFile = ChartRepository.getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    private suspend fun drawWaveform(
        sampleInfo: SampleInfo,
        waveformChannelChunks: List<List<List<Float>>>?,
        channelIndex: Int,
        chunkIndex: Int,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        if (hasCachedWaveform(sampleInfo, channelIndex, chunkIndex)) {
            waveformStatusList[channelIndex to chunkIndex] = ChartLoadingStatus.Loaded
            return
        } else {
            deleteCachedWaveform(sampleInfo, channelIndex, chunkIndex)
        }
        requireNotNull(waveformChannelChunks) {
            "waveformChannelChunks[$channelIndex][$chunkIndex] is required. However it's not loaded."
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
        waveformStatusList[channelIndex to chunkIndex] = ChartLoadingStatus.Loaded
    }

    private fun hasCachedSpectrogram(
        sampleInfo: SampleInfo,
        chunkIndex: Int
    ): Boolean {
        val targetFile = ChartRepository.getSpectrogramImageFile(sampleInfo, chunkIndex)
        if (targetFile.exists()) {
            if (targetFile.lastModified() > sampleInfo.lastModified) {
                return true
            }
        }
        return false
    }

    private fun deleteCachedSpectrogram(
        sampleInfo: SampleInfo,
        chunkIndex: Int
    ) {
        val targetFile = ChartRepository.getSpectrogramImageFile(sampleInfo, chunkIndex)
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    private suspend fun drawSpectrogram(
        sampleInfo: SampleInfo,
        spectrogramDataChunks: List<List<DoubleArray>>?,
        chunkIndex: Int,
        density: Density,
        layoutDirection: LayoutDirection
    ) {
        if (hasCachedSpectrogram(sampleInfo, chunkIndex)) {
            spectrogramStatusList[chunkIndex] = ChartLoadingStatus.Loaded
            return
        } else {
            deleteCachedSpectrogram(sampleInfo, chunkIndex)
        }
        requireNotNull(spectrogramDataChunks) {
            "spectrogramDataChunks[$chunkIndex] is required. However it's not loaded."
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
        spectrogramStatusList[chunkIndex] = ChartLoadingStatus.Loaded
    }

    companion object {
        private const val PaintingAlgorithmVersion = 1
    }
}
