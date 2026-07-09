package ui

import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.ui.dialog.ColorPickerState
import com.sdercolin.vlabeler.util.rgbHexString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [com.sdercolin.vlabeler.ui.dialog.ColorPickerDialog] renders inside a separate
 * [androidx.compose.ui.window.DialogWindow] which cannot be driven through
 * [androidx.compose.ui.test.runComposeUiTest]. Its behavior lives in [ColorPickerState], exercised directly here.
 */
class ColorPickerDialogUiTest {

    @Test
    fun testHexInputUpdatesRgbColorAndFinishSubmits() {
        var submitted: Color? = null
        var called = false
        val state = ColorPickerState(
            initialColor = Color.Red,
            useAlpha = false,
            submit = { submitted = it; called = true },
        )
        state.updateColorByHex("#00FF00")
        assertEquals("#00FF00", state.colorHexString)
        state.finish()
        assertEquals(true, called)
        assertEquals("#00FF00", submitted?.rgbHexString)
    }

    @Test
    fun testHexInputKeepsAlphaWhenUseAlpha() {
        val state = ColorPickerState(initialColor = Color.Red, useAlpha = true, submit = {})
        state.updateColorByHex("#8000FF00")
        assertEquals("#8000FF00", state.colorHexString)
    }

    @Test
    fun testResetRestoresInitialColor() {
        val state = ColorPickerState(initialColor = Color.Red, useAlpha = false, submit = {})
        state.updateColorByHex("#00FF00")
        state.reset()
        assertEquals("#FF0000", state.colorHexString)
    }

    @Test
    fun testCancelSubmitsNull() {
        var submitted: Color? = Color.Red
        var called = false
        val state = ColorPickerState(
            initialColor = Color.Red,
            useAlpha = false,
            submit = { submitted = it; called = true },
        )
        state.cancel()
        assertEquals(true, called)
        assertNull(submitted)
    }

    @Test
    fun testUseAlphaFalseDropsInitialAlpha() {
        val state = ColorPickerState(initialColor = Color.Red.copy(alpha = 0.5f), useAlpha = false, submit = {})
        // dropping alpha yields the plain RGB hex string
        assertEquals("#FF0000", state.colorHexString)
        assertTrue(state.useAlpha.not())
    }
}
