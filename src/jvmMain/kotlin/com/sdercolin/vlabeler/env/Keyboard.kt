package com.sdercolin.vlabeler.env

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
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
import com.sdercolin.vlabeler.ui.editor.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This class handles the overall keyboard inputs and expose flows of [KeyboardState] and [KeyAction] to the UI.
 */
@Stable
class KeyboardViewModel(private val coroutineScope: CoroutineScope, keymaps: AppConf.Keymaps) {

    private var keyActions: List<Pair<KeySet, KeyAction>> = KeyAction.getNonMenuKeySets(keymaps)
    private var mouseClickActions: Map<Pair<KeySet, Tool>, MouseClickAction> = MouseClickAction.getKeySets(keymaps)
        .associateWithTool()
    private var mouseScrollActions: Map<KeySet, MouseScrollAction> = MouseScrollAction.getKeySets(keymaps).toMap()

    private val _keyboardActionFlow = MutableSharedFlow<KeyAction>(replay = 0)

    /**
     * A flow of [KeyAction]s that are triggered by keyboard inputs.
     */
    val keyboardActionFlow: Flow<KeyAction> = _keyboardActionFlow.asSharedFlow()

    private val _keyboardStateFlow = MutableStateFlow(getIdleState())

    /**
     * A flow of [KeyboardState]s that containing information about the current pressed keys.
     */
    val keyboardStateFlow: StateFlow<KeyboardState> = _keyboardStateFlow.asStateFlow()

    /**
     * Should be called when the keymaps are updated.
     */
    suspend fun updateKeymaps(keymaps: AppConf.Keymaps) {
        keyActions = KeyAction.getNonMenuKeySets(keymaps)
        mouseClickActions = MouseClickAction.getKeySets(keymaps).associateWithTool()
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

    /**
     * Should be called when a [KeyEvent] is received at the app's top layer.
     */
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

    /**
     * Should be called when the window loses focus.
     */
    fun clear() {
        coroutineScope.launch {
            emitState(getIdleState())
        }
    }

    private fun List<Pair<KeySet, MouseClickAction>>.associateWithTool(): Map<Pair<KeySet, Tool>, MouseClickAction> {
        return associate { (keySet, action) -> (keySet to action.tool) to action }
    }
}

/**
 * A data class that contains information about the current pressed keys.
 *
 * @property keySet The current pressed keys.
 * @property availableMouseClickActions A map of [KeySet]s to [MouseClickAction]s that are available to be triggered.
 * @property availableMouseScrollActions A map of [KeySet]s to [MouseScrollAction]s that are available to be triggered.
 */
@Immutable
data class KeyboardState(
    val keySet: KeySet?,
    val availableMouseClickActions: Map<Pair<KeySet, Tool>, MouseClickAction>,
    val availableMouseScrollActions: Map<KeySet, MouseScrollAction>,
) {

    /**
     * Get the [MouseClickAction] that is enabled by the current pressed keys and the given [PointerEvent].
     */
    fun getEnabledMouseClickAction(pointerEvent: PointerEvent, tool: Tool = Tool.Cursor): MouseClickAction? {
        val mainKey = pointerEvent.toVirtualKey() ?: return null
        return availableMouseClickActions[KeySet(mainKey, keySet?.subKeys.orEmpty()) to tool]
            ?.takeIf { it.pointerEventType == null || it.pointerEventType == pointerEvent.type }
    }

    /**
     * Get the [MouseScrollAction] that is enabled by the current pressed keys and the given [PointerEvent].
     */
    fun getEnabledMouseScrollAction(pointerEvent: PointerEvent): MouseScrollAction? {
        if (pointerEvent.type != PointerEventType.Scroll) return null
        val mainKey = pointerEvent.toVirtualKey() ?: return null
        return availableMouseScrollActions[KeySet(mainKey, keySet?.subKeys.orEmpty())]
    }
}

/**
 * Returns whether the given [KeyEvent] is a key release event of the given [ActualKey].
 */
fun KeyEvent.isReleased(key: ActualKey) = released && this.key == key

/**
 * Returns whether the given [KeyEvent] has the native Ctrl key pressed. By "native Ctrl", it means `Ctrl` on
 * Windows/Linux and `Meta` on macOS.
 */
val KeyEvent.isNativeCtrlPressed get() = if (isMacOS) isMetaPressed else isCtrlPressed

/**
 * Returns whether the given [KeyEvent] has the native Meta key pressed. By "native Meta", it means `Meta` on
 * Windows/Linux and `Ctrl` on macOS.
 */
val KeyEvent.isNativeMetaPressed get() = if (isMacOS) isCtrlPressed else isMetaPressed

/**
 * Whether the given [KeyEvent] is a key release event.
 */
val KeyEvent.released get() = type == KeyEventType.KeyUp
