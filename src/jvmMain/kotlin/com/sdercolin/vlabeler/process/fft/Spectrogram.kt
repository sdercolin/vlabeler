package com.sdercolin.vlabeler.process.fft

import com.sdercolin.vlabeler.io.Wave
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType

fun Wave.Channel.toSpectrogram(sampleRate: Float): Array<DoubleArray> {
    val frameSize = 512
    val frameCount = data.size / frameSize

    val window = getHammingWindowFactors(frameSize)
    val signals: Array<DoubleArray> = Array(frameCount) { i ->
        DoubleArray(frameSize) { j ->
            data[i * frameSize + j] * window[j]
        }
    }

    val fft = FastFourierTransformer(DftNormalization.UNITARY)
    val absoluteSpectrogram: Array<DoubleArray> = Array(frameCount) { i ->
        fft.transform(signals[i], TransformType.FORWARD).toList().map { it.abs() }
            .let { it.take(it.size / 2) }
            .toDoubleArray()
    }

    if (absoluteSpectrogram.isEmpty()) return arrayOf()
    val frequencyUnitCount = absoluteSpectrogram.first().size

    return Array(frameCount) { i ->
        DoubleArray(frequencyUnitCount) { j ->
            log10(absoluteSpectrogram[i][j])
        }
    }.let { arr ->
        val min = arr.flatMap { it.toList() }.minOrNull()!!
        arr.map { it.map { p -> p - min } }
    }.let { arr ->
        val max = arr.flatMap { it.toList() }.maxOrNull()!!
        arr.map { it.map { p -> p / max }.toDoubleArray() }.toTypedArray()
    }
}

private fun getHammingWindowFactors(windowSize: Int): DoubleArray {
    val result = DoubleArray(windowSize)
    for (i in 0 until windowSize) {
        result[i] = 0.54 + 0.46 * cos(2 * PI * i / (windowSize - 1))
    }
    return result
}


private class Radix2Fft(
    private val n: Int // Should be power of 2
) {
    private val nLogged = log2(n.toDouble()).toInt()
    private val fftSize = n / 2
    private val twoPiDivN = PI * 2 / n
    private val x = Array(n) { Complex() }
    private val dft = Array(n) { Complex() }

    fun run(input: Array<Float>, output: Array<Double>) {
        repeat(n) { i ->
            x[i].re = input[i].toDouble()
        }
        performFft()
        repeat(fftSize) { i ->
            output[i] = 20 * log10(dft[i].getMagnitude()) / n
        }
    }

    private fun performFft() {
        var bSep: Int // BSep is memory spacing between butterflies
        var bWidth: Int // BWidth is memory spacing of opposite ends of the butterfly
        var p: Int // P is number of similar Wn's to be used in that stage
        var iaddr: Int // bitmask for bit reversal
        var ii: Int // Integer bitfield for bit reversal (Decimation in Time)
        var dftIndex = 0 // Pointer to first elements in DFT array

        // Decimation In Time - x[n] sample sorting
        var i = 0
        while (i < n) {
            val pX: Complex = x[i] // Calculate current x[n] from index i.
            ii = 0 // Reset new address for DFT[n]
            iaddr = i // Copy i for manipulations
            for (l in 0 until nLogged)  // Bit reverse i and store in ii...
            {
                if (iaddr and 0x01 != 0) // Detemine least significant bit
                    ii += (1 shl (nLogged - 1)) - l // Increment ii by 2^(M-1-l) if lsb was 1
                iaddr = iaddr shr 1 // right shift iaddr to test next bit. Use logical operations for speed increase
                if (iaddr == 0) break
            }
            val dftItem: Complex = dft[ii] // Calculate current DFT[n] from bit reversed index ii
            dftItem.re = pX.re // Update the complex array with address sorted time domain signal x[n]
            dftItem.im = pX.im // NB: Imaginary is always zero
            i++
            dftIndex++
        }

        val wn = Complex()

        // FFT Computation by butterfly calculation
        for (stage in 1..nLogged)  // Loop for M stages, where 2^M = N
        {
            bSep = 2.0.pow(stage.toDouble()).toInt() // Separation between butterflies = 2^stage
            p = n / bSep // Similar Wn's in this stage = N/Bsep
            bWidth = bSep / 2 // Butterfly width (spacing between opposite points) = Separation / 2.
            for (j in 0 until bWidth)  // Loop for j calculations per butterfly
            {
                if (j != 0) // Save on calculation if R = 0, as WN^0 = (1 + j0)
                {
                    wn.re = cos(twoPiDivN * p * j) // Calculate Wn (Real and Imaginary)
                    wn.im = -sin(twoPiDivN * p * j)
                }

                // HiIndex is the index of the DFT array for the top value of each butterfly calc
                var hiIndex = j
                val temp = Complex()
                while (hiIndex < n) {
                    val pHi: Complex = dft[hiIndex] // Point to higher value
                    val pLo: Complex = dft[hiIndex + bWidth] // Point to lower value
                    if (j != 0) // If exponential power is not zero...
                    {
                        // Perform complex multiplication of LoValue with Wn
                        temp.re = pLo.re * wn.re - pLo.im * wn.im
                        temp.im = pLo.re * wn.im + pLo.im * wn.re

                        // Find new LoValue (complex subtraction)
                        pLo.re = pHi.re - temp.re
                        pLo.im = pHi.im - temp.im

                        // Find new HiValue (complex addition)
                        pHi.re = pHi.re + temp.re
                        pHi.im = pHi.im + temp.im
                    } else {
                        temp.re = pLo.re
                        temp.im = pLo.im

                        // Find new LoValue (complex subtraction)
                        pLo.re = pHi.re - temp.re
                        pLo.im = pHi.im - temp.im

                        // Find new HiValue (complex addition)
                        pHi.re = pHi.re + temp.re
                        pHi.im = pHi.im + temp.im
                    }
                    hiIndex += bSep
                }
            }
        }
    }

}

private class Complex(var re: Double = 0.0, var im: Double = 0.0) {
    fun getMagnitude() = sqrt(re.pow(2) + im.pow(2))
}