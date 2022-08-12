@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.key.KeySet
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

    private val actions: List<Pair<KeySet, KeyAction>> by lazy { KeyAction.getNonMenuKeySets() }

    private val _keyboardActionFlow = MutableSharedFlow<KeyAction>(replay = 0)
    val keyboardActionFlow = _keyboardActionFlow.asSharedFlow()

    private val _keyboardStateFlow = MutableStateFlow(KeyboardState())
    val keyboardStateFlow = _keyboardStateFlow.asStateFlow()

    private suspend fun emitEvent(action: KeyAction) {
        _keyboardActionFlow.emit(action)
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

        val caughtAction = actions.firstOrNull { it.first.shouldCatch(event) }?.second

        coroutineScope.launch {
            emitState()
            caughtAction?.let { emitEvent(it) }
        }
        return caughtAction != null
    }
}

@Immutable
data class KeyboardState(
    val isCtrlPressed: Boolean = false,
    val isShiftPressed: Boolean = false
)

fun KeyEvent.isReleased(key: Key) = released && this.key == key
val KeyEvent.released get() = type == KeyEventType.KeyUp
val KeyEvent.shouldIncreaseResolution get() = (key == Key.Minus || key == Key.NumPadSubtract) && released
val KeyEvent.shouldDecreaseResolution get() = (key == Key.Equals || key == Key.NumPadAdd) && released
