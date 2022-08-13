@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.key.ActualKey
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Stable
class KeyboardViewModel(private val coroutineScope: CoroutineScope, keymaps: AppConf.Keymaps) {

    private var keyActions: List<Pair<KeySet, KeyAction>> = KeyAction.getNonMenuKeySets(keymaps)
    private var mouseClickActions: List<Pair<KeySet, MouseClickAction>> = MouseClickAction.getKeySets(keymaps)

    private val _keyboardActionFlow = MutableSharedFlow<KeyAction>(replay = 0)
    val keyboardActionFlow = _keyboardActionFlow.asSharedFlow()

    private val _keyboardStateFlow = MutableStateFlow(getIdleState())
    val keyboardStateFlow = _keyboardStateFlow.asStateFlow()

    suspend fun updateKeymaps(keymaps: AppConf.Keymaps) {
        keyActions = KeyAction.getNonMenuKeySets(keymaps)
        mouseClickActions = MouseClickAction.getKeySets(keymaps)
        _keyboardStateFlow.emit(getIdleState())
    }

    private fun getIdleState() = KeyboardState(
        keySet = null,
        enabledMouseClickAction = mouseClickActions.find { it.first.needNoKeys() }?.second
    )

    private suspend fun emitEvent(action: KeyAction) {
        _keyboardActionFlow.emit(action)
    }

    private suspend fun emitState(state: KeyboardState) {
        _keyboardStateFlow.emit(state)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val keySet = KeySet.fromKeyEvent(event)
        val caughtKeyAction = keyActions.firstOrNull { it.first.shouldCatch(event, true) }?.second
        val mouseClickAction = mouseClickActions.firstOrNull { it.first.shouldCatch(event, false) }?.second

        coroutineScope.launch {
            emitState(KeyboardState(keySet, mouseClickAction))
            caughtKeyAction?.let { emitEvent(it) }
        }
        return caughtKeyAction != null
    }
}

@Immutable
data class KeyboardState(
    val keySet: KeySet?,
    val enabledMouseClickAction: MouseClickAction?
) {

    val isCtrlPressed get() = keySet?.subKeys?.contains(Key.Ctrl) == true
    val isShiftPressed get() = keySet?.subKeys?.contains(Key.Shift) == true
}

fun KeyEvent.isReleased(key: ActualKey) = released && this.key == key
val KeyEvent.isNativeCtrlPressed get() = if (isMacOS) isMetaPressed else isCtrlPressed
val KeyEvent.isNativeMetaPressed get() = if (isMacOS) isCtrlPressed else isMetaPressed
val KeyEvent.released get() = type == KeyEventType.KeyUp
