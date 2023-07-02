package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Language

/**
 * A data class that contains an action and the key set that triggers the action.
 */
sealed class ActionKeyBind<T : Action> {
    abstract val action: T
    abstract val keySet: KeySet?

    fun getTitle(language: Language): String = action.getTitle(language)
    abstract val editable: Boolean
    abstract val equalsDefault: Boolean
    abstract fun update(keySet: KeySet?): ActionKeyBind<T>
}

data class KeyActionKeyBind(
    override val action: KeyAction,
    override val keySet: KeySet?,
) : ActionKeyBind<KeyAction>() {
    override val editable: Boolean = true
    override val equalsDefault: Boolean = action.defaultKeySet == keySet
    override fun update(keySet: KeySet?): ActionKeyBind<KeyAction> = copy(keySet = keySet)
}

data class MouseClickActionKeyBind(
    override val action: MouseClickAction,
    override val keySet: KeySet?,
) : ActionKeyBind<MouseClickAction>() {
    override val editable: Boolean = true
    override val equalsDefault: Boolean = action.defaultKeySet == keySet
    override fun update(keySet: KeySet?): ActionKeyBind<MouseClickAction> = copy(keySet = keySet)
}

data class MouseScrollActionKeyBind(
    override val action: MouseScrollAction,
    override val keySet: KeySet?,
) : ActionKeyBind<MouseScrollAction>() {
    override val editable: Boolean = action.editable
    override val equalsDefault: Boolean = action.defaultKeySet == keySet
    override fun update(keySet: KeySet?): ActionKeyBind<MouseScrollAction> = copy(keySet = keySet)
}

fun <T : Action> List<ActionKeyBind<T>>.getConflictingKeyBinds(keySet: KeySet?, action: T): List<ActionKeyBind<T>> =
    if (keySet == null) {
        listOf()
    } else {
        filter { it.keySet == keySet && it.action.conflictGroupHash == action.conflictGroupHash && it.action != action }
    }
