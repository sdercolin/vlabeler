package io

import com.sdercolin.vlabeler.io.Semitone
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.io.toFundamental
import com.sdercolin.vlabeler.io.toFundamentalSwipePrime
import com.sdercolin.vlabeler.model.AppConf
import testutil.TestWaves
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [Semitone], [toFundamental] and [toFundamentalSwipePrime].
 */
class FundamentalTest {

    private val conf = AppConf.Fundamental()
    private val sampleRate = 44100f

    @Test
    fun testSemitoneFromFrequency() {
        assertEquals(69f, Semitone.fromFrequency(440f), 1e-4f)
        assertEquals(81f, Semitone.fromFrequency(880f), 1e-4f)
        assertEquals(57f, Semitone.fromFrequency(220f), 1e-4f)
    }

    @Test
    fun testSemitoneToFrequency() {
        assertEquals(440f, Semitone.toFrequency(69f), 1e-2f)
        assertEquals(880f, Semitone.toFrequency(81f), 1e-2f)
    }

    @Test
    fun testSemitoneRoundTrip() {
        listOf(130.81f, 261.63f, 440f, 880f).forEach { frequency ->
            assertEquals(frequency, Semitone.toFrequency(Semitone.fromFrequency(frequency)), 1e-2f)
        }
    }

    @Test
    fun testSineFundamentalIsDetected() {
        val frequency = 220.0
        val wave = Wave(listOf(TestWaves.sineChannel(22050, frequency = frequency, amplitude = 16000f)))

        val fundamental = wave.toFundamental(conf, sampleRate)

        assertTrue(fundamental.freq.isNotEmpty())
        assertEquals(fundamental.freq.size, fundamental.corr.size)
        // all candidates are within the configured fundamental range (snapped to the semitone grid)
        fundamental.freq.forEach { assertTrue(it in 120f..900f) }
        // where the correlation is the highest, the detected frequency should be close to the sine frequency
        val bestIndex = fundamental.corr.indices.maxByOrNull { fundamental.corr[it] }!!
        val detected = fundamental.freq[bestIndex]
        assertTrue(
            detected in 200f..240f,
            "Expected the best candidate near $frequency Hz but got $detected Hz",
        )
    }

    @Test
    fun testHigherPitchIsDetectedHigher() {
        val wave = Wave(listOf(TestWaves.sineChannel(22050, frequency = 440.0, amplitude = 16000f)))

        val fundamental = wave.toFundamental(conf, sampleRate)

        val bestIndex = fundamental.corr.indices.maxByOrNull { fundamental.corr[it] }!!
        val detected = fundamental.freq[bestIndex]
        assertTrue(
            detected in 410f..470f,
            "Expected the best candidate near 440 Hz but got $detected Hz",
        )
    }

    @Test
    fun testInvalidConfReturnsFallback() {
        val invalidConf = conf.copy(minFundamental = 880f, maxFundamental = 130f)
        val wave = Wave(listOf(TestWaves.sineChannel(4410)))

        val fundamental = wave.toFundamentalSwipePrime(invalidConf, sampleRate)

        assertEquals(listOf(invalidConf.minFundamental), fundamental.freq)
        assertEquals(listOf(0f), fundamental.corr)
    }
}
