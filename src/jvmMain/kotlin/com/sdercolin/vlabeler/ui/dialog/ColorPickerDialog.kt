@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.common.ColorHexInputBox
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.rgbHexString
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toHsv
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class ColorPickerArgs(
    val color: Color,
    val useAlpha: Boolean,
    val submit: (Color?) -> Unit,
)

private typealias Point = Pair<Float, Float>

private val Point.x get() = first
private val Point.y get() = second

class ColorPickerState(
    private val initialColor: Color,
    val useAlpha: Boolean,
    private val submit: (Color?) -> Unit,
) {
    var color: Color by mutableStateOf(initialColor.runIf(!useAlpha) { copy(alpha = 1f) })
    var baseColor: Color by mutableStateOf(color.toHsv().let { Color.hsv(it[0], it[1], 1f) })

    val colorHexString: String get() = if (useAlpha) color.argbHexString else color.rgbHexString

    // cursor coordinates in the color wheel (both x, y are -1 to 1, with 0 as the origin)
    private var coordinates: Point by mutableStateOf(color.toCoordinates())

    val valueSlider = SliderState(
        baseColor = baseColor,
        value = color.toHsv()[2],
        valueApplier = { value, color -> color.toHsv().let { Color.hsv(it[0], it[1], value) } },
        update = { updateColor() },
    )

    val alphaSlider = SliderState(
        baseColor = color.copy(alpha = 1f),
        value = color.alpha,
        valueApplier = { value, color -> color.copy(alpha = value) },
        update = { updateColor() },
    )

    private var isPressed = false

    class SliderState(
        baseColor: Color,
        value: Float,
        val valueApplier: (Float, Color) -> Color,
        private val update: () -> Unit,
    ) {

        var baseColor: Color by mutableStateOf(baseColor)

        var value: Float by mutableStateOf(value) // value range is 0 to 1

        private var isPressed = false

        fun getCurrentColor() = valueApplier(value, baseColor)

        fun onPointerPressEvent(event: PointerEvent, size: IntSize) {
            isPressed = true
            value = (event.changes[0].position.x / size.width).coerceIn(0f, 1f)
            update()
        }

        fun onPointerMoveEvent(event: PointerEvent, size: IntSize) {
            if (isPressed) {
                value = (event.changes[0].position.x / size.width).coerceIn(0f, 1f)
                update()
            }
        }

        fun onPointerReleaseEvent() {
            isPressed = false
        }
    }

    private fun Color.toCoordinates(): Point {
        val (h, s) = toHsv()
        val rad = h * PI / 180
        val x = s * cos(rad)
        val y = s * sin(rad)
        return Point(x.toFloat(), y.toFloat())
    }

    fun getOffset(size: Size): Offset {
        val (x, y) = coordinates
        val radius = size.minDimension / 2
        return Offset(x * radius + radius, y * radius + radius)
    }

    private fun Offset.toCoordinates(size: IntSize): Point {
        val radius = min(size.width, size.height) / 2
        val x = (x - radius) / radius
        val y = (y - radius) / radius
        val r = (x.pow(2) + y.pow(2)).pow(0.5f)
        if (r == 0f) return Point(0f, 0f)
        val actualR = r.coerceAtMost(1f)
        val actualX = x * actualR / r
        val actualY = y * actualR / r
        return Point(actualX, actualY)
    }

    private fun updateColor() {
        val x = coordinates.x
        val y = coordinates.y
        val h = (atan2(y, x) / PI * 180 + 360) % 360
        val s = (x * x + y * y).coerceAtMost(1f).toDouble().pow(0.5)
        baseColor = Color.hsv(h.toFloat(), s.toFloat(), 1f)
        valueSlider.baseColor = baseColor
        if (useAlpha) {
            alphaSlider.baseColor = valueSlider.getCurrentColor()
            color = alphaSlider.getCurrentColor()
        } else {
            color = valueSlider.getCurrentColor()
        }
    }

    fun finish() {
        submit(color)
    }

    fun updateColorByHex(hex: String) {
        applyColor(hex.toColor())
    }

    fun reset() {
        applyColor(initialColor)
    }

    private fun applyColor(color: Color) {
        this.color = color
        coordinates = color.toCoordinates()
        valueSlider.value = color.toHsv()[2]
        alphaSlider.value = color.alpha
        updateColor()
    }

    fun cancel() {
        submit(null)
    }

    fun onPointerPressEvent(event: PointerEvent, size: IntSize) {
        isPressed = true
        coordinates = event.changes[0].position.toCoordinates(size)
        updateColor()
    }

    fun onPointerMoveEvent(event: PointerEvent, size: IntSize) {
        if (isPressed) {
            coordinates = event.changes[0].position.toCoordinates(size)
            updateColor()
        }
    }

    fun onPointerReleaseEvent() {
        isPressed = false
    }
}

@Composable
fun ColorPickerDialog(
    appConf: AppConf,
    initialColor: Color,
    useAlpha: Boolean,
    submit: (Color?) -> Unit,
    state: ColorPickerState = remember(initialColor, useAlpha, submit) {
        ColorPickerState(initialColor, useAlpha, submit)
    },
) {
    DialogWindow(
        title = string(Strings.ColorPickerDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = state::cancel,
        state = rememberDialogState(width = 400.dp, height = 600.dp.runIf(!useAlpha) { minus(45.dp) }),
        resizable = false,
    ) {
        AppTheme(appConf.view) {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: ColorPickerState) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 45.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp),
        ) {
            Canvas(
                modifier = Modifier.size(300.dp)
                    .onPointerEvent(PointerEventType.Press) {
                        state.onPointerPressEvent(it, size)
                    }
                    .onPointerEvent(PointerEventType.Move) {
                        state.onPointerMoveEvent(it, size)
                    }
                    .onPointerEvent(PointerEventType.Release) {
                        state.onPointerReleaseEvent()
                    },
                onDraw = {
                    val maxRadius = this.size.width / 2
                    val offsetIntMax = 50
                    for (offsetInt in 0 until offsetIntMax) {
                        val offsetRatio = offsetInt.toFloat() / offsetIntMax
                        val radius = maxRadius * (1 - offsetRatio)
                        val brush = Brush.sweepGradient(
                            colors = (0..360 step 60).map {
                                Color.hsv(
                                    hue = it.toFloat(),
                                    saturation = 1 - offsetRatio,
                                    value = 1f,
                                )
                            },
                        )
                        drawCircle(
                            brush = brush,
                            radius = radius,
                        )
                    }

                    val cursorPosition = state.getOffset(this.size)
                    val shadowPosition = cursorPosition + Offset(4f, 4f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Black, Black50, Transparent),
                            center = shadowPosition,
                            radius = 28f,
                        ),
                        radius = 28f,
                        center = shadowPosition,
                    )
                    drawCircle(
                        color = White,
                        radius = 25f,
                        center = cursorPosition,
                    )
                    drawCircle(
                        color = state.baseColor,
                        radius = 16f,
                        center = cursorPosition,
                    )
                },
            )

            Slider(state.valueSlider)

            if (state.useAlpha) {
                Slider(
                    state.alphaSlider,
                    backgroundBox = {
                        Image(
                            modifier = Modifier.fillMaxWidth()
                                .height(20.dp)
                                .padding(horizontal = 5.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            painter = painterResource(Resources.transparencyGridPng),
                            contentDescription = null,
                        )
                    },
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(10.dp))
                ColorHexInputBox(
                    value = state.colorHexString,
                    defaultValue = state.colorHexString,
                    onValidValue = state::updateColorByHex,
                    useAlpha = state.useAlpha,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = state::reset) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = state::finish) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Slider(state: ColorPickerState.SliderState, backgroundBox: (@Composable () -> Unit)? = null) {
    val brush = remember(state.baseColor) {
        Brush.linearGradient(
            colors = listOf(state.valueApplier(0f, state.baseColor), state.baseColor),
        )
    }
    val totalWidthDp = 300.dp
    Box(
        modifier = Modifier.width(totalWidthDp)
            .onPointerEvent(PointerEventType.Press) {
                state.onPointerPressEvent(it, size)
            }
            .onPointerEvent(PointerEventType.Move) {
                state.onPointerMoveEvent(it, size)
            }
            .onPointerEvent(PointerEventType.Release) {
                state.onPointerReleaseEvent()
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        backgroundBox?.invoke()
        val density = LocalDensity.current
        val cornerRadius = 5.dp
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val capWidth = 10.dp
        val capWidthPx = with(density) { capWidth.toPx() }
        val capHeight = 25.dp
        val capHeightPx = with(density) { capHeight.toPx() }
        Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                with(drawContext.canvas.nativeCanvas) {
                    val layerSaved = saveLayer(null, null)
                    drawRoundRect(
                        brush = brush,
                        size = size.copy(width = size.width - capWidthPx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        topLeft = Offset(capWidthPx / 2, 0f),
                    )
                    val capSize = Size(capWidthPx, capHeightPx)
                    val capCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    val capOffset = Offset(
                        (size.width - capWidthPx).times(state.value),
                        (size.height - capHeightPx) / 2,
                    )
                    drawRoundRect(
                        color = state.getCurrentColor(),
                        size = capSize,
                        cornerRadius = capCornerRadius,
                        topLeft = capOffset,
                        blendMode = BlendMode.Src,
                    )
                    drawRoundRect(
                        color = LightGray,
                        size = capSize,
                        cornerRadius = capCornerRadius,
                        topLeft = capOffset,
                        style = Stroke(width = 5f),
                    )
                    restoreToCount(layerSaved)
                }
            }
        }
    }
}
