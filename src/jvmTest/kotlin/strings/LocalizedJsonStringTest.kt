package strings

import com.sdercolin.vlabeler.exception.LocalizedStringDeserializedException
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [LocalizedJsonString].
 */
class LocalizedJsonStringTest {

    @Test
    fun testPlainString() {
        val plainString = "\"Hello World\""
        val parsed = plainString.parseJson<LocalizedJsonString>()

        val expected = LocalizedJsonString(mapOf(Language.default.code to plainString.trim('"')))
        assertEquals(expected, parsed)
        assertEquals(plainString, parsed.stringifyJson())
    }

    @Test
    fun testLocalized() {
        val localizedString = """
            {
                "en": "Hello World",
                "zh": "你好，世界"
            }
        """.trimIndent()
        val parsed = localizedString.parseJson<LocalizedJsonString>()

        val expected = LocalizedJsonString(
            mapOf(
                "en" to "Hello World",
                "zh" to "你好，世界",
            ),
        )
        assertEquals(expected, parsed)
        assertEquals(localizedString, parsed.stringifyJson())
        assertEquals("你好，世界", parsed.getCertain(Language.ChineseSimplified))
    }

    @Test
    fun testLocalizedStringMissingDefault() {
        val localizedStringMissingDefault = """
            {
                "zh": "你好，世界",
                "ja": "こんにちは世界",
                "ko": "안녕 세상"
            }
        """.trimIndent()

        assertThrows<LocalizedStringDeserializedException> {
            localizedStringMissingDefault.parseJson<LocalizedJsonString>()
        }
    }
}
