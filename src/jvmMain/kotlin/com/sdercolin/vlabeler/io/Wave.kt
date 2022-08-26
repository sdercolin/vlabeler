package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.SampleChunk
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.sound.sampled.AudioSystem
import kotlin.math.pow

@Immutable
class Wave(val channels: List<Channel>) {
    @Stable
    class Channel(val data: FloatArray)

    val length get() = channels[0].data.size
}

suspend fun loadSampleChunk(
    sampleInfo: SampleInfo,
    appConf: AppConf,
    chunkIndex: Int,
    chunkSize: Int,
): Result<SampleChunk> = withContext(Dispatchers.IO) {
    val stream = AudioSystem.getAudioInputStream(sampleInfo.file.toFile())
    runCatching {
        val offset = chunkIndex * chunkSize.toLong()
        val sampleByteSize = sampleInfo.bitDepth / 8
        val channelCount = sampleInfo.channels
        val frameSize = sampleByteSize * channelCount
        val isBigEndian = stream.format.isBigEndian
        val channels = List(channelCount) { mutableListOf<Float>() }
        var readFrameCount = 0
        var pos = offset
        val buffer = ByteArray(frameSize)
        stream.skipNBytes(offset * frameSize)
        Log.debug("Loading chunk $chunkIndex: offset=$offset")
        while (readFrameCount < chunkSize) {
            yield()
            val readSize = stream.readNBytes(buffer, 0, frameSize)
            if (readSize == 0) break
            for (channelIndex in channels.indices) {
                val sampleSize = frameSize / channelCount
                val channel = channels[channelIndex]
                val channelBytes = buffer.slice(
                    channelIndex * sampleSize until (channelIndex + 1) * sampleSize,
                ).let { if (isBigEndian) it.reversed() else it.toList() }

                val sample = channelBytes.mapIndexed { index, byte ->
                    val uByte = if (index == channelBytes.lastIndex) byte.toInt()
                    else (byte.toInt() and 0xFF)
                    uByte shl (8 * index)
                }
                    .sum()
                    .let {
                        if (sampleInfo.isFloat) {
                            Float.fromBits(it)
                        } else {
                            // decrease bit depth to 16 bit
                            (it.toDouble() / 2.0.pow(sampleInfo.bitDepth - 1) * Short.MAX_VALUE).toFloat()
                        }
                    }

                channel.add(sample)
            }
            pos += frameSize
            readFrameCount++
        }
        val wave = Wave(channels = channels.map { Wave.Channel(it.toFloatArray()) })
        val spectrogram = if (appConf.painter.spectrogram.enabled) {
            wave.toSpectrogram(appConf.painter.spectrogram, sampleInfo.sampleRate)
        } else null
        SampleChunk(
            info = sampleInfo,
            index = chunkIndex,
            wave = wave,
            spectrogram = spectrogram,
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

const val WaveLoadingAlgorithmVersion = 4
