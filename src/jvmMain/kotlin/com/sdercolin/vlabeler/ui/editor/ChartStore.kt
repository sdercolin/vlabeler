package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.MelScale
import com.sdercolin.vlabeler.io.NormalizedSampleSizeInBits
import com.sdercolin.vlabeler.io.loadSampleChunk
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleChunk
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.util.launchGcDelayed
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toColorOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.pow

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

    fun prepareForNewLoading(project: Project, appConf: AppConf, chunkCount: Int, channelCount: Int) {
        job?.cancel()
        initializeStates(chunkCount, channelCount)
        ChartRepository.init(project, appConf, PaintingAlgorithmVersion)
    }

    fun load(
        scope: CoroutineScope,
        sampleInfo: SampleInfo,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
        startingChunkIndex: Int,
        onRenderProgress: suspend () -> Unit,
    ) {
        Log.info("ChartStore load(${sampleInfo.name})")
        job = scope.launch(Dispatchers.IO) {
            val reorderedChunkIndexes = reorderChunks(startingChunkIndex, sampleInfo.chunkCount)
            val colorPalette = appConf.painter.spectrogram.colorPalette.create()
            reorderedChunkIndexes.forEach { chunkIndex ->
                yield()

                val chunk = if (hasCachedChunk(sampleInfo, chunkIndex)) {
                    null
                } else {
                    loadSampleChunk(sampleInfo, appConf, chunkIndex, sampleInfo.chunkSize).getOrThrow()
                }

                repeat(sampleInfo.channels) { channelIndex ->
                    System.gc()
                    renderWaveform(
                        sampleInfo,
                        chunk,
                        channelIndex,
                        chunkIndex,
                        appConf,
                        density,
                        layoutDirection,
                        onRenderProgress,
                    )
                }
                if (sampleInfo.hasSpectrogram && appConf.painter.spectrogram.enabled) {
                    System.gc()
                    renderSpectrogram(
                        sampleInfo,
                        chunk,
                        chunkIndex,
                        density,
                        appConf,
                        layoutDirection,
                        colorPalette,
                        onRenderProgress,
                    )
                }
            }
            launchGcDelayed()
        }
    }

    suspend fun awaitLoad() = job?.join()

    private fun initializeStates(
        chunkCount: Int,
        channelCount: Int,
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
        chunkCount: Int,
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

    fun hasCachedSample(sampleInfo: SampleInfo): Boolean {
        repeat(sampleInfo.chunkCount) {
            if (!hasCachedChunk(sampleInfo, it)) {
                return false
            }
        }
        return true
    }

    private fun hasCachedChunk(sampleInfo: SampleInfo, chunkIndex: Int): Boolean {
        repeat(sampleInfo.channels) { channelIndex ->
            if (!hasCachedWaveform(sampleInfo, channelIndex, chunkIndex)) return false
        }

        if (sampleInfo.hasSpectrogram) {
            if (!hasCachedSpectrogram(sampleInfo, chunkIndex)) return false
        }

        return true
    }

    private fun hasCachedWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
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
        chunkIndex: Int,
    ) {
        val targetFile = ChartRepository.getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    private suspend fun renderWaveform(
        sampleInfo: SampleInfo,
        chunk: SampleChunk?,
        channelIndex: Int,
        chunkIndex: Int,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
        onRenderProgress: suspend () -> Unit,
    ) {
        if (hasCachedWaveform(sampleInfo, channelIndex, chunkIndex)) {
            waveformStatusList[channelIndex to chunkIndex] = ChartLoadingStatus.Loaded
            onRenderProgress()
            return
        } else {
            deleteCachedWaveform(sampleInfo, channelIndex, chunkIndex)
        }
        requireNotNull(chunk) {
            "Chunk $chunkIndex is required. However it's not loaded."
        }
        val data = chunk.wave.channels[channelIndex].data
        val dataDensity = appConf.painter.amplitude.unitSize
        val width = data.size / dataDensity
        val maxRawY = 2.0.pow(NormalizedSampleSizeInBits - 1).toFloat()
        val height = appConf.painter.amplitude.intensityAccuracy
        val size = Size(width.toFloat(), height.toFloat())
        val newBitmap = ImageBitmap(width, height)
        val color = appConf.painter.amplitude.color.toColorOrNull() ?: AppConf.Amplitude.DefaultColor.toColor()
        CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
            Log.info("Waveforms chunk $chunkIndex in channel $channelIndex: draw bitmap")
            val yScale = maxRawY / height * 2 * (1 + appConf.painter.amplitude.yAxisBlankRate)
            data.toList()
                .map { height / 2 - it / yScale }
                .chunked(dataDensity).forEachIndexed { index, dataChunk ->
                    val max = dataChunk.maxOrNull() ?: 0f
                    val min = dataChunk.minOrNull() ?: 0f
                    drawRect(
                        color = color,
                        topLeft = Offset(index.toFloat(), min),
                        size = Size(1f, max - min),
                    )
                }
        }
        yield()
        ChartRepository.putWaveform(sampleInfo, channelIndex, chunkIndex, newBitmap)
        yield()
        waveformStatusList[channelIndex to chunkIndex] = ChartLoadingStatus.Loaded
        onRenderProgress()
    }

    private fun hasCachedSpectrogram(
        sampleInfo: SampleInfo,
        chunkIndex: Int,
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
        chunkIndex: Int,
    ) {
        val targetFile = ChartRepository.getSpectrogramImageFile(sampleInfo, chunkIndex)
        if (targetFile.exists()) {
            targetFile.delete()
        }
    }

    private suspend fun renderSpectrogram(
        sampleInfo: SampleInfo,
        chunk: SampleChunk?,
        chunkIndex: Int,
        density: Density,
        appConf: AppConf,
        layoutDirection: LayoutDirection,
        colorPalette: SpectrogramColorPalette,
        onRenderProgress: suspend () -> Unit,
    ) {
        if (hasCachedSpectrogram(sampleInfo, chunkIndex)) {
            spectrogramStatusList[chunkIndex] = ChartLoadingStatus.Loaded
            onRenderProgress()
            return
        } else {
            deleteCachedSpectrogram(sampleInfo, chunkIndex)
        }
        requireNotNull(chunk) {
            "Chunk $chunkIndex is required. However it's not loaded."
        }
        val rawData = requireNotNull(chunk.spectrogram).data
        val pointDensity = appConf.painter.spectrogram.pointDensity
        val data = rawData.chunked(pointDensity).map { group ->
            val result = DoubleArray(group.first().size) { 0.0 }
            for (point in group) {
                for (i in point.indices) {
                    result[i] += point[i]
                }
            }
            result.map { it / group.size }
        }
        val maxFrequency = sampleInfo.sampleRate / 2
        val maxFrequencyToDisplay = appConf.painter.spectrogram.maxFrequency
        val maxMel = MelScale.toMel(maxFrequencyToDisplay.toDouble()).toInt()
        val width = data.size.toFloat()
        val height = maxMel.toFloat()
        val size = Size(width, height)
        val newBitmap = ImageBitmap(width.toInt(), height.toInt())
        Log.info("Spectrogram chunk $chunkIndex: draw bitmap")
        CanvasDrawScope().draw(density, layoutDirection, Canvas(newBitmap), size) {
            data.forEachIndexed { xIndex, yArray ->
                if (yArray.isEmpty()) return@forEachIndexed
                val frequencyList = yArray.indices.map { it.toFloat() * maxFrequency / (yArray.size - 1) }

                val step = appConf.painter.spectrogram.melScaleStep
                val mels = frequencyList.map { MelScale.toMel(it.toDouble()) }
                    .let { mels ->
                        val firstOverIndex = mels.indexOfFirst { it > maxMel }
                        if (firstOverIndex >= 0) {
                            mels.subList(0, firstOverIndex)
                        } else {
                            val lastMel = mels.last()
                            if (lastMel + step < maxMel) {
                                mels + listOf(lastMel + step, maxMel.toDouble())
                            } else {
                                mels + listOf(maxMel.toDouble())
                            }
                        }
                    }
                val interpolated = mels.foldIndexed(listOf<Pair<Int, Double>>()) { index, acc, keyMel ->
                    if (acc.isEmpty()) {
                        listOf(keyMel.toInt() to yArray.getOrElse(index) { 0.0 })
                    } else {
                        val thisIntensity = yArray.getOrElse(index) { 0.0 }
                        val (lastMel, lastIntensity) = acc.last()
                        acc + (lastMel + step..keyMel.toInt() step step).map { mel ->
                            val intensity = lastIntensity +
                                (thisIntensity - lastIntensity) * (mel - lastMel) / (keyMel - lastMel)
                            mel to intensity
                        }
                    }
                }
                interpolated.forEach { (mel, intensity) ->
                    val color = colorPalette.get(intensity.toFloat())
                    val left = xIndex.toFloat()
                    val top = height - mel
                    drawRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(1f, step.toFloat()),
                    )
                }
            }
        }
        yield()
        ChartRepository.putSpectrogram(sampleInfo, chunkIndex, newBitmap)
        yield()
        spectrogramStatusList[chunkIndex] = ChartLoadingStatus.Loaded
        onRenderProgress()
    }

    companion object {
        private const val PaintingAlgorithmVersion = 5
    }
}
