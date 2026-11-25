package util

import com.sdercolin.vlabeler.util.divideWithBigDecimal
import com.sdercolin.vlabeler.util.multiplyWithBigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for [multiplyWithBigDecimal] and [divideWithBigDecimal].
 */
class BigDemicalTest {

    @Test
    fun testMultiply() {
        assertEquals(6f, 2f.multiplyWithBigDecimal(3f))
        assertEquals(3f, 1.5f.multiplyWithBigDecimal(2f))
        assertEquals(0f, 0f.multiplyWithBigDecimal(123.45f))
        assertEquals(-2.5f, 1.25f.multiplyWithBigDecimal(-2f))
    }

    @Test
    fun testDivide() {
        assertEquals(2.5f, 5f.multiplyWithBigDecimal(0.5f))
        assertEquals(3.5f, 7f.divideWithBigDecimal(2f))
        assertEquals(0.25f, 1f.divideWithBigDecimal(4f))
        assertEquals(-1.5f, 3f.divideWithBigDecimal(-2f))
    }

    @Test
    fun testDivideWithNonTerminatingExpansionThrows() {
        // BigDecimal.divide is called without a scale/rounding mode,
        // so a non-terminating decimal expansion throws.
        assertFailsWith<ArithmeticException> {
            1f.divideWithBigDecimal(3f)
        }
    }
}
