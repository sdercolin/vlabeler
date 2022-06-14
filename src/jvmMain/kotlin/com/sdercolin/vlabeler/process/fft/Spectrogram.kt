package com.sdercolin.vlabeler.process.fft

import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.windows.Hamming
import com.sdercolin.vlabeler.io.Wave
import kotlin.math.log10

fun Wave.Channel.toSpectrogram(sampleRate: Float): Array<DoubleArray> {
    val frameSize = 512
    val maxFrequencyRate = 0.5
    val frameCount = data.size / frameSize

    val window = Hamming(frameSize).window
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

    return Array(frameCount) { i ->
        DoubleArray(frequencyUnitCount) { j ->
            (20 * log10(absoluteSpectrogram[i][j] / frameSize)).coerceIn(-20.0, 40.0)
        }
    }
        .let { arr ->
            val min = arr.flatMap { it.toList() }.minOrNull()!!
            arr.map { it.map { p -> p - min } }
        }.let { arr ->
            val max = arr.flatMap { it.toList() }.maxOrNull()!!
            arr.map { it.map { p -> p / max }.toDoubleArray() }.toTypedArray()
        }
}