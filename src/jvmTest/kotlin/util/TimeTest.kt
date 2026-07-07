package util

import com.sdercolin.vlabeler.util.toFrame
import com.sdercolin.vlabeler.util.toMillisecond
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [toFrame] and [toMillisecond]. See `env.TimeTest` for `getTimeText`.
 */
class TimeTest {

    @Test
    fun testToFrame() {
        assertEquals(44100f, toFrame(1000f, 44100f))
        assertEquals(22050f, toFrame(500f, 44100f))
        assertEquals(48f, toFrame(1f, 48000f))
        assertEquals(0f, toFrame(0f, 44100f))
    }

    @Test
    fun testToMillisecond() {
        assertEquals(1000f, toMillisecond(44100f, 44100f))
        assertEquals(500f, toMillisecond(22050f, 44100f))
        assertEquals(1f, toMillisecond(48f, 48000f))
        assertEquals(0f, toMillisecond(0f, 44100f))
    }

    @Test
    fun testRoundTrip() {
        val millis = 1234.5f
        assertEquals(millis, toMillisecond(toFrame(millis, 44100f), 44100f))
    }
}
