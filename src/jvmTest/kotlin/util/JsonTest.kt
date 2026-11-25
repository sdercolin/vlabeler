package util

import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [parseJson] and [stringifyJson].
 */
class JsonTest {

    @Serializable
    private data class Item(
        val name: String,
        val count: Int = 0,
        val enabled: Boolean = true,
    )

    @Test
    fun testParseJsonPrimitiveList() {
        assertEquals(listOf(1, 2, 3), "[1, 2, 3]".parseJson())
    }

    @Test
    fun testParseJsonMap() {
        assertEquals(mapOf("a" to "b", "c" to "d"), """{"a": "b", "c": "d"}""".parseJson())
    }

    @Test
    fun testParseJsonObject() {
        val parsed = """{"name": "entry", "count": 5, "enabled": false}""".parseJson<Item>()
        assertEquals(Item(name = "entry", count = 5, enabled = false), parsed)
    }

    @Test
    fun testParseJsonObjectWithDefaults() {
        val parsed = """{"name": "entry"}""".parseJson<Item>()
        assertEquals(Item(name = "entry", count = 0, enabled = true), parsed)
    }

    @Test
    fun testParseJsonIsLenient() {
        // The global instance is configured with `isLenient = true`,
        // so unquoted strings are accepted.
        val parsed = """{name: entry, count: 5}""".parseJson<Item>()
        assertEquals(Item(name = "entry", count = 5), parsed)
    }

    @Test
    fun testStringifyJsonRoundTrip() {
        val source = Item(name = "entry", count = 42, enabled = false)
        assertEquals(source, source.stringifyJson().parseJson())
    }

    @Test
    fun testStringifyJsonEncodesDefaultsAndPrettyPrints() {
        val expected =
            """
            {
                "name": "entry",
                "count": 0,
                "enabled": true
            }
            """.trimIndent()
        assertEquals(expected, Item(name = "entry").stringifyJson())
    }
}
