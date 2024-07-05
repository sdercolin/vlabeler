package strings

import com.sdercolin.vlabeler.ui.string.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [Language.find].
 */
class LanguageDetectTest {

    @Test
    fun testDetection() {
        val languageTags = listOf(
            "en", "en-US", "zh", "zh-CN", "zh-Hans", "zh-TW", "ja", "ja-JP-AAA", "ko", "ko-KR", "XXX",
        )
        val expected = listOf(
            Language.English,
            Language.English,
            Language.ChineseSimplified,
            Language.ChineseSimplified,
            Language.ChineseSimplified,
            Language.ChineseSimplified,
            Language.Japanese,
            Language.Japanese,
            Language.Korean,
            Language.Korean,
            null,
        )
        val actual = languageTags.map { Language.find(it) }
        assertEquals(expected, actual)
    }
}
