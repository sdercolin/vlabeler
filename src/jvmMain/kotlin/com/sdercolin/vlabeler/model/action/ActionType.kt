package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.ui.string.Strings

enum class ActionType(val requiresCompleteKeySet: Boolean, val descriptionInEditDialog: Strings?) {
    Key(true, null),
    MouseClick(false, Strings.PreferencesKeymapEditDialogDescriptionMouseClick),
    MouseScroll(false, Strings.PreferencesKeymapEditDialogDescriptionMouseScroll),
}
