package com.sdercolin.vlabeler.model.palette

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.ui.theme.DarkGray
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.rgbHexString
import kotlinx.serialization.Serializable

/**
 * Color palette definition model.
 *
 * @param name Name of the palette
 * @param items List of color items in the palette. Every item is a pair of color hex string and its weight. The weight
 *     is used to determine the size of the color step between this item and the previous item. There must be at least
 *     two items in the list. The first item must have a weight of 0.
 */
@Serializable
@Immutable
data class ColorPaletteDefinition(
    val name: String,
    val items: List<Item>,
) {

    @Serializable
    @Immutable
    data class Item(
        val color: String,
        val weight: Float,
    )

    fun validate() = this.also {
        require(items.size >= 2) { "Color palette must have at least two items." }
        require(items.first().weight == 0f) { "The first item of a color palette must have a weight of 0." }
    }

    companion object {
        private val presetsInCode = listOf(
            ColorPaletteDefinition(
                name = "Plain",
                items = listOf(
                    Item(White.copy(alpha = 0f).argbHexString, 0f),
                    Item(White.rgbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Reversed",
                items = listOf(
                    Item(White.rgbHexString, 0f),
                    Item(DarkGray.rgbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Foggy",
                items = listOf(
                    Item("#00C76504", 0f),
                    Item("#C76504", 5f),
                    Item(White.rgbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Snowy",
                items = listOf(
                    Item("#00163EAB", 0f),
                    Item("#163EAB", 5f),
                    Item(White.rgbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Dawn",
                items = listOf(
                    Item(Color.Black.rgbHexString, 0f),
                    Item("#020724", 2f),
                    Item("#0286AF", 4f),
                    Item("#BFCAB8", 2f),
                    Item("#E6AAAB", 1f),
                    Item(White.rgbHexString, 0.5f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Sunset",
                items = listOf(
                    Item(Color.Black.rgbHexString, 0f),
                    Item("#02063E", 4f),
                    Item("#F21E07", 5f),
                    Item("#EDED0C", 1f),
                    Item("#FCFEF0", 0.5f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Midnight",
                items = listOf(
                    Item(Color.Black.rgbHexString, 0f),
                    Item("#1F1F47", 3f),
                    Item("#C45A0F", 2f),
                    Item("#F9CA3A", 0.5f),
                    Item(White.rgbHexString, 0.5f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Rainbow",
                items = listOf(
                    Item(Color.Black.rgbHexString, 0f),
                    Item(Color.Blue.rgbHexString, 1f),
                    Item(Color.Cyan.rgbHexString, 1f),
                    Item(Color.Green.rgbHexString, 1f),
                    Item(Color.Yellow.rgbHexString, 1f),
                    Item(Color.Red.rgbHexString, 1f),
                ),
            ),
        )
        private val presetsInFile = listOf(
            "Inferno.json",
            "Magma.json",
            "Plasma.json",
            "Viridis.json",
        ).map { "palette/$it" }

        val presets by lazy {
            presetsInCode + presetsInFile.map { path ->
                useResource(path) { it.bufferedReader().readText() }.parseJson()
            }
        }
    }
}
