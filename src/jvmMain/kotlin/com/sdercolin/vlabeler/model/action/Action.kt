package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.ui.string.Language

/**
 * An action that is registered in the keymaps.
 */
sealed interface Action {

    /**
     * The order of the action in the keymap list.
     */
    val displayOrder: Int

    /**
     * The localized title of the action.
     */
    fun getTitle(language: Language): String
}
