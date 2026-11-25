package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Test
import testutil.TestLabelers
import kotlin.test.assertEquals

/**
 * Tests for [MarkerState.getPointIndexForHovering] and [MarkerState.onLabelHovered].
 *
 * All states use sampleRate = 1000 Hz / resolution = 1 (1 ms == 1 px, canvas 1000 px). The hover radius is 20 px for
 * start/end/borders and 5 px for field points. [labelSize] is zero so the label-rectangle hit test never matches and
 * only the line hit test is exercised.
 */
class MarkerStateHoverTest {

    private val density = Density(1f)
    private val labelSize = DpSize(0.dp, 0.dp)
    private val labelShiftUp = 0.dp
    private val canvasHeight = 1000f
    private val waveformsHeightRatio = 0.8f

    private fun MarkerState.hover(x: Float, y: Float) = getPointIndexForHovering(
        x = x,
        y = y,
        conf = labelerConf,
        canvasHeight = canvasHeight,
        waveformsHeightRatio = waveformsHeightRatio,
        density = density,
        labelSize = labelSize,
        labelShiftUp = labelShiftUp,
    )

    /**
     * Continuous entries at 100..200, 200..350, 350..500: start = 100, borders = [200, 350], end = 500.
     */
    private fun continuousState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
            MarkerStateFactory.entry(350f, 500f, name = "c"),
        ),
    )

    /**
     * Utau-singer single entry: implicit start = 100 ("left"), fixed = 400, preu = 250, ovl = 150, end = 600.
     */
    private fun utauSingerState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.utauSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f)),
        ),
    )

    @Test
    fun hoveringNearEndReturnsEndPointIndex() {
        val state = continuousState()
        // right of end
        assertEquals(MarkerCursorState.END_POINT_INDEX, state.hover(505f, 500f))
        // left of end, but still closer to the end than to the last border (350)
        assertEquals(MarkerCursorState.END_POINT_INDEX, state.hover(495f, 500f))
    }

    @Test
    fun hoveringNearStartReturnsStartPointIndex() {
        val state = continuousState()
        // left of start
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.hover(95f, 500f))
        // right of start, but still closer to the start than to the first border (200)
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.hover(110f, 500f))
    }

    @Test
    fun hoveringNearStartOfImplicitStartLabelerReturnsStartPointIndex() {
        val state = utauSingerState()
        // the actual start is the "left" point at 100
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.hover(99f, 500f))
    }

    @Test
    fun hoveringNearBorderReturnsBorderIndex() {
        val state = continuousState()
        // border 0 is at 200 with a radius of 20 px
        assertEquals(0, state.hover(205f, 500f))
        assertEquals(0, state.hover(195f, 500f))
    }

    @Test
    fun hoveringFarFromAnyPointReturnsNone() {
        val state = continuousState()
        assertEquals(MarkerCursorState.NONE_POINT_INDEX, state.hover(275f, 500f))
    }

    @Test
    fun hoveringIsDisabledWhileALabelIsHovered() {
        val state = continuousState()
        state.onLabelHovered(0, true)
        assertEquals(MarkerCursorState.NONE_POINT_INDEX, state.hover(205f, 500f))
        state.onLabelHovered(0, false)
        assertEquals(0, state.hover(205f, 500f))
    }

    @Test
    fun hoveringFieldPointLineBelowItsTopReturnsFieldIndex() {
        val state = utauSingerState()
        // "fixed" (index 0) is at 400 with a radius of 5 px and height 0.5:
        // its line top is canvasHeight * ratio * (1 - 0.5) = 400, so y = 800 hits the line
        assertEquals(0, state.hover(403f, 800f))
    }

    @Test
    fun hoveringFieldPointAboveItsLineTopReturnsNone() {
        val state = utauSingerState()
        // same x as above, but y = 100 is above the line top (400)
        assertEquals(MarkerCursorState.NONE_POINT_INDEX, state.hover(403f, 100f))
    }

    @Test
    fun hoveringNearStartButCloserToNextPointReturnsStart() {
        val state = MarkerStateFactory.create(
            labelerConf = TestLabelers.nnsvsSinger,
            allEntries = listOf(
                MarkerStateFactory.entry(100f, 110f, name = "a"),
                MarkerStateFactory.entry(110f, 500f, name = "b"),
            ),
        )
        // x = 108 is within 20 px of the start (100) but closer to the border (110); the line hit test skips the
        // border because the position is also within the start radius and left of the border, so the fallback
        // start check applies
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.hover(108f, 500f))
    }
}
