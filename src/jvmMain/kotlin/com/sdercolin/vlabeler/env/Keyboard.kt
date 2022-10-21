@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseScrollAction
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
    private var mouseClickActions: Map<KeySet, MouseClickAction> = MouseClickAction.getKeySets(keymaps).toMap()
    private var mouseScrollActions: Map<KeySet, MouseScrollAction> = MouseScrollAction.getKeySets(keymaps).toMap()

    private val _keyboardActionFlow = MutableSharedFlow<KeyAction>(replay = 0)
    val keyboardActionFlow = _keyboardActionFlow.asSharedFlow()

    private val _keyboardStateFlow = MutableStateFlow(getIdleState())
    val keyboardStateFlow = _keyboardStateFlow.asStateFlow()

    suspend fun updateKeymaps(keymaps: AppConf.Keymaps) {
        keyActions = KeyAction.getNonMenuKeySets(keymaps)
        mouseClickActions = MouseClickAction.getKeySets(keymaps).toMap()
        mouseScrollActions = MouseScrollAction.getKeySets(keymaps).toMap()
        _keyboardStateFlow.emit(getIdleState())
    }

    private fun getIdleState() = KeyboardState(null, mouseClickActions, mouseScrollActions)

    private suspend fun emitEvent(action: KeyAction) {
        _keyboardActionFlow.emit(action)
    }

    private suspend fun emitState(state: KeyboardState) {
        _keyboardStateFlow.emit(state)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val keySet = KeySet.fromKeyEvent(event)
        val caughtKeyAction = keyActions.firstOrNull { it.first == keySet }?.second

        coroutineScope.launch {
            emitState(KeyboardState(keySet, mouseClickActions, mouseScrollActions))
            caughtKeyAction?.let { emitEvent(it) }
        }

        // Avoid triggering focus on menu bar on Windows
        val shouldBlockEvent = keySet.mainKey == null && keySet.subKeys == setOf(Key.Alt)
        return caughtKeyAction != null && shouldBlockEvent
    }
}

@Immutable
data class KeyboardState(
    val keySet: KeySet?,
    val availableMouseClickActions: Map<KeySet, MouseClickAction>,
    val availableMouseScrollActions: Map<KeySet, MouseScrollAction>,
) {

    fun getEnabledMouseClickAction(pointerEvent: PointerEvent): MouseClickAction? {
        val mainKey = pointerEvent.toVirtualKey() ?: return null
        return availableMouseClickActions[KeySet(mainKey, keySet?.subKeys.orEmpty())]
            ?.takeIf { it.pointerEventType == pointerEvent.type }
    }

    fun getEnabledMouseScrollAction(pointerEvent: PointerEvent): MouseScrollAction? {
        if (pointerEvent.type != PointerEventType.Scroll) return null
        val mainKey = pointerEvent.toVirtualKey() ?: return null
        return availableMouseScrollActions[KeySet(mainKey, keySet?.subKeys.orEmpty())]
    }
}

fun KeyEvent.isReleased(key: ActualKey) = released && this.key == key
val KeyEvent.isNativeCtrlPressed get() = if (isMacOS) isMetaPressed else isCtrlPressed
val KeyEvent.isNativeMetaPressed get() = if (isMacOS) isCtrlPressed else isMetaPressed
val KeyEvent.released get() = type == KeyEventType.KeyUp
fun Modifier.onEnterKey(action: () -> Unit) = onKeyEvent {
    if (it.isReleased(ActualKey.Enter)) {
        action()
        true
    } else {
        false
    }
}
