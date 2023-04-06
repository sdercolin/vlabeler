package com.sdercolin.vlabeler.model.palette

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.ui.theme.DarkGray
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.argbHexString
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
        val presets = listOf(
            ColorPaletteDefinition(
                name = "Plain",
                items = listOf(
                    Item(White.copy(alpha = 0f).argbHexString, 0f),
                    Item(White.argbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Reversed",
                items = listOf(
                    Item(White.argbHexString, 0f),
                    Item(DarkGray.argbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Foggy",
                items = listOf(
                    Item("#00C76504", 0f),
                    Item("#FFC76504", 5f),
                    Item(White.argbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Snowy",
                items = listOf(
                    Item("#00163EAB", 0f),
                    Item("#FF163EAB", 5f),
                    Item(White.argbHexString, 1f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Dawn",
                items = listOf(
                    Item(Color.Black.argbHexString, 0f),
                    Item("#FF020724", 2f),
                    Item("#FF0286AF", 4f),
                    Item("#FFBFCAB8", 2f),
                    Item("#FFE6AAAB", 1f),
                    Item(White.argbHexString, 0.5f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Sunset",
                items = listOf(
                    Item(Color.Black.argbHexString, 0f),
                    Item("#FF02063E", 4f),
                    Item("#FFF21E07", 5f),
                    Item("#FFEDED0C", 1f),
                    Item("#FFFCFEF0", 0.5f),
                ),
            ),
            ColorPaletteDefinition(
                name = "Midnight",
                items = listOf(
                    Item(Color.Black.argbHexString, 0f),
                    Item("#FF1F1F47", 3f),
                    Item("#FFC45A0F", 2f),
                    Item("#FFF9CA3A", 0.5f),
                    Item(White.argbHexString, 0.5f),
                ),
            ),
        )
    }
}
