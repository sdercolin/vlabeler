package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf

class PreferencesEditorState(private val initConf: AppConf, private val submit: (AppConf?) -> Unit) {

    private var savedConf: AppConf by mutableStateOf(initConf)
    private var _conf: AppConf by mutableStateOf(initConf)
    val conf get() = _conf
    private val pageChildrenMap = mutableMapOf<PreferencesPageListItem, List<PreferencesPageListItem>>()
    val pages = mutableStateListOf<PreferencesPageListItem>().apply {
        val initialItems = PreferencesPage.getRootPages().toTypedArray().map { PreferencesPageListItem(it, 0) }
        addAll(initialItems)
    }
    var selectedPage: PreferencesPageListItem by mutableStateOf(pages.first())

    val canSave get() = savedConf != _conf

    fun save() {
        savedConf = _conf
    }

    fun finish(positive: Boolean) {
        if (positive) save()
        submit(savedConf.takeIf { it != initConf })
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
            ?: page.page.children
                .map { PreferencesPageListItem(it, page.level + 1) }
                .also { pageChildrenMap[page] = it }
        val index = pages.indexOf(page)
        pages.addAll(index + 1, children)
        page.isExpanded = true
    }

    private fun collapsePage(page: PreferencesPageListItem) {
        val children = pageChildrenMap[page] ?: return
        pages.removeAll(children)
        page.isExpanded = false
        if (children.contains(selectedPage)) {
            selectedPage = page
        }
    }

    fun selectPage(page: PreferencesPageListItem) {
        selectedPage = page
    }
}
