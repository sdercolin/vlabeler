package util

import com.sdercolin.vlabeler.util.or
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [or].
 */
class ElvisTest {

    @Test
    fun testOrWithNullReceiver() {
        val value: String? = null
        assertEquals("default", value.or("default"))
    }

    @Test
    fun testOrWithNonNullReceiver() {
        val value: String? = "actual"
        assertEquals("actual", value.or("default"))
    }
}
