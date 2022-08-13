package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.string

sealed class ActionKeyBind<T : Any> {
    abstract val action: T
    abstract val keySet: KeySet?
    abstract val title: String
    abstract val editable: Boolean
}

data class KeyActionKeyBind(
    override val action: KeyAction,
    override val keySet: KeySet?
) : ActionKeyBind<KeyAction>() {
    override val title: String = action.displayedNameSections.joinToString(" > ") { string(it) }
    override val editable: Boolean = true
}
