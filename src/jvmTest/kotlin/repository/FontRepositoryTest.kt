package repository

import androidx.compose.ui.text.font.FontFamily
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.util.parseJson
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [FontRepository].
 *
 * Note: [FontRepository.initialize] and [FontRepository.load] operate on the real application directory
 * (`AppDir/fonts`), which must not be touched by tests, so only the parts that are independent of that directory are
 * covered here: the fallback behaviors and the JSON format of custom font definition files.
 */
class FontRepositoryTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun testGetUnknownFontFamilyFallsBackToDefault() {
        assertEquals(FontFamily.Default, FontRepository.getFontFamily("nonexistent-font-family"))
    }

    @Test
    fun testHasUnknownFontFamilyReturnsFalse() {
        assertFalse(FontRepository.hasFontFamily("nonexistent-font-family"))
    }

    @Test
    fun testFontFamilyDefinitionParsingWithDefaults() {
        val json = """
            {
                "name": "My Font",
                "fonts": [
                    { "path": "my-font.ttf" }
                ]
            }
        """.trimIndent()

        val definition = json.parseJson<FontRepository.FontFamilyDefinition>()

        assertEquals("My Font", definition.name)
        assertEquals(1, definition.fonts.size)
        assertEquals("my-font.ttf", definition.fonts[0].path)
        assertNull(definition.fonts[0].weight)
        assertNull(definition.fonts[0].isItalic)
    }

    @Test
    fun testFontFamilyDefinitionParsingWithAllFields() {
        val json = """
            {
                "name": "My Font",
                "fonts": [
                    { "path": "my-font-bold-italic.otf", "weight": 700, "isItalic": true }
                ]
            }
        """.trimIndent()

        val definition = json.parseJson<FontRepository.FontFamilyDefinition>()

        val font = definition.fonts.single()
        assertEquals("my-font-bold-italic.otf", font.path)
        assertEquals(700, font.weight)
        assertEquals(true, font.isItalic)
    }

    @Test
    fun testBuiltInFontOptions() {
        assertEquals("Default", FontRepository.FontOption.BuiltIn.Default.name)
        assertEquals(FontFamily.Default, FontRepository.FontOption.BuiltIn.Default.fontFamily)
        assertEquals(FontFamily.SansSerif, FontRepository.FontOption.BuiltIn.SansSerif.fontFamily)
        assertEquals(FontFamily.Serif, FontRepository.FontOption.BuiltIn.Serif.fontFamily)
        assertEquals(FontFamily.Monospace, FontRepository.FontOption.BuiltIn.Monospace.fontFamily)
        assertEquals(FontFamily.Cursive, FontRepository.FontOption.BuiltIn.Cursive.fontFamily)
    }

    @Test
    fun testFontDirectoryIsUnderAppDir() {
        assertEquals("fonts", FontRepository.fontDirectory.name)
        assertTrue(FontRepository.fontDirectory.isAbsolute)
    }
}
