package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleChunk
import com.sdercolin.vlabeler.model.SampleInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.sound.sampled.AudioSystem

/**
 * Data class to represent a wave.
 *
 * @property channels A list of channels. Each channel is a list of amplitude data.
 */
@Immutable
class Wave(val channels: List<Channel>) {
    @Stable
    class Channel(val data: FloatArray)

    val length get() = channels[0].data.size
}

/**
 * Load a sample chunk from a sample file.
 *
 * @param project The project.
 * @param sampleInfo The sample info.
 * @param appConf The app configuration.
 * @param chunkIndex The index of the chunk to load.
 * @param chunkSize The number of frames in each chunk. The last chunk may have fewer frames, but it doesn't matter
 *     because the chunkSize is used for calculating the offset of the current chunk.
 */
suspend fun loadSampleChunk(
    project: Project,
    sampleInfo: SampleInfo,
    appConf: AppConf,
    chunkIndex: Int,
    chunkSize: Int,
): Result<SampleChunk> = withContext(Dispatchers.IO) {
    val stream = with(AudioSystem.getAudioInputStream(sampleInfo.getFile(project))) {
        val format = format.normalize(appConf.painter.amplitude.resampleDownToHz)
        AudioSystem.getAudioInputStream(format, this)
    }
    runCatching {
        val offset = chunkIndex * chunkSize.toLong()
        val sampleByteSize = stream.format.sampleSizeInBits / 8
        val channelCount = sampleInfo.channels
        val frameSize = sampleByteSize * channelCount
        val isBigEndian = stream.format.isBigEndian
        val channels = List(channelCount) { mutableListOf<Float>() }
        val buffer = ByteArray(chunkSize * frameSize)
        stream.skip(offset * frameSize)
        Log.debug("Loading chunk $chunkIndex: offset=$offset")
        val readSize = stream.readNBytes(buffer, 0, chunkSize * frameSize)
        val readFrameCount = readSize / frameSize
        for (frameIndex in 0 until readFrameCount) {
            val frameStart = frameIndex * frameSize
            val frameEnd = frameStart + frameSize
            val frame = buffer.sliceArray(frameStart until frameEnd)
            for (channelIndex in channels.indices) {
                val sample = getSampleValueFromFrame(
                    frame = frame,
                    frameSize = frameSize,
                    channelIndex = channelIndex,
                    channelCount = channelCount,
                    isBigEndian = isBigEndian,
                )
                val normalizedSample = if (sampleInfo.normalizeRatio != null) {
                    sample * sampleInfo.normalizeRatio
                } else {
                    sample
                }
                channels[channelIndex].add(normalizedSample)
            }
        }
        val wave = Wave(channels = channels.map { Wave.Channel(it.toFloatArray()) })
        val spectrogram = if (appConf.painter.spectrogram.enabled) {
            wave.toSpectrogram(appConf.painter.spectrogram, sampleInfo.sampleRate)
        } else null
        val power = if (appConf.painter.power.enabled) {
            wave.toPower(appConf.painter.power)
        } else null
        val fundamental = if (appConf.painter.fundamental.enabled) {
            wave.toFundamental(appConf.painter.fundamental, sampleInfo.sampleRate)
        } else null
        SampleChunk(
            info = sampleInfo,
            index = chunkIndex,
            wave = wave,
            spectrogram = spectrogram,
            power = power,
            fundamental = fundamental,
        )
    }.onFailure {
        if (it is CancellationException) {
            Log.info("Cancelled loading chunk $chunkIndex")
        } else {
            Log.error("Error loading chunk $chunkIndex")
            Log.error(it)
        }
        stream.close()
    }.onSuccess {
        stream.close()
    }
}

/**
 * Get the amplitude value of a sample from the given frame. By "sample", we mean the value of a single channel in a
 * frame.
 *
 * @param frame The data of the frame.
 * @param frameSize The size of a frame in bytes. This should be equal to [frame].size.
 * @param channelIndex The index of the channel.
 * @param channelCount The number of channels.
 * @param isBigEndian Whether the data is big endian.
 */
fun getSampleValueFromFrame(
    frame: ByteArray,
    frameSize: Int,
    channelIndex: Int,
    channelCount: Int,
    isBigEndian: Boolean,
): Float {
    val sampleSize = frameSize / channelCount
    val channelBytes = frame.slice(
        channelIndex * sampleSize until (channelIndex + 1) * sampleSize,
    ).let { if (isBigEndian) it.reversed() else it.toList() }

    return channelBytes.mapIndexed { index, byte ->
        val uByte = if (index == channelBytes.lastIndex) byte.toInt()
        else (byte.toInt() and 0xFF)
        uByte shl (8 * index)
    }
        .sum().toFloat()
}

const val WAVE_LOADING_ALGORITHM_VERSION = 4
