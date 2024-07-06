package strings

import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import kotlin.test.Test

/**
 * Tests for [stringCertain].
 */
class StringFormatTest {

    private val maxParamCount = 5

    @Test
    fun test() {
        Language.entries.forEach { language ->
            Strings.entries.forEach { key ->
                stringCertain(key, language, *Array(maxParamCount) { it })
            }
        }
    }
}
