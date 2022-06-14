package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.process.fft.toSpectrogram
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class Wave(val channels: List<Channel>) {
    class Channel(val data: List<Float>)

    val length get() = channels[0].data.size
}

fun loadSampleFile(file: File, appConf: AppConf): Sample {
    val stream = AudioSystem.getAudioInputStream(file)
    val format = stream.format
    println("Loaded wav file: $format")
    val channelNumber = format.channels
    val isBigEndian = format.isBigEndian
    val channels = (0 until channelNumber).map { mutableListOf<Float>() }
    val frameByteSize = format.sampleSizeInBits / 8
    if (frameByteSize > 4) {
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
            println("Warning: Ignored last ${sampleBytes.size} bytes.")
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
    val wave = Wave(channels = channels.map { Wave.Channel(it) })
    val spectrogram = if (appConf.painter.spectrogram.enabled) {
        wave.channels.first().toSpectrogram(appConf.painter.spectrogram, format.sampleRate)
    } else null
    return Sample(
        info = SampleInfo(
            name = file.nameWithoutExtension,
            file = file,
            sampleRate = format.sampleRate,
            channels = wave.channels.size
        ),
        wave = wave,
        spectrogram = spectrogram
    )
}