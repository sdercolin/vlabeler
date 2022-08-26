package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.NormalizedSampleRate
import com.sdercolin.vlabeler.io.WaveLoadingAlgorithmVersion
import kotlinx.serialization.Serializable
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.ceil

@Serializable
@Immutable
data class SampleInfo(
    val name: String,
    val file: String,
    val sampleRate: Float,
    val channels: Int,
    val length: Int,
    val lengthMillis: Float,
    val chunkSize: Int,
    val chunkCount: Int,
    val hasSpectrogram: Boolean,
    val lastModified: Long,
    val algorithmVersion: Int,
) {

    companion object {

        fun load(file: File, appConf: AppConf): Result<SampleInfo> = runCatching {
            val stream = AudioSystem.getAudioInputStream(file)
            val format = stream.format
            Log.debug("Sample info loaded: $format")
            val channelNumber = format.channels
            val frameLengthLong = stream.frameLength * NormalizedSampleRate / stream.format.sampleRate
            if (frameLengthLong > Int.MAX_VALUE) {
                throw IllegalArgumentException(
                    "Cannot load sample with frame length ($frameLengthLong) > ${Int.MAX_VALUE}",
                )
            }
            val frameLength = frameLengthLong.toInt()
            val channels = (0 until channelNumber).map { mutableListOf<Float>() }
            if (format.encoding !in arrayOf(AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_FLOAT)) {
                throw Exception("Unsupported audio encoding: ${format.encoding}")
            }
            val maxChunkSize = appConf.painter.maxDataChunkSize
            val sampleRate = NormalizedSampleRate
            val lengthInMillis = frameLength / sampleRate * 1000
            val chunkCount = ceil(frameLength.toDouble() / maxChunkSize).toInt()
            val chunkSize = frameLength / chunkCount
            stream.close()
            SampleInfo(
                name = file.nameWithoutExtension,
                file = file.absolutePath,
                sampleRate = sampleRate,
                channels = channels.size,
                length = frameLength,
                lengthMillis = lengthInMillis,
                chunkSize = chunkSize,
                chunkCount = chunkCount,
                hasSpectrogram = appConf.painter.spectrogram.enabled,
                lastModified = file.lastModified(),
                algorithmVersion = WaveLoadingAlgorithmVersion,
            )
        }
    }
}
