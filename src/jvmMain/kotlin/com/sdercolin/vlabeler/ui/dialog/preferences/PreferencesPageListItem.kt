package com.sdercolin.vlabeler.ui.dialog.preferences

class PreferencesPageListItem(val model: PreferencesPage, val level: Int) {
    val canExpand: Boolean = model.children.isNotEmpty()
    var isExpanded: Boolean = false
}
