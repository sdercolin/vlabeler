package com.sdercolin.vlabeler.process

import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.windows.Bartlett
import com.github.psambit9791.jdsp.windows.Blackman
import com.github.psambit9791.jdsp.windows.Hamming
import com.github.psambit9791.jdsp.windows.Hanning
import com.github.psambit9791.jdsp.windows.Rectangular
import com.github.psambit9791.jdsp.windows.Triangular
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.model.AppConf
import kotlin.math.log10

fun Wave.Channel.toSpectrogram(conf: AppConf.Spectrogram, sampleRate: Float): Array<DoubleArray> {
    val frameSize = conf.frameSize
    val maxFrequencyRate = conf.maxFrequency / sampleRate * 2
    val frameCount = data.size / frameSize

    val window = when (conf.windowType) {
        AppConf.WindowType.Hamming -> Hamming(frameSize).window
        AppConf.WindowType.Hanning -> Hanning(frameSize).window
        AppConf.WindowType.Rectangular -> Rectangular(frameSize).window
        AppConf.WindowType.Triangular -> Triangular(frameSize).window
        AppConf.WindowType.Blackman -> Blackman(frameSize).window
        AppConf.WindowType.Bartlett -> Bartlett(frameSize).window
    }
    val signals: Array<DoubleArray> = Array(frameCount) { i ->
        DoubleArray(frameSize) { j ->
            data[i * frameSize + j] * window[j]
        }
    }

    val absoluteSpectrogram: Array<DoubleArray> = Array(frameCount) { i ->
        val signal = signals[i].copyOf()
        val fft = FastFourier(signal)
        fft.transform()
        val magnitude = fft.getMagnitude(true)
        magnitude.copyOf((magnitude.size * maxFrequencyRate).toInt())
    }

    if (absoluteSpectrogram.isEmpty()) return arrayOf()
    val frequencyUnitCount = absoluteSpectrogram.first().size

    val min = conf.minIntensity.toDouble()
    val max = conf.maxIntensity.toDouble()
    return Array(frameCount) { i ->
        DoubleArray(frequencyUnitCount) { j ->
            (20 * log10(absoluteSpectrogram[i][j] / frameSize))
                .coerceIn(min, max)
                .let { (it - min) / (max - min) }
        }
    }
}