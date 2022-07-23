package com.sdercolin.vlabeler.ui.editor

import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.ui.theme.DarkGray
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.toColor
import kotlin.math.roundToInt

/**
 * Helper class to convert intensity values to color
 * @param keyColors List of colors to use for as the key frames in the interpolation except for the darkest one.
 * @param stepWeights List of weights between key frames in the interpolation.
 *                    The size of the list must be equal to the size of the keyColors list.
 */
class SpectrogramColorPalette(keyColors: List<Color>, stepWeights: List<Float>) {

    private val colors = keyColors
        .let { listOf(it.first().copy(alpha = 0f)) + it }
        .foldIndexed(listOf<Color>()) { index, acc, color ->
            if (acc.isEmpty()) {
                acc + color
            } else {
                val stepWeight = stepWeights[index - 1]
                val lastColor = acc.last()
                val lastR = lastColor.red * InterpolationStandardSize
                val lastG = lastColor.green * InterpolationStandardSize
                val lastB = lastColor.blue * InterpolationStandardSize
                val lastA = lastColor.alpha * InterpolationStandardSize
                val r = color.red * InterpolationStandardSize
                val g = color.green * InterpolationStandardSize
                val b = color.blue * InterpolationStandardSize
                val a = color.alpha * InterpolationStandardSize
                val stepSize = (InterpolationStandardSize * stepWeight).roundToInt()
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
                            (lastA + it * aStep).roundToInt()
                        )
                    }
                } else {
                    acc.dropLast(1) + color
                }
            }
        }

    /**
     * Returns the color at the given intensity
     * @param intensity the intensity of the color. Should be in the range [0, 1]
     */
    fun get(intensity: Float): Color {
        val index = (intensity * (colors.size - 1)).roundToInt()
        return colors[index]
    }

    enum class Presets(private val keyColors: List<Color>, private val stepWeights: List<Float>) {
        Plain(listOf(White), listOf(1f)),
        Reversed(listOf(White, DarkGray), listOf(0f, 1f)),
        Foggy(
            listOf("#c76504".toColor(), White),
            listOf(5f, 1f)
        ),
        Snowy(
            listOf("#163eab".toColor(), White),
            listOf(5f, 1f)
        ),
        Dawn(
            listOf(
                Color.Black,
                "#020724".toColor(),
                "#0286af".toColor(),
                "#bfcab8".toColor(),
                "#e6aaab".toColor(),
                Color.White
            ),
            listOf(0f, 2f, 4f, 2f, 1f, 0.5f)
        ),
        Sunset(
            listOf(
                Color.Black,
                "#02063e".toColor(),
                "#f21e07".toColor(),
                "#eded0c".toColor(),
                "#fcfef0".toColor()
            ),
            listOf(0f, 4f, 5f, 1f, 0.5f)
        ),
        Midnight(
            listOf(
                Color.Black,
                "#1f1f47".toColor(),
                "#c45a0f".toColor(),
                "#f9ca3a".toColor(),
                White
            ),
            listOf(0f, 3f, 2f, 0.5f, 0.5f)
        );

        fun create() = SpectrogramColorPalette(keyColors, stepWeights)
    }

    companion object {
        private const val InterpolationStandardSize = 255
    }
}
