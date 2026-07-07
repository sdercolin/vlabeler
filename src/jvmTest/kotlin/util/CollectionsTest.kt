package util

import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getNullableOrElse
import com.sdercolin.vlabeler.util.getPreviousOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [getPreviousOrNull], [getNextOrNull] and [getNullableOrElse].
 */
class CollectionsTest {

    private val list = listOf("a", "b", "c")

    @Test
    fun testGetPreviousOrNullFound() {
        assertEquals("a", list.getPreviousOrNull { it == "b" })
        assertEquals("b", list.getPreviousOrNull { it == "c" })
    }

    @Test
    fun testGetPreviousOrNullOnFirstItem() {
        assertNull(list.getPreviousOrNull { it == "a" })
    }

    @Test
    fun testGetPreviousOrNullNotFound() {
        assertNull(list.getPreviousOrNull { it == "d" })
    }

    @Test
    fun testGetNextOrNullFound() {
        assertEquals("b", list.getNextOrNull { it == "a" })
        assertEquals("c", list.getNextOrNull { it == "b" })
    }

    @Test
    fun testGetNextOrNullOnLastItem() {
        assertNull(list.getNextOrNull { it == "c" })
    }

    @Test
    fun testGetNextOrNullNotFound() {
        assertNull(list.getNextOrNull { it == "d" })
    }

    @Test
    fun testGetNullableOrElseWithExistingValue() {
        val map = mapOf<String, Int?>("a" to 1, "b" to null)
        assertEquals(1, map.getNullableOrElse("a") { 100 })
    }

    @Test
    fun testGetNullableOrElseWithExistingNullValue() {
        val map = mapOf<String, Int?>("a" to 1, "b" to null)
        assertNull(map.getNullableOrElse("b") { 100 })
    }

    @Test
    fun testGetNullableOrElseWithMissingKey() {
        val map = mapOf<String, Int?>("a" to 1, "b" to null)
        assertEquals(100, map.getNullableOrElse("c") { 100 })
    }
}
