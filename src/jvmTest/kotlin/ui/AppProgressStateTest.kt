package ui

import com.sdercolin.vlabeler.ui.AppProgressStateImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppProgressStateTest {

    @Test
    fun `initial state is not busy`() {
        val state = AppProgressStateImpl()
        assertFalse(state.isBusy)
    }

    @Test
    fun `showProgress sets busy`() {
        val state = AppProgressStateImpl()
        state.showProgress()
        assertTrue(state.isBusy)
    }

    @Test
    fun `hideProgress resets busy`() {
        val state = AppProgressStateImpl()
        state.showProgress()
        state.hideProgress()
        assertFalse(state.isBusy)
    }

    @Test
    fun `hideProgress without showProgress keeps not busy`() {
        val state = AppProgressStateImpl()
        state.hideProgress()
        assertFalse(state.isBusy)
    }
}
