package io

import com.sdercolin.vlabeler.io.NORMALIZED_SAMPLE_SIZE_IN_BITS
import com.sdercolin.vlabeler.io.normalize
import javax.sound.sampled.AudioFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [normalize].
 */
class AudioFormatTest {

    private fun format(
        sampleRate: Float,
        sampleSizeInBits: Int = 24,
        channels: Int = 1,
        isBigEndian: Boolean = false,
    ) = AudioFormat(sampleRate, sampleSizeInBits, channels, true, isBigEndian)

    @Test
    fun testZeroMaxSampleRateKeepsOriginalRate() {
        val normalized = format(96000f).normalize(0)
        assertEquals(96000f, normalized.sampleRate)
    }

    @Test
    fun testHigherRateIsResampledDownToMax() {
        val normalized = format(96000f).normalize(44100)
        assertEquals(44100f, normalized.sampleRate)
    }

    @Test
    fun testLowerRateIsKept() {
        val normalized = format(22050f).normalize(44100)
        assertEquals(22050f, normalized.sampleRate)
    }

    @Test
    fun testSampleSizeIsNormalized() {
        listOf(8, 16, 24, 32).forEach { bits ->
            val normalized = format(44100f, sampleSizeInBits = bits).normalize(44100)
            assertEquals(NORMALIZED_SAMPLE_SIZE_IN_BITS, normalized.sampleSizeInBits)
        }
    }

    @Test
    fun testChannelsAndEndiannessArePreserved() {
        val normalized = format(48000f, channels = 2, isBigEndian = true).normalize(44100)
        assertEquals(2, normalized.channels)
        assertTrue(normalized.isBigEndian)
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, normalized.encoding)
    }
}
