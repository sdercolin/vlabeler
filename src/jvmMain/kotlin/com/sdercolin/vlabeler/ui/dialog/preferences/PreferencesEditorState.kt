package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.getConflictingKeyBinds

class PreferencesEditorState(
    initConf: AppConf,
    private val submit: (AppConf?) -> Unit,
    private val apply: (AppConf) -> Unit,
    initialPage: PreferencesPage?,
    private val onViewPage: (PreferencesPage) -> Unit,
) {
    var savedConf: AppConf by mutableStateOf(initConf)
        private set
    private var _conf: AppConf by mutableStateOf(initConf)
    val conf get() = _conf
    private val pageChildrenMap = mutableMapOf<PreferencesPageListItem, List<PreferencesPageListItem>>()
    val pages = mutableStateListOf<PreferencesPageListItem>().apply {
        val rootPages = PreferencesPage.getRootPages().toTypedArray().map { PreferencesPageListItem(it, 0) }
        addAll(rootPages)
        if (initialPage != null) {
            val route = mutableListOf<PreferencesPage>()

            fun search(page: PreferencesPage): Boolean {
                if (page == initialPage) {
                    return true
                }
                route.add(page)
                val children = page.children
                if (children.isNotEmpty()) {
                    for (child in children) {
                        if (search(child)) {
                            return true
                        }
                    }
                }
                route.removeAt(route.lastIndex)
                return false
            }

            rootPages.forEach { rootPage ->
                if (search(rootPage.model)) {
                    route.forEachIndexed { level, pageModel ->
                        val page = first { it.model == pageModel }
                        addAll(
                            indexOf(page) + 1,
                            pageModel.children.map { PreferencesPageListItem(it, level + 1) },
                        )
                        page.isExpanded = true
                    }
                    return@forEach
                }
            }
        }
    }
    var selectedPage: PreferencesPageListItem by mutableStateOf(
        if (initialPage != null) {
            pages.first { it.model == initialPage }
        } else {
            pages.first()
        },
    )

    val needSave get() = savedConf != _conf

    fun save() {
        apply(_conf)
        savedConf = _conf
    }

    fun finish(positive: Boolean) {
        if (positive && needSave) {
            submit(_conf)
        } else {
            submit(null)
        }
    }

    fun resetPage() {
        _conf = selectedPage.model.content
            .flatMap { it.items }
            .fold(conf) { acc, item ->
                item.reset(acc)
            }
    }

    fun resetAll() {
        _conf = AppConf()
    }

    fun togglePage(page: PreferencesPageListItem) {
        if (page.isExpanded) {
            collapsePage(page)
        } else {
            expandPage(page)
        }
    }

    private fun expandPage(page: PreferencesPageListItem) {
        val children = pageChildrenMap[page]
            ?: page.createChildren().also { pageChildrenMap[page] = it }
        val index = pages.indexOf(page)
        pages.addAll(index + 1, children)
        page.isExpanded = true
    }

    private fun collapsePage(page: PreferencesPageListItem) {
        val children = pageChildrenMap[page] ?: return
        pages.removeAll(children)
        page.isExpanded = false
        if (children.contains(selectedPage)) {
            selectPage(page)
        }
    }

    fun selectPage(page: PreferencesPageListItem) {
        selectedPage = page
        onViewPage(page.model)
    }

    fun selectPageByLink(pageModel: PreferencesPage) {
        val page = pages.firstOrNull { it.model == pageModel }
            ?: run {
                val parent = selectedPage
                expandPage(parent)
                pages.first { it.model == pageModel }
            }
        selectPage(page)
    }

    private fun PreferencesPageListItem.createChildren() =
        model.children.map { PreferencesPageListItem(it, level + 1) }

    fun <T> update(item: PreferencesItem<T>, newValue: T) {
        _conf = item.update(conf, newValue)
    }

    var keymapItemEditDialogArgs: KeymapItemEditDialogArgs<*>? by mutableStateOf(null)
        private set
    var keymapItemEditConflictDialogArgs: KeymapItemEditConflictDialogArgs<*>? by mutableStateOf(null)
        private set

    data class KeymapItemEditDialogArgs<K : Action>(
        val actionKeyBind: ActionKeyBind<K>,
        val keymapItem: PreferencesItem.Keymap<K>,
        val allKeyBinds: List<ActionKeyBind<K>>,
        val submit: (ActionKeyBind<K>?) -> Unit,
    )

    data class KeymapItemEditConflictDialogArgs<K : Action>(
        val editedKeyBind: ActionKeyBind<K>,
        val conflictingKeyBinds: List<ActionKeyBind<K>>,
        val cancel: () -> Unit,
        val keep: () -> Unit,
        val remove: () -> Unit,
    )

    fun <K : Action> openKeymapItemEditDialog(
        actionKeyBind: ActionKeyBind<K>,
        keymap: PreferencesItem.Keymap<K>,
        allKeyBinds: List<ActionKeyBind<K>>,
    ) {
        keymapItemEditDialogArgs = KeymapItemEditDialogArgs(
            actionKeyBind = actionKeyBind,
            keymapItem = keymap,
            allKeyBinds = allKeyBinds,
            submit = { edited ->
                submitKeymapItemEditDialog(edited, keymap, allKeyBinds)
            },
        )
    }

    private fun <K : Action> submitKeymapItemEditDialog(
        edited: ActionKeyBind<K>?,
        keymap: PreferencesItem.Keymap<K>,
        allKeyBinds: List<ActionKeyBind<K>>,
    ) {
        keymapItemEditDialogArgs = null
        if (edited == null) return
        val editedKeymap = keymap.select(conf).associateBy { it.action }.toMutableMap()
        editedKeymap.update(edited)
        val updatedConf = keymap.update(conf, editedKeymap.map { it.value })
        val conflictingKeyBinds = allKeyBinds.getConflictingKeyBinds(edited.keySet, edited.action)
        if (conflictingKeyBinds.isEmpty()) {
            _conf = updatedConf
            return
        }
        keymapItemEditConflictDialogArgs = KeymapItemEditConflictDialogArgs(
            editedKeyBind = edited,
            conflictingKeyBinds = conflictingKeyBinds,
            cancel = {
                keymapItemEditConflictDialogArgs = null
            },
            keep = {
                keymapItemEditConflictDialogArgs = null
                _conf = updatedConf
            },
            remove = {
                keymapItemEditConflictDialogArgs = null
                conflictingKeyBinds.forEach {
                    editedKeymap.update(it.update(null))
                }
                _conf = keymap.update(conf, editedKeymap.map { it.value })
            },
        )
    }

    private fun <K : Action> MutableMap<K, ActionKeyBind<K>>.update(
        edited: ActionKeyBind<K>,
    ) {
        if (edited.equalsDefault) {
            remove(edited.action)
        } else {
            this[edited.action] = edited
        }
    }
}
