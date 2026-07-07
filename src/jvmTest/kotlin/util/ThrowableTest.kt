package util

import com.sdercolin.vlabeler.exception.LocalizedException
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import com.sdercolin.vlabeler.util.getLocalizedMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [getLocalizedMessage].
 */
class ThrowableTest {

    private class TestLocalizedException(cause: Throwable? = null) :
        LocalizedException(Strings.CommonOkay, cause)

    @Test
    fun testPlainThrowable() {
        val throwable = Exception("boom")
        assertEquals(throwable.toString(), throwable.getLocalizedMessage(Language.English))
    }

    @Test
    fun testPlainThrowableWithPlainCause() {
        // A non-localized cause is not appended
        val throwable = Exception("boom", Exception("cause"))
        assertEquals(throwable.toString(), throwable.getLocalizedMessage(Language.English))
    }

    @Test
    fun testLocalizedException() {
        val throwable = TestLocalizedException()
        val expected = stringCertain(Strings.CommonOkay, Language.English)
        assertEquals(expected, throwable.getLocalizedMessage(Language.English))
    }

    @Test
    fun testPlainThrowableWithLocalizedCause() {
        val cause = TestLocalizedException()
        val throwable = Exception("boom", cause)
        val expected = "$throwable\n\nCaused by:\n${stringCertain(Strings.CommonOkay, Language.English)}"
        assertEquals(expected, throwable.getLocalizedMessage(Language.English))
    }
}
