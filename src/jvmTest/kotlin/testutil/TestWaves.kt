package testutil

import com.sdercolin.vlabeler.io.Wave
import kotlin.math.PI
import kotlin.math.sin

/**
 * Builds [Wave] objects in memory for DSP tests. Amplitude values use the same scale as the wave loading code, i.e.
 * raw 16-bit sample values (up to `Short.MAX_VALUE`).
 */
object TestWaves {

    fun sineChannel(
        frameCount: Int,
        sampleRate: Float = 44100f,
        frequency: Double = 440.0,
        amplitude: Float = Short.MAX_VALUE * 0.5f,
    ): Wave.Channel {
        val data = FloatArray(frameCount) { i ->
            (sin(2 * PI * frequency * i / sampleRate) * amplitude).toFloat()
        }
        return Wave.Channel(data)
    }

    fun silentChannel(frameCount: Int): Wave.Channel = Wave.Channel(FloatArray(frameCount))

    fun invertedChannel(channel: Wave.Channel): Wave.Channel =
        Wave.Channel(FloatArray(channel.data.size) { i -> -channel.data[i] })
}
