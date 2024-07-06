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
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.model.action.MouseScrollActionKeyBind
import com.sdercolin.vlabeler.ui.string.Language

class PreferencesKeymapState<K : Action>(private val item: PreferencesItem.Keymap<K>, state: PreferencesEditorState) {

    var allKeyBinds: List<ActionKeyBind<K>> by mutableStateOf(getAllKeyBinds(state.conf))
        private set

    var searchText by mutableStateOf("")
        private set

    var displayedKeyBinds: List<ActionKeyBind<K>> by mutableStateOf(allKeyBinds)
        private set

    fun search(text: String, language: Language) {
        searchText = text
        displayedKeyBinds = filterKeyBinds(searchText, language)
    }

    private fun filterKeyBinds(text: String, language: Language) =
        allKeyBinds.filter { if (text.isNotEmpty()) it.getTitle(language).contains(text, ignoreCase = true) else true }
            .sortedBy { it.action.displayOrder }

    private fun getAllKeyBinds(
        conf: AppConf,
    ): List<ActionKeyBind<K>> {
        val customKeyBinds: List<ActionKeyBind<K>> = item.select(conf)
        val customActions: List<K> = customKeyBinds.map { it.action }
        return when (item.actionType) {
            ActionType.Key -> KeyAction.entries.filterNot { customActions.contains(it as Action) }
                .map { KeyActionKeyBind(it, it.defaultKeySet) }
            ActionType.MouseClick -> MouseClickAction.entries.filterNot { customActions.contains(it as Action) }
                .map { MouseClickActionKeyBind(it, it.defaultKeySet) }
            ActionType.MouseScroll -> MouseScrollAction.entries.filterNot { customActions.contains(it as Action) }
                .map { MouseScrollActionKeyBind(it, it.defaultKeySet) }
        }
            .filterIsInstance<ActionKeyBind<K>>()
            .plus(customKeyBinds)
            .sortedBy { it.action.displayOrder }
    }

    fun update(conf: AppConf, language: Language) {
        allKeyBinds = getAllKeyBinds(conf)
        search(searchText, language)
    }
}
