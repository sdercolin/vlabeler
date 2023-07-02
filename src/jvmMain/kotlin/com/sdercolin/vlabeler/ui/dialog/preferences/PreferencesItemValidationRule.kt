package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.string.Strings

/**
 * A validation rule for a preference item.
 *
 * @param validate a function that takes an [AppConf] and returns a boolean indicating whether the preference item is
 *     valid.
 * @param prompt the error prompt to be displayed when the preference item is invalid.
 */
class PreferencesItemValidationRule(
    val validate: (AppConf) -> Boolean,
    val prompt: Strings,
)
