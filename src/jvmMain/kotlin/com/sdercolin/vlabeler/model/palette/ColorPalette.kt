package com.sdercolin.vlabeler.model.palette

import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.util.toColor
import kotlin.math.roundToInt

/**
 * Helper class to convert intensity values to color
 *
 * @param definition Color palette definition. See [ColorPaletteDefinition] for more details.
 */
class ColorPalette(definition: ColorPaletteDefinition) {

    private val keyColors = definition.items.map { it.color.toColor() }
    private val stepWeights = definition.items.map { it.weight }

    private val colors = keyColors
        .foldIndexed(listOf<Color>()) { index, acc, color ->
            if (acc.isEmpty()) {
                acc + color
            } else {
                val stepWeight = stepWeights[index]
                val lastColor = acc.last()
                val size = INTERPOLATION_STANDARD_SIZE
                val lastR = lastColor.red * size
                val lastG = lastColor.green * size
                val lastB = lastColor.blue * size
                val lastA = lastColor.alpha * size
                val r = color.red * size
                val g = color.green * size
                val b = color.blue * size
                val a = color.alpha * size
                val stepSize = (size * stepWeight).roundToInt()
                if (stepSize > 0) {
                    val rStep = (r - lastR) / stepSize
                    val gStep = (g - lastG) / stepSize
                    val bStep = (b - lastB) / stepSize
                    val aStep = (a - lastA) / stepSize
                    acc + (1..stepSize).map {
                        Color(
                            (lastR + it * rStep).roundToInt(),
                            (lastG + it * gStep).roundToInt(),
                            (lastB + it * bStep).roundToInt(),
                            (lastA + it * aStep).roundToInt(),
                        )
                    }
                } else {
                    acc
                }
            }
        }

    /**
     * Returns the color at the given intensity
     *
     * @param intensity the intensity of the color. Should be in the range [0, 1]
     */
    fun get(intensity: Float): Color {
        val index = (intensity * (colors.size - 1)).roundToInt()
        return colors[index]
    }

    companion object {
        private const val INTERPOLATION_STANDARD_SIZE = 255
    }
}

fun ColorPaletteDefinition.create() = ColorPalette(this)
