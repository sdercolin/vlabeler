package util

import com.sdercolin.vlabeler.util.roundToDecimalDigit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [roundToDecimalDigit].
 */
class MathTest {

    @Test
    fun testFloatRoundToDecimalDigit() {
        assertEquals(1.23f, 1.234f.roundToDecimalDigit(2))
        assertEquals(1.24f, 1.236f.roundToDecimalDigit(2))
        assertEquals(1.2f, 1.234f.roundToDecimalDigit(1))
        assertEquals(1f, 1.234f.roundToDecimalDigit(0))
        assertEquals(5f, 5f.roundToDecimalDigit(3))
    }

    @Test
    fun testFloatRoundToDecimalDigitWithNull() {
        assertEquals(1.234f, 1.234f.roundToDecimalDigit(null))
    }

    @Test
    fun testDoubleRoundToDecimalDigit() {
        assertEquals(1.23, 1.234.roundToDecimalDigit(2))
        assertEquals(1.24, 1.236.roundToDecimalDigit(2))
        assertEquals(1.2, 1.234.roundToDecimalDigit(1))
        assertEquals(1.0, 1.234.roundToDecimalDigit(0))
        // values whose scaled representation exceeds the Float precision (2^24) must not lose precision
        assertEquals(200000.01, 200000.011.roundToDecimalDigit(2))
    }

    @Test
    fun testDoubleRoundToDecimalDigitWithNull() {
        assertEquals(1.234, 1.234.roundToDecimalDigit(null))
    }
}
