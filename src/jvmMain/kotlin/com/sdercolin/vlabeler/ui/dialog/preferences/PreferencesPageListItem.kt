package com.sdercolin.vlabeler.ui.dialog.preferences

class PreferencesPageListItem(val page: PreferencesPage, val level: Int) {
    val canExpand: Boolean = page.children.isNotEmpty()
    var isExpanded: Boolean = false
}
