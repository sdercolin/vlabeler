package util

import com.sdercolin.vlabeler.util.contains
import com.sdercolin.vlabeler.util.length
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the `contains` operator and [length] of `FloatRange`.
 */
class FloatRangeTest {

    @Test
    fun testContainsOverlappingRange() {
        assertEquals(true, (1f..5f).contains(3f..7f))
        assertEquals(true, (1f..5f).contains(0f..2f))
        assertEquals(true, (1f..5f).contains(2f..3f))
        assertEquals(true, (1f..5f).contains(0f..10f))
    }

    @Test
    fun testContainsTouchingRange() {
        assertEquals(true, (1f..5f).contains(5f..7f))
        assertEquals(true, (1f..5f).contains(0f..1f))
    }

    @Test
    fun testContainsDisjointRange() {
        assertEquals(false, (1f..5f).contains(6f..7f))
        assertEquals(false, (1f..5f).contains(-2f..0f))
    }

    @Test
    fun testLength() {
        assertEquals(4f, (1f..5f).length)
        assertEquals(0f, (2f..2f).length)
        assertEquals(3f, (-1.5f..1.5f).length)
    }
}
