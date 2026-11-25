package testutil

import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates small PCM wav files used as sample file fixtures, so that no binary files need to be committed.
 */
object TestWav {

    const val DEFAULT_SAMPLE_RATE = 44100

    /**
     * Writes a 16-bit mono PCM wav file containing a sine wave to [file].
     */
    fun write(
        file: File,
        durationMs: Int = 1000,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        frequency: Double = 440.0,
    ) {
        val frameCount = sampleRate * durationMs / 1000
        val data = ByteArray(frameCount * 2)
        for (i in 0 until frameCount) {
            val value = (sin(2 * PI * frequency * i / sampleRate) * Short.MAX_VALUE * 0.5).toInt()
            data[i * 2] = (value and 0xFF).toByte()
            data[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
        }
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        file.parentFile?.mkdirs()
        AudioInputStream(ByteArrayInputStream(data), format, frameCount.toLong()).use { stream ->
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file)
        }
    }
}
