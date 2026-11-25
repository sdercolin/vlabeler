package com.sdercolin.vlabeler.ui.editor.labeler.timescale

import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [Timescale.find] and the non-composable [TimescaleBarState].
 *
 * [Timescale.find] returns the first item (ordered from the smallest) whose minor step is at least 80 px wide under
 * the given time-to-pixel conversion, falling back to the largest item (major = 1 h, minor = 30 min).
 *
 * [TimescaleBarState] lays out the ticks of the selected timescale on the screen: `position = index * stepPx -
 * screenRange.start`, where major ticks carry a label produced by `getTimeText(index * major)` and minor ticks carry
 * `null`. A minor tick that coincides with a major tick is merged into the major one.
 */
class TimescaleTest {

    // region Timescale.find

    @Test
    fun findReturnsSmallestItemWhenExtremelyZoomedIn() {
        // 1 ms = 1000 px: even the smallest minor step (0.1 ms -> 100 px) is wide enough
        val item = Timescale.find { it * 1000 }
        assertEquals(0.5, item.major)
        assertEquals(0.1, item.minor)
    }

    @Test
    fun findAtOneMillisecondPerPixel() {
        // minor 50 ms -> 50 px is too narrow; minor 100 ms -> 100 px is the first that fits
        val item = Timescale.find { it }
        assertEquals(500.0, item.major)
        assertEquals(100.0, item.minor)
    }

    @Test
    fun findAcceptsMinorStepOfExactlyMinimumWidth() {
        // minor 100 ms -> exactly 80 px, which satisfies the ">= 80" requirement
        val item = Timescale.find { it * 0.8f }
        assertEquals(500.0, item.major)
        assertEquals(100.0, item.minor)
    }

    @Test
    fun findSkipsMinorStepJustBelowMinimumWidth() {
        // minor 100 ms -> 79 px is rejected; the next item (minor 500 ms -> 395 px) is selected
        val item = Timescale.find { it * 0.79f }
        assertEquals(1000.0, item.major)
        assertEquals(500.0, item.minor)
    }

    @Test
    fun findReturnsLastItemWhenExtremelyZoomedOut() {
        // even the largest minor step (30 min -> 18 px) is too narrow, so the last item is used as fallback
        val item = Timescale.find { it / 100000 }
        assertEquals(3600000.0, item.major)
        assertEquals(1800000.0, item.minor)
    }

    @Test
    fun findWithEntryConverterSelectsItemByResolution() {
        // 44100 Hz / resolution 100: 1 ms -> 0.441 px;
        // minor 100 ms -> 44.1 px is too narrow, minor 500 ms -> 220.5 px fits
        val converter = EntryConverter(44100f, 100)
        val item = Timescale.find { converter.convertToPixel(it) }
        assertEquals(1000.0, item.major)
        assertEquals(500.0, item.minor)
    }

    // endregion

    // region TimescaleBarState

    @Test
    fun barStateAtOneMillisecondPerPixelWithoutOffset() {
        // 1000 Hz / resolution 1: 1 ms == 1 px, so the timescale is (major 500 ms, minor 100 ms)
        val state = TimescaleBarState(sampleRate = 1000f, resolution = 1, screenRange = 0f..1000f)
        assertEquals(
            listOf(
                0f to "0",
                100f to null,
                200f to null,
                300f to null,
                400f to null,
                500f to "0.5",
                600f to null,
                700f to null,
                800f to null,
                900f to null,
                1000f to "1",
            ),
            state.scalePositions,
        )
        assertEquals(
            listOf(
                0f to "0",
                500f to "0.5",
                1000f to "1",
            ),
            state.scalePositionsWithTexts,
        )
    }

    @Test
    fun barStateAppliesScrollOffset() {
        // same scale as above, but scrolled to 250..1250 px:
        // major ticks at absolute 500 and 1000 px appear at 250 and 750 px on screen
        val state = TimescaleBarState(sampleRate = 1000f, resolution = 1, screenRange = 250f..1250f)
        assertEquals(
            listOf(
                50f to null,
                150f to null,
                250f to "0.5",
                350f to null,
                450f to null,
                550f to null,
                650f to null,
                750f to "1",
                850f to null,
                950f to null,
            ),
            state.scalePositions,
        )
        assertEquals(
            listOf(
                250f to "0.5",
                750f to "1",
            ),
            state.scalePositionsWithTexts,
        )
    }

    @Test
    fun barStateFormatsMinuteScaleLabels() {
        // 1000 Hz / resolution 500: 1 ms == 0.002 px;
        // minor 30 s -> 60 px is too narrow, minor 1 min -> 120 px fits (major 5 min -> 600 px)
        val state = TimescaleBarState(sampleRate = 1000f, resolution = 500, screenRange = 0f..1300f)
        assertEquals(
            listOf(
                0f to "0",
                120f to null,
                240f to null,
                360f to null,
                480f to null,
                600f to "5:00",
                720f to null,
                840f to null,
                960f to null,
                1080f to null,
                1200f to "10:00",
            ),
            state.scalePositions,
        )
        assertEquals(
            listOf(
                0f to "0",
                600f to "5:00",
                1200f to "10:00",
            ),
            state.scalePositionsWithTexts,
        )
    }

    @Test
    fun barStateFormatsHourScaleLabelsUsingFallbackItem() {
        // 1000 Hz / resolution 25000: 1 ms == 0.00004 px; no minor step reaches 80 px, so the fallback item
        // (major 1 h -> 144 px, minor 30 min -> 72 px) is used
        val state = TimescaleBarState(sampleRate = 1000f, resolution = 25000, screenRange = 0f..300f)
        assertEquals(
            listOf(
                0f to "0",
                72f to null,
                144f to "1:00:00",
                216f to null,
                288f to "2:00:00",
            ),
            state.scalePositions,
        )
        assertEquals(
            listOf(
                0f to "0",
                144f to "1:00:00",
                288f to "2:00:00",
            ),
            state.scalePositionsWithTexts,
        )
    }

    // endregion
}
