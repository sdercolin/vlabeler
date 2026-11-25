package io

import com.sdercolin.vlabeler.io.Wave
import com.sdercolin.vlabeler.io.toPower
import com.sdercolin.vlabeler.model.AppConf
import testutil.TestWaves
import kotlin.math.abs
import kotlin.math.log10
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [toPower].
 */
class PowerTest {

    private val conf = AppConf.Power()

    // the dB value produced for a silent signal: 20 * log10(1 / 2^15)
    private val silenceDb = (20 * log10(1.0 / 32768)).toFloat()

    @Test
    fun testSilenceIsAtFloor() {
        val wave = Wave(listOf(TestWaves.silentChannel(6000)))

        val power = wave.toPower(conf)

        assertEquals(1, power.data.size)
        assertTrue(power.data[0].isNotEmpty())
        power.data[0].forEach { assertEquals(silenceDb, it, 1e-3f) }
    }

    @Test
    fun testOutputShape() {
        val frameCount = 6000
        val wave = Wave(listOf(TestWaves.sineChannel(frameCount)))

        val power = wave.toPower(conf)

        // The data is padded with (windowSize - unitSize) / 2 zeros, then windowed by unitSize with partial windows.
        val paddedSize = frameCount + (conf.windowSize - conf.unitSize) / 2
        val expectedSize = (paddedSize + conf.unitSize - 1) / conf.unitSize
        assertEquals(expectedSize, power.data[0].size)
    }

    @Test
    fun testLouderSignalHasHigherPower() {
        val frameCount = 6000
        val loud = Wave(listOf(TestWaves.sineChannel(frameCount, amplitude = 16000f)))
        val quiet = Wave(listOf(TestWaves.sineChannel(frameCount, amplitude = 1600f)))

        val loudPower = loud.toPower(conf).data[0]
        val quietPower = quiet.toPower(conf).data[0]

        assertEquals(loudPower.size, quietPower.size)
        val middle = loudPower.size / 2
        assertTrue(loudPower[middle] > quietPower[middle])
        // 10x amplitude is +20 dB
        assertEquals(20f, loudPower[middle] - quietPower[middle], 0.1f)
    }

    @Test
    fun testFullScaleSinePowerValue() {
        val wave = Wave(listOf(TestWaves.sineChannel(6000, amplitude = 32767f)))

        val power = wave.toPower(conf).data[0]

        // RMS of a full-scale sine is 1/sqrt(2), which is about -3.01 dB.
        // Use a window in the middle: windows at the edges are affected by padding and partial window sizes.
        assertEquals(-3.01f, power[power.size / 2], 0.3f)
        power.forEach { assertTrue(it <= 0f && it >= silenceDb) }
    }

    @Test
    fun testMergeChannels() {
        val channel = TestWaves.sineChannel(6000)
        val wave = Wave(listOf(channel, channel))

        val merged = wave.toPower(conf.copy(mergeChannels = true))
        val separate = wave.toPower(conf.copy(mergeChannels = false))

        assertEquals(1, merged.data.size)
        assertEquals(2, separate.data.size)
        assertEquals(separate.data[0].size, separate.data[1].size)
        // identical channels: merged result equals each separate channel's result
        merged.data[0].forEachIndexed { i, value ->
            assertTrue(abs(value - separate.data[0][i]) < 1e-3f)
        }
    }

    @Test
    fun testOppositeChannelsCancelWhenMerged() {
        val channel = TestWaves.sineChannel(6000, amplitude = 16000f)
        val wave = Wave(listOf(channel, TestWaves.invertedChannel(channel)))

        val power = wave.toPower(conf.copy(mergeChannels = true))

        assertEquals(1, power.data.size)
        power.data[0].forEach { assertEquals(silenceDb, it, 1e-3f) }
    }
}
