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
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
data class Spectrogram(val data: List<DoubleArray>, val hopSize: Int)

private const val StandardSampleRate = 44100

fun Wave.toSpectrogram(conf: AppConf.Spectrogram, sampleRate: Float): Spectrogram {
    val dataLength = channels.minOf { it.data.size }
    val data = DoubleArray(dataLength) { i ->
        channels.sumOf { it.data[i].toDouble() } / channels.size
    }
    val maxFrequencyRate = conf.maxFrequency / sampleRate * 2
    val hopSize = (conf.standardHopSize * sampleRate / StandardSampleRate).roundToInt()
    val windowSize = (conf.standardWindowSize * 2.0.pow(sampleRate.toDouble() / StandardSampleRate)).toInt()

    val window = when (conf.windowType) {
        AppConf.WindowType.Hamming -> Hamming(windowSize).window
        AppConf.WindowType.Hanning -> Hanning(windowSize).window
        AppConf.WindowType.Rectangular -> Rectangular(windowSize).window
        AppConf.WindowType.Triangular -> Triangular(windowSize).window
        AppConf.WindowType.Blackman -> Blackman(windowSize).window
        AppConf.WindowType.BlackmanHarris -> BlackmanHarris(windowSize).window
        AppConf.WindowType.Bartlett -> Bartlett(windowSize).window
    }
    val peak = data.maxByOrNull { it.absoluteValue }!!
    val paddedData = List(windowSize / 2) { 0.0 } + data.toList() + List(windowSize / 2) { 0.0 }
    val frames = paddedData.windowed(windowSize, hopSize) { frame ->
        frame.mapIndexed { index, point -> point * window[index] / peak * Short.MAX_VALUE }.toDoubleArray()
    }

    val absoluteSpectrogram: Array<DoubleArray> = Array(frames.size) { i ->
        val signal = frames[i]
        val fft = FastFourier(signal)
        fft.transform()
        val magnitude = fft.getMagnitude(true)
        magnitude.copyOf((magnitude.size * maxFrequencyRate).toInt())
    }

    if (absoluteSpectrogram.isEmpty()) return Spectrogram(listOf(), hopSize)
    val frequencySize = absoluteSpectrogram.first().size

    val min = conf.minIntensity.toDouble()
    val max = conf.maxIntensity.toDouble()
    runCatching { require(min < max) { "minIntensity must be less than maxIntensity" } }
        .onFailure {
            Log.error(it)
            return Spectrogram(listOf(), hopSize)
        }
    val output = List(frames.size) { i ->
        DoubleArray(frequencySize) { j ->
            (20 * log10(absoluteSpectrogram[i][j] / windowSize))
                .coerceIn(min, max)
                .let { (it - min) / (max - min) }
        }
    }
    return Spectrogram(output, hopSize)
}

object MelScale {
    fun toMel(frequency: Double): Double {
        return 2595 * log10(1 + frequency / 700)
    }
}
