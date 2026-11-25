package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.MelScale
import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.io.toSpectrogram
import com.sdercolin.vlabeler.model.AppConf
import testutil.TestWaves
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [toSpectrogram] and [MelScale].
 */
class SpectrogramTest {

    private val conf = AppConf.Spectrogram()
    private val sampleRate = 44100f

    // matches the window size calculation in toSpectrogram for 44100 Hz
    private val windowSize = (conf.standardWindowSize * 2.0.pow((sampleRate / 44100f).toDouble())).toInt()

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun testShapeAndValueRange() {
        val wave = Wave(listOf(TestWaves.sineChannel(22050, frequency = 1000.0, amplitude = 1000f)))

        val spectrogram = wave.toSpectrogram(conf, sampleRate)

        assertEquals(conf.standardHopSize, spectrogram.hopSize)
        assertTrue(spectrogram.data.isNotEmpty())
        val frequencySize = spectrogram.data.first().size
        assertTrue(frequencySize > 0)
        spectrogram.data.forEach { frame ->
            assertEquals(frequencySize, frame.size)
            frame.forEach { value -> assertTrue(value in 0.0..1.0) }
        }
    }

    @Test
    fun testHopSizeScalesWithSampleRate() {
        val lowSampleRate = 22050f
        val wave = Wave(listOf(TestWaves.sineChannel(11025, sampleRate = lowSampleRate, amplitude = 1000f)))

        val spectrogram = wave.toSpectrogram(conf, lowSampleRate)

        assertEquals((conf.standardHopSize * lowSampleRate / 44100).roundToInt(), spectrogram.hopSize)
    }

    @Test
    fun testEnergyPeaksAtSineFrequency() {
        val frequency = 1000.0
        val wave = Wave(listOf(TestWaves.sineChannel(22050, frequency = frequency, amplitude = 1000f)))

        val spectrogram = wave.toSpectrogram(conf, sampleRate)

        // pick a frame in the middle where the window fully covers the sine wave
        val frame = spectrogram.data[spectrogram.data.size / 2]
        val peakBin = frame.indices.maxByOrNull { frame[it] }!!
        val binFrequency = peakBin * sampleRate / windowSize
        assertTrue(
            abs(binFrequency - frequency) < 100.0,
            "Expected peak near $frequency Hz but found it at $binFrequency Hz",
        )
        assertTrue(frame[peakBin] > 0.5)
        // a far away bin should carry almost no energy
        val farBin = (5000 * windowSize / sampleRate).toInt()
        assertTrue(frame[farBin] < 0.1)
    }

    @Test
    fun testOppositeChannelsCancelWhenMerged() {
        val channel = TestWaves.sineChannel(22050, frequency = 1000.0, amplitude = 1000f)
        val wave = Wave(listOf(channel, TestWaves.invertedChannel(channel)))

        val spectrogram = wave.toSpectrogram(conf, sampleRate)

        assertTrue(spectrogram.data.isNotEmpty())
        spectrogram.data.forEach { frame ->
            frame.forEach { value -> assertEquals(0.0, value, 1e-9) }
        }
    }

    @Test
    fun testInvalidIntensityRangeReturnsEmptyData() {
        val wave = Wave(listOf(TestWaves.sineChannel(22050, amplitude = 1000f)))

        val spectrogram = wave.toSpectrogram(conf.copy(minIntensity = 55, maxIntensity = 55), sampleRate)

        assertTrue(spectrogram.data.isEmpty())
    }

    @Test
    fun testMelScaleRoundTrip() {
        assertEquals(0.0, MelScale.toMel(0.0), 1e-9)
        listOf(100.0, 440.0, 1000.0, 8000.0).forEach { frequency ->
            assertEquals(frequency, MelScale.toFreq(MelScale.toMel(frequency)), 1e-6)
        }
    }

    @Test
    fun testMelScaleIsMonotonic() {
        val mels = listOf(100.0, 440.0, 1000.0, 8000.0).map { MelScale.toMel(it) }
        assertEquals(mels, mels.sorted())
    }
}
