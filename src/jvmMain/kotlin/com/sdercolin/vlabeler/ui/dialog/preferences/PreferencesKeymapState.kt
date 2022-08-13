package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind

class PreferencesKeymapState<K : Action>(private val item: PreferencesItem.Keymap<K>, state: PreferencesEditorState) {

    var allKeyBinds: List<ActionKeyBind<K>> by mutableStateOf(getAllKeyBinds(state.conf))
        private set

    var searchText by mutableStateOf("")
        private set

    var displayedKeyBinds by mutableStateOf(filterKeyBinds(searchText))
        private set

    fun search(text: String) {
        searchText = text
        displayedKeyBinds = filterKeyBinds(searchText)
    }

    private fun filterKeyBinds(text: String) =
        allKeyBinds.filter { if (text.isNotEmpty()) it.title.contains(text, ignoreCase = true) else true }
            .sortedBy { it.action.displayOrder }

    private fun getAllKeyBinds(
        conf: AppConf,
    ): List<ActionKeyBind<K>> {
        val customKeyBinds: List<ActionKeyBind<K>> = item.select(conf)
        val customActions: List<K> = customKeyBinds.map { it.action }
        return when (item.actionType) {
            ActionType.Key -> KeyAction.values().filterNot { customActions.contains(it as Action) }
                .map { KeyActionKeyBind(it, it.defaultKeySet) }
                .filterIsInstance<ActionKeyBind<K>>()
                .plus(customKeyBinds)
        }
    }

    fun update(conf: AppConf) {
        allKeyBinds = getAllKeyBinds(conf)
        search(searchText)
    }
}
