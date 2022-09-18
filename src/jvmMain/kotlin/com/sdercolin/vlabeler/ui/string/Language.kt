package com.sdercolin.vlabeler.ui.string

import androidx.compose.runtime.compositionLocalOf

var currentLanguage: Language = Language.default

val LocalLanguage = compositionLocalOf { Language.default }

enum class Language(val code: String, val displayName: String) : Text {
    English("en", "English"),
    ChineseSimplified("zh-Hans", "简体中文"),
    Japanese("ja", "日本語"),
    ;

    override val text: String
        get() = displayName

    companion object {

        val default = English

        fun find(languageTag: String): Language? {
            return values().find { languageTag.startsWith(it.code) }
        }
    }
}
