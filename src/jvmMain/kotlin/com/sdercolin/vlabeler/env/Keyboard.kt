package com.sdercolin.vlabeler.env

import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.util.update

@OptIn(ExperimentalComposeUiApi::class)
class KeyEventHandler(private val player: Player, private val state: MutableState<KeyboardState>) {
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.key == Key.Spacebar && event.type == KeyEventType.KeyUp) {
            player.toggle()
            return true
        }
        val isLeftCtrl = if (isMacOs) event.key == Key.MetaLeft else event.key == Key.CtrlLeft
        if (isLeftCtrl) {
            if (event.type == KeyEventType.KeyUp) {
                state.update { copy(isLeftCtrlPressed = false) }
                return true
            } else if (event.type == KeyEventType.KeyDown) {
                state.update { copy(isLeftCtrlPressed = true) }
                return true
            }
        }
        val isRightCtrl = if (isMacOs) event.key == Key.MetaRight else event.key == Key.CtrlRight
        if (isRightCtrl) {
            if (event.type == KeyEventType.KeyUp) {
                state.update { copy(isRightCtrlPressed = false) }
                return true
            } else if (event.type == KeyEventType.KeyDown) {
                state.update { copy(isRightCtrlPressed = true) }
                return true
            }
        }
        if (event.key == Key.ShiftLeft) {
            if (event.type == KeyEventType.KeyUp) {
                state.update { copy(isLeftShiftPressed = false) }
                return true
            } else if (event.type == KeyEventType.KeyDown) {
                state.update { copy(isLeftShiftPressed = true) }
                return true
            }
        }
        if (event.key == Key.ShiftRight) {
            if (event.type == KeyEventType.KeyUp) {
                state.update { copy(isRightShiftPressed = false) }
                return true
            } else if (event.type == KeyEventType.KeyDown) {
                state.update { copy(isRightShiftPressed = true) }
                return true
            }
        }
        return false
    }
}

data class KeyboardState(
    val isLeftCtrlPressed: Boolean = false,
    val isRightCtrlPressed: Boolean = false,
    val isLeftShiftPressed: Boolean = false,
    val isRightShiftPressed: Boolean = false
) {
    val isCtrlPressed get() = isLeftCtrlPressed || isRightCtrlPressed
    val isShiftPressed get() = isLeftShiftPressed || isRightShiftPressed
}