package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.ui.string.Strings

abstract class PreferencesPage(
    val displayedName: Strings,
    val description: Strings,
    val scrollable: Boolean = true,
) {
    open val children: List<PreferencesPage> = listOf()
    open val content: List<PreferencesGroup> = listOf()

    val name: String get() = displayedName.name
}
