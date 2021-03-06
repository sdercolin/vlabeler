package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.windows.Bartlett
import com.github.psambit9791.jdsp.windows.Blackman
import com.github.psambit9791.jdsp.windows.BlackmanHarris
import com.github.psambit9791.jdsp.windows.Hamming
import com.github.psambit9791.jdsp.windows.Hanning
import com.github.psambit9791.jdsp.windows.Rectangular
import com.github.psambit9791.jdsp.windows.Triangular
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import kotlin.math.absoluteValue
import kotlin.math.log10

@Immutable
data class Spectrogram(val data: List<DoubleArray>, val frameSize: Int)

fun Wave.toSpectrogram(conf: AppConf.Spectrogram, sampleRate: Float): Spectrogram {
    val dataLength = channels.minOf { it.data.size }
    val data = DoubleArray(dataLength) { i ->
        channels.sumOf { it.data[i].toDouble() } / channels.size
    }
    val frameSize = conf.frameSize
    val maxFrequencyRate = conf.maxFrequency / sampleRate * 2
    val frameCount = data.size / frameSize
    val window = when (conf.windowType) {
        AppConf.WindowType.Hamming -> Hamming(frameSize).window
        AppConf.WindowType.Hanning -> Hanning(frameSize).window
        AppConf.WindowType.Rectangular -> Rectangular(frameSize).window
        AppConf.WindowType.Triangular -> Triangular(frameSize).window
        AppConf.WindowType.Blackman -> Blackman(frameSize).window
        AppConf.WindowType.BlackmanHarris -> BlackmanHarris(frameSize).window
        AppConf.WindowType.Bartlett -> Bartlett(frameSize).window
    }
    val peak = data.maxByOrNull { it.absoluteValue }!!
    val signals: Array<DoubleArray> = Array(frameCount) { i ->
        DoubleArray(frameSize) { j ->
            data[i * frameSize + j] * window[j] / peak * Short.MAX_VALUE
        }
    }

    val absoluteSpectrogram: Array<DoubleArray> = Array(frameCount) { i ->
        val signal = signals[i].copyOf()
        val fft = FastFourier(signal)
        fft.transform()
        val magnitude = fft.getMagnitude(true)
        magnitude.copyOf((magnitude.size * maxFrequencyRate).toInt())
    }

    if (absoluteSpectrogram.isEmpty()) return Spectrogram(listOf(), frameSize)
    val frequencySize = absoluteSpectrogram.first().size

    val min = conf.minIntensity.toDouble()
    val max = conf.maxIntensity.toDouble()
    runCatching { require(min < max) { "minIntensity must be less than maxIntensity" } }
        .onFailure {
            Log.error(it)
            return Spectrogram(listOf(), frameSize)
        }
    val output = List(frameCount) { i ->
        DoubleArray(frequencySize) { j ->
            (20 * log10(absoluteSpectrogram[i][j] / frameSize))
                .coerceIn(min, max)
                .let { (it - min) / (max - min) }
        }
    }
    return Spectrogram(output, frameSize)
}
