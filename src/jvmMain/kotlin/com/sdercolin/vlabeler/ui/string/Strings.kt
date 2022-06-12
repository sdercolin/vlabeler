package com.sdercolin.vlabeler.ui.string

import com.sdercolin.vlabeler.ui.string.Language.English

enum class Strings(val en: String) {
    LanguageDisplayName(
        en = English.displayName
    ),
    CommonDialogConfirmButton(
        en = "OK"
    ),
    SetResolutionDialogTitle(
        en = "Horizontal Resolution"
    ),
    SetResolutionDialogDescription(
        en = "Input horizontal resolution (frames per pixel) for the editor (%d ~ %d)"
    );

    fun get(language: Language): String = when (language) {
        English -> en
    }
}

fun string(key: Strings, vararg params: Any?): String {
    val template = key.get(currentLanguage)
    return template.format(*params)
}