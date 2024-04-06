package com.sdercolin.vlabeler.io

import androidx.compose.runtime.Immutable
import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.windows.Hanning
import com.sdercolin.vlabeler.model.AppConf
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Data class to represent SWIPE kernels.
 *
 * @property candidateFrequency A list of candidate fundamental frequencies.
 * @property maxHarmonicFrequency The maximum harmonic frequency to be calculated.
 * @property frequencyXAxis The kernel data is stored in ERBs. This is the frequency axis in ERBs.
 * @property kernels A list of kernels.
 * @property kernelNorm The norm of each kernel.
 */
@Immutable
private data class SwipeKernelData(
    val candidateFrequency: List<Float>,
    val maxHarmonicFrequency: Float,
    val frequencyXAxis: List<Float>,
    val kernels: List<FloatArray>,
    val kernelNorm: List<Float>,
)

private const val EPS = 1e-8f

/**
 * Data class to represent SWIPE kernels.
 */
private object SwipeKernel {
    private var kernelData: SwipeKernelData? = null
    private var currentConf: AppConf.Fundamental? = null
    private var currentSampleRate: Float? = null

    /**
     * Get harmonics (1 and the prime numbers less than or equal to [numOfHarmonics]).
     *
     * @param numOfHarmonics The maximum number of harmonics.
     */
    private fun getHarmonics(numOfHarmonics: Int): List<Int> {
        val primes = mutableListOf<Int>()
        primes.add(1)
        var i = 2
        while (i <= numOfHarmonics) {
            var isPrime = true
            val end = sqrt(i.toFloat()).toInt()
            for (j in 2..end) {
                if (i % j == 0) {
                    isPrime = false
                    break
                }
            }
            if (isPrime) {
                primes.add(i)
            }
            i++
        }
        return primes
    }

    /**
     * Update SWIPE kernels and other data to calculate the fundamental frequency.
     *
     * @param conf The configuration of the fundamental frequency.
     * @param sampleRate The sample rate of the wave.
     */
    private fun updateKernel(conf: AppConf.Fundamental, sampleRate: Float): SwipeKernelData? {
        if (conf.semitoneSampleNum < 1 || conf.maxFundamental < conf.minFundamental) {
            return null
        }

        // Calculate the candidate fundamental frequency.
        val startSemitone = round(Semitone.fromFrequency(conf.minFundamental))
        val endSemitone = round(Semitone.fromFrequency(conf.maxFundamental))
        val semitoneStep = 1f / conf.semitoneSampleNum.toFloat()
        val candidateLength = floor((endSemitone - startSemitone) / semitoneStep + 1).toInt()
        val candidateFrequency = List(candidateLength) { i ->
            Semitone.toFrequency(startSemitone + i * semitoneStep)
        }

        // Calculate the sample frequency axis by ERBs.
        val maxHarmonicFrequency = minOf(conf.maxHarmonicFrequency, sampleRate / 2)
        val erbsLength = maxOf(ceil(ERBs.fromFrequency(maxHarmonicFrequency) / conf.erbsStep).toInt(), 1)
        val frequencyXAxis = List(erbsLength) { i -> ERBs.toFrequency(conf.erbsStep * i) }

        // function to convert frequency to index.
        val freqToIndex: (Float, Boolean) -> Int = { freq, upperBound ->
            val erbs = ERBs.fromFrequency(freq)
            val index = if (upperBound) (erbs / conf.erbsStep + 1).toInt() else (erbs / conf.erbsStep).toInt()
            maxOf(0, minOf(erbsLength - 1, index))
        }

        // Calculate kernels
        val kernels = candidateFrequency.map { fundamentalFreq ->
            val numOfHarmonics = ceil(maxHarmonicFrequency / fundamentalFreq).toInt()
            val harmonics = getHarmonics(numOfHarmonics)
            val kernel = FloatArray(erbsLength) { 0.0f }
            harmonics.map { idx ->
                val centerFreq = fundamentalFreq * idx
                val startIndex = freqToIndex(centerFreq - 0.75f * fundamentalFreq, false)
                val oneThirdIndex = freqToIndex(centerFreq - 0.25f * fundamentalFreq, true)
                val twoThirdIndex = freqToIndex(centerFreq + 0.25f * fundamentalFreq, true)
                val endIndex = freqToIndex(centerFreq + 0.75f * fundamentalFreq, true)
                // Draw the first part of the kernel.
                for (i in startIndex until oneThirdIndex) {
                    val freq = frequencyXAxis[i]
                    kernel[i] += 0.5f * cos(2.0f * Math.PI.toFloat() * freq / fundamentalFreq)
                }
                // Draw the second part of the kernel.
                for (i in oneThirdIndex until twoThirdIndex) {
                    val freq = frequencyXAxis[i]
                    kernel[i] += cos(2.0f * Math.PI.toFloat() * freq / fundamentalFreq)
                }
                // Draw the third part of the kernel.
                for (i in twoThirdIndex until endIndex) {
                    val freq = frequencyXAxis[i]
                    kernel[i] += 0.5f * cos(2.0f * Math.PI.toFloat() * freq / fundamentalFreq)
                }
            }
            kernel
        }

        // Calculate kernel norm.
        // According to (3-11), divide the kernel by the frequency.
        val kernelNorm = kernels.map { kernel ->
            kernel.mapIndexed { i, value ->
                if (frequencyXAxis[i] == 0.0f) 0.0f else value * value / frequencyXAxis[i]
            }.sum().let { maxOf(sqrt(it), EPS) }
        }

        return SwipeKernelData(
            candidateFrequency = candidateFrequency,
            maxHarmonicFrequency = maxHarmonicFrequency,
            frequencyXAxis = frequencyXAxis,
            kernels = kernels,
            kernelNorm = kernelNorm,
        )
    }

    /**
     * Get SWIPE kernels and other data to calculate the fundamental frequency.
     *
     * @param conf The configuration of the fundamental frequency.
     * @param sampleRate The sample rate of the wave.
     */
    @Synchronized
    fun getKernel(conf: AppConf.Fundamental, sampleRate: Float): SwipeKernelData? {
        if (kernelData == null || currentConf != conf || currentSampleRate != sampleRate) {
            kernelData = updateKernel(conf, sampleRate)
            if (kernelData == null) {
                return null
            }
            currentConf = conf
            currentSampleRate = sampleRate
        }
        return kernelData!!
    }
}

/**
 * Convert a wave to fundamental frequency using SWIPE'. Reference: A sawtooth waveform inspired pitch estimator for
 * speech and music @article{camacho2008sawtooth, title={A sawtooth waveform inspired pitch estimator for speech and
 * music}, author={Camacho, Arturo and Harris, John G}, journal={The Journal of the Acoustical Society of America},
 * volume={124}, number={3}, pages={1638--1652}, year={2008}, publisher={AIP Publishing} }
 *
 * @param conf The configuration of the fundamental frequency.
 * @param sampleRate The sample rate of the wave.
 */
fun Wave.toFundamentalSwipePrime(conf: AppConf.Fundamental, sampleRate: Float): Fundamental {
    // Calculate kernel of each candidateFreq.
    val kernelData = SwipeKernel.getKernel(conf, sampleRate)
        ?: return Fundamental(
            List(1) { conf.minFundamental },
            List(1) { 0.0f },
        )

    // Calculate the window size according to the 3.7.
    // Always use the Hanning window which has k=2. So T = 4k/f = 8/f.
    // Then N = T / (1/sampleRate) = 8 * sampleRate / f.
    val fftBestWindowSize = kernelData.candidateFrequency.map { 8.0f * sampleRate / it }
    // According to 3.12.1.2, use only power-of-two window size.
    val minWindowSizeLog2 = floor(log2(fftBestWindowSize.min())).toInt()
    val maxWindowSizeLog2 = ceil(log2(fftBestWindowSize.max())).toInt()
    val windowSizeList = (minWindowSizeLog2..maxWindowSizeLog2).map { 2.0.pow(it).toInt() }

    // FFT by windowSizeList.
    // rawFFTResult[i][j] is the FFT result of the i-th window size and the j-th window.
    val rawFFTResults = windowSizeList.map { windowSize ->
        // Convert FFT result to sqrt(|X(f)|)
        this.fastFourierTransformSwipe(windowSize, sampleRate, kernelData.maxHarmonicFrequency)
            .map { it.map { value -> value.toFloat() } }
    }

    // Interpolate the FFT result by ERBs and convert to sqrt(|X(e)|).
    // erbsFFTResult[i][j] has the same size as any kernelData.kernels[k].
    // Maybe use spline of interp1 in Matlab to get better performance?
    val erbsFFTResults = rawFFTResults.mapIndexed { windowIdx, listOfResults ->
        // Convert frequency to index.
        // The max frequency = sampleRate / 2
        // The FFT result has a size of （windowSize / 2 + 1)
        // So the frequency step = (sampleRate / 2) / (windowSize / 2 + 1 - 1) = sampleRate / windowSize
        val samplePos = kernelData.frequencyXAxis.map { it / (sampleRate / windowSizeList[windowIdx]) }
        listOfResults.map {
            val interpolated = samplePos.map { pos ->
                val idx = minOf(pos, (it.size - 1).toFloat())
                val lower = floor(idx).toInt()
                val upper = ceil(idx).toInt()
                val lowerValue = it[lower]
                val upperValue = it[upper]
                val value = lowerValue + (upperValue - lowerValue) * (pos - lower)
                sqrt(abs(value))
            }
            interpolated.toFloatArray()
        }
    }

    // Calculate signal norm.
    val fftResultNorm = erbsFFTResults.map { listOfResults ->
        listOfResults.map { result -> // result is the FFT result of a window.
            result.map {
                it * it
            }.sum().let { maxOf(sqrt(it), EPS) }
        }
    }

    // For each candidate frequency, calculate the convolution of its kernel and the FFT result
    // of the nearest 2 window sizes, then merge the result at the lower window size.
    val convResults = fftBestWindowSize.mapIndexed { candidateIdx, bestWindowSize ->
        val bestWindowSizeLog2 = log2(bestWindowSize)
        val intWindowSizeLog2 = floor(bestWindowSizeLog2).toInt()
        val fracWindowSizeLog2 = bestWindowSizeLog2 - intWindowSizeLog2
        val intFFTIndex = windowSizeList.indexOf(2.0.pow(intWindowSizeLog2).toInt())
        // Get kernel.
        val kernel = kernelData.kernels[candidateIdx]
        val kernelNorm = kernelData.kernelNorm[candidateIdx]
        // Calculate convolution of the kernel and the FFT results of intFFTIndex.
        var conv = erbsFFTResults[intFFTIndex].mapIndexed { pos, fft ->
            fft.mapIndexed { i, value ->
                if (kernelData.frequencyXAxis[i] == 0.0f) {
                    0.0f
                } else {
                    value * kernel[i] / sqrt(kernelData.frequencyXAxis[i])
                }
            }.sum() / kernelNorm / fftResultNorm[intFFTIndex][pos]
        }
        // Calculate convolution of the kernel and the FFT result of intFFTIndex + 1.
        if (fracWindowSizeLog2 > 0 && intFFTIndex + 1 < erbsFFTResults.size) {
            val nextConv = erbsFFTResults[intFFTIndex + 1].mapIndexed { pos, fft ->
                fft.mapIndexed { i, value ->
                    if (kernelData.frequencyXAxis[i] == 0.0f) {
                        0.0f
                    } else {
                        value * kernel[i] / sqrt(kernelData.frequencyXAxis[i])
                    }
                }.sum() / kernelNorm / fftResultNorm[intFFTIndex + 1][pos]
            }
            // Merge the result.
            // The nextConv is calculated by the 2 times larger window size. So its size is half of the conv.
            conv = conv.mapIndexed { i, value ->
                value * (1 - fracWindowSizeLog2) + nextConv[i / 2] * fracWindowSizeLog2
            }
        }
        // Repeat all results to the minimum window size.
        val repeatTime = 2.0.pow(intFFTIndex).toInt()
        FloatArray(conv.size * repeatTime) { i -> conv[i / repeatTime] }
    }

    // Calculate the fundamental frequency.
    // TODO: According to 3.10.2, we can interpolate the result to get a continuous frequency output.
    val length = convResults.minOfOrNull { it.size } ?: 0
    val fundamental = List(length) { i ->
        val maxIdx = convResults.indices.maxByOrNull { j -> convResults[j][i] } ?: 0
        kernelData.candidateFrequency[maxIdx]
    }
    val corr = List(length) { i ->
        convResults.indices.maxOfOrNull { j -> convResults[j][i] } ?: 0.0f
    }

    return Fundamental(fundamental, corr)
}

/**
 * Convert frequency to ERBs and vice versa. ERBs = 21.4 * log10(1 + f / 229).
 */
private object ERBs {
    /**
     * Convert frequency to ERBs.
     *
     * @param frequency The frequency to be converted.
     */
    fun fromFrequency(frequency: Float): Float {
        return 21.4f * log10(1.0f + frequency / 229.0f)
    }

    /**
     * Convert ERBs to frequency.
     *
     * @param erbs The ERBs to be converted.
     */
    fun toFrequency(erbs: Float): Float {
        return 229.0f * (10.0f.pow(erbs / 21.4f) - 1.0f)
    }
}

/**
 * Calculate FFT by window size.
 *
 * @param windowSize The size of the window.
 * @param sampleRate The sample rate of the wave.
 * @param maxHarmonicFrequency The maximum harmonic frequency to be returned.
 */
private fun Wave.fastFourierTransformSwipe(
    windowSize: Int,
    sampleRate: Float,
    maxHarmonicFrequency: Float,
): List<DoubleArray> {
    // Sum all channels.
    val dataLength = channels.minOf { it.data.size }
    val data = DoubleArray(dataLength) { i ->
        channels.sumOf { it.data[i].toDouble() } / channels.size
    }

    // According to 3.10.1.1, the hop size is half of the window size.
    val maxFrequencyRate = maxHarmonicFrequency / sampleRate * 2
    val hopSize = maxOf(windowSize / 2, 1)
    val window = Hanning(windowSize).window

    // Pad the data and apply window.
    val paddedData = List(windowSize / 2) { 0.0 } + data.toList() + List(windowSize / 2) { 0.0 }
    val frames = paddedData.windowed(windowSize, hopSize) { frame ->
        frame.mapIndexed { index, point -> point * window[index] }.toDoubleArray()
    }

    // FFT
    val absoluteSpectrogram: Array<DoubleArray> = Array(frames.size) { i ->
        val signal = frames[i]
        val fft = FastFourier(signal)
        fft.transform()
        val magnitude = fft.getMagnitude(true)
        magnitude.copyOf((magnitude.size * maxFrequencyRate).toInt())
    }

    return absoluteSpectrogram.toList()
}
