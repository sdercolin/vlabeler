@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Stable
class KeyboardViewModel(private val coroutineScope: CoroutineScope) {
    private var isLeftCtrlPressed: Boolean = false
    private var isRightCtrlPressed: Boolean = false
    private var isLeftShiftPressed: Boolean = false
    private var isRightShiftPressed: Boolean = false
    private val isCtrlPressed get() = isLeftCtrlPressed || isRightCtrlPressed
    private val isShiftPressed get() = isLeftShiftPressed || isRightShiftPressed

    private val _keyboardEventFlow = MutableSharedFlow<KeyEvent>(replay = 0)
    val keyboardEventFlow = _keyboardEventFlow.asSharedFlow()

    private val _keyboardStateFlow = MutableStateFlow(KeyboardState())
    val keyboardStateFlow = _keyboardStateFlow.asStateFlow()

    private suspend fun emitEvent(event: KeyEvent) {
        _keyboardEventFlow.emit(event)
    }

    private suspend fun emitState() {
        val state = KeyboardState(isCtrlPressed, isShiftPressed)
        _keyboardStateFlow.emit(state)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val isLeftCtrl = if (isMacOS) event.key == Key.MetaLeft else event.key == Key.CtrlLeft
        if (isLeftCtrl) {
            if (event.type == KeyEventType.KeyUp) {
                isLeftCtrlPressed = false
            } else if (event.type == KeyEventType.KeyDown) {
                isLeftCtrlPressed = true
            }
        }
        val isRightCtrl = if (isMacOS) event.key == Key.MetaRight else event.key == Key.CtrlRight
        if (isRightCtrl) {
            if (event.type == KeyEventType.KeyUp) {
                isRightCtrlPressed = false
            } else if (event.type == KeyEventType.KeyDown) {
                isRightCtrlPressed = true
            }
        }
        if (event.key == Key.ShiftLeft) {
            if (event.type == KeyEventType.KeyUp) {
                isLeftShiftPressed = false
            } else if (event.type == KeyEventType.KeyDown) {
                isLeftShiftPressed = true
            }
        }
        if (event.key == Key.ShiftRight) {
            if (event.type == KeyEventType.KeyUp) {
                isRightShiftPressed = false
            } else if (event.type == KeyEventType.KeyDown) {
                isRightShiftPressed = true
            }
        }
        val eventCaught = event.shouldBeCaught

        coroutineScope.launch {
            emitState()
            if (eventCaught) emitEvent(event)
        }
        return eventCaught
    }
}

@Immutable
data class KeyboardState(
    val isCtrlPressed: Boolean = false,
    val isShiftPressed: Boolean = false
)

fun KeyEvent.isReleased(key: Key) = released && this.key == key
val KeyEvent.released get() = type == KeyEventType.KeyUp
val KeyEvent.shouldTogglePlayer get() = key == Key.Spacebar && released
val KeyEvent.shouldTogglePlayerWithInCurrentEntry get() = shouldTogglePlayer && !isShiftPressed
val KeyEvent.shouldIncreaseResolution get() = (key == Key.Minus || key == Key.NumPadSubtract) && released
val KeyEvent.shouldDecreaseResolution get() = (key == Key.Equals || key == Key.NumPadAdd) && released
val KeyEvent.shouldBeCaught
    get() = shouldTogglePlayer || shouldIncreaseResolution || shouldDecreaseResolution

fun getNumberKey(number: Int) =
    when (number) {
        0 -> Key.Zero
        1 -> Key.One
        2 -> Key.Two
        3 -> Key.Three
        4 -> Key.Four
        5 -> Key.Five
        6 -> Key.Six
        7 -> Key.Seven
        8 -> Key.Eight
        9 -> Key.Nine
        else -> throw IllegalArgumentException("Cannot get number key for number $number")
    }
