package com.sdercolin.vlabeler.repository

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isWindows
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.io.toSpectrogram
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.model.SampleInfo
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

object SampleRepository {

    private val map = mutableMapOf<String, Sample>()

    fun load(file: File, appConf: AppConf): Result<SampleInfo> {
        val sample = loadSampleFile(file, appConf).getOrElse {
            return Result.failure(it)
        }
        map[sample.info.name] = sample
        return Result.success(sample.info)
    }

    fun retrieve(name: String): Sample = map.getValue(name).also { map.remove(name) }

    fun clear() = map.clear()

    private fun loadSampleFile(file: File, appConf: AppConf): Result<Sample> = runCatching {
        val stream = AudioSystem.getAudioInputStream(file)
        val format = stream.format
        Log.debug("Sample file loaded: $format")
        val channelNumber = format.channels
        val isBigEndian = format.isBigEndian
        val channels = (0 until channelNumber).map { mutableListOf<Float>() }
        val frameByteSize = format.sampleSizeInBits / 8
        if (frameByteSize > 4 || (isWindows && frameByteSize != 2)) {
            throw Exception("Unsupported sampleSizeInBits: ${format.sampleSizeInBits} bit")
        }
        val sampleSize = channelNumber * frameByteSize
        val isFloat = when (format.encoding) {
            AudioFormat.Encoding.PCM_SIGNED -> false
            AudioFormat.Encoding.PCM_FLOAT -> true
            else -> throw Exception("Unsupported audio encoding")
        }

        while (true) {
            val sampleBytes = stream.readNBytes(sampleSize)
            if (sampleBytes.isEmpty()) break
            if (sampleBytes.size != sampleSize) {
                Log.error("Ignored last ${sampleBytes.size} bytes.")
                break
            }
            for (channelIndex in channels.indices) {
                val channel = channels[channelIndex]
                val channelBytes = sampleBytes.slice(
                    channelIndex * frameByteSize until (channelIndex + 1) * frameByteSize
                ).let { if (isBigEndian) it.reversed() else it.toList() }

                val sample = channelBytes.mapIndexed { index, byte ->
                    val uByte = if (index == channelBytes.lastIndex) byte.toInt()
                    else (byte.toInt() and 0xFF)
                    uByte shl (8 * index)
                }
                    .sum()
                    .let { if (isFloat) Float.fromBits(it) else it.toFloat() }
                channel.add(sample)
            }
        }
        stream.close()
        val wave = Wave(channels = channels.map { Wave.Channel(it.toFloatArray()) })
        val spectrogram = if (appConf.painter.spectrogram.enabled) {
            wave.toSpectrogram(appConf.painter.spectrogram, format.sampleRate)
        } else null
        val info = SampleInfo(
            name = file.nameWithoutExtension,
            file = file,
            sampleRate = format.sampleRate,
            bitDepth = frameByteSize,
            isFloat = isFloat,
            channels = channels.size,
            length = wave.length,
            lengthMillis = channels[0].size.toFloat() / format.sampleRate * 1000,
            hasSpectrogram = spectrogram != null
        )
        return Result.success(
            Sample(
                info = info,
                wave = wave,
                spectrogram = spectrogram
            )
        )
    }
}
