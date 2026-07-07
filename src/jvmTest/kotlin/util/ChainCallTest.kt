package util

import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.runIfHave
import com.sdercolin.vlabeler.util.runIfNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [runIf], [runIfNotNull] and [runIfHave].
 */
class ChainCallTest {

    @Test
    fun testRunIfWithTrueCondition() {
        assertEquals(2, 1.runIf(true) { this + 1 })
    }

    @Test
    fun testRunIfWithFalseCondition() {
        assertEquals(1, 1.runIf(false) { this + 1 })
    }

    @Test
    fun testRunIfNotNullWithTrueCondition() {
        assertEquals(2, 1.runIfNotNull(true) { this + 1 })
        assertNull(1.runIfNotNull(true) { null })
    }

    @Test
    fun testRunIfNotNullWithFalseCondition() {
        assertEquals(1, 1.runIfNotNull(false) { null })
    }

    @Test
    fun testRunIfHaveWithParameter() {
        assertEquals("ab", "a".runIfHave("b") { this + it })
    }

    @Test
    fun testRunIfHaveWithoutParameter() {
        val parameter: String? = null
        assertEquals("a", "a".runIfHave(parameter) { this + it })
    }
}
