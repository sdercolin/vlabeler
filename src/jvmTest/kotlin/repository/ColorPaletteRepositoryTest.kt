package repository

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.palette.ColorPaletteDefinition
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ColorPaletteRepository] and the palette definitions it serves.
 *
 * Note: [ColorPaletteRepository.initialize] and [ColorPaletteRepository.load] operate on the real application
 * directory (`AppDir/color_palettes`), which must not be touched by tests, so only the parts that are independent of
 * that directory are covered here: the fallback behaviors and the bundled palette definitions including the JSON
 * format used for custom palette files.
 */
class ColorPaletteRepositoryTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun testGetUnknownNameFallsBackToFirstPreset() {
        assertEquals(
            ColorPaletteDefinition.presets.first(),
            ColorPaletteRepository.get("nonexistent-palette-name"),
        )
    }

    @Test
    fun testHasUnknownNameReturnsFalse() {
        assertFalse(ColorPaletteRepository.has("nonexistent-palette-name"))
    }

    @Test
    fun testPresetsAreValidAndUnique() {
        val presets = ColorPaletteDefinition.presets
        presets.forEach { it.validate() }
        assertEquals(presets.size, presets.map { it.name }.distinct().size)
        // bundled in code
        assertTrue(presets.any { it.name == "Plain" })
        // bundled as resource files
        listOf("Inferno", "Magma", "Plasma", "Viridis").forEach { name ->
            assertTrue(presets.any { it.name == name }, "Missing bundled palette: $name")
        }
    }

    @Test
    fun testPaletteDefinitionJsonRoundTrip() {
        // the same format is used for custom palette files loaded by ColorPaletteRepository.load
        ColorPaletteDefinition.presets.forEach { preset ->
            assertEquals(preset, preset.stringifyJson().parseJson<ColorPaletteDefinition>())
        }
    }

    @Test
    fun testCustomPaletteFileFormatParsing() {
        val json = """
            {
                "name": "Custom",
                "items": [
                    { "color": "#00FFFFFF", "weight": 0.0 },
                    { "color": "#FF0000", "weight": 1.0 }
                ]
            }
        """.trimIndent()

        val definition = json.parseJson<ColorPaletteDefinition>().validate()

        assertEquals("Custom", definition.name)
        assertEquals(
            listOf(
                ColorPaletteDefinition.Item("#00FFFFFF", 0f),
                ColorPaletteDefinition.Item("#FF0000", 1f),
            ),
            definition.items,
        )
    }

    @Test
    fun testValidateRejectsInvalidPalettes() {
        assertFailsWith<IllegalArgumentException> {
            ColorPaletteDefinition(
                name = "TooFewItems",
                items = listOf(ColorPaletteDefinition.Item("#FFFFFF", 0f)),
            ).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            ColorPaletteDefinition(
                name = "NonZeroFirstWeight",
                items = listOf(
                    ColorPaletteDefinition.Item("#FFFFFF", 1f),
                    ColorPaletteDefinition.Item("#000000", 1f),
                ),
            ).validate()
        }
    }
}
