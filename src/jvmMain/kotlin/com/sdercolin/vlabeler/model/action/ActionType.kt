package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.ui.string.Strings

/**
 * Type of [Action].
 *
 * @property requiresCompleteKeySet Whether the action requires a complete key set to be triggered.
 * @property descriptionInEditDialog The description of the action that is displayed in the edit dialog.
 */
enum class ActionType(val requiresCompleteKeySet: Boolean, val descriptionInEditDialog: Strings?) {
    Key(true, null),
    MouseClick(false, Strings.PreferencesKeymapEditDialogDescriptionMouseClick),
    MouseScroll(false, Strings.PreferencesKeymapEditDialogDescriptionMouseScroll),
}
