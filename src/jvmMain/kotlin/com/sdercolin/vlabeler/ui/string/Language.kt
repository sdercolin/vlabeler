package com.sdercolin.vlabeler.ui.string

import androidx.compose.runtime.compositionLocalOf

var currentLanguage: Language = Language.default

val LocalLanguage = compositionLocalOf { Language.default }

enum class Language(val code: String, private val displayName: String) : Text {
    English("en", "English"),
    ChineseSimplified("zh-Hans", "简体中文"),
    Japanese("ja", "日本語"),
    Korean("ko", "한국어"),
    ;

    override val text: String
        get() = displayName

    companion object {

        val default = English

        fun find(languageTag: String): Language? {
            for (value in values()) {
                val codeLevels = value.code.split("-").scan("") { acc, s ->
                    if (acc.isEmpty()) s else "$acc-$s"
                }.filter { it.isNotEmpty() }
                for (code in codeLevels.reversed()) {
                    if (languageTag.startsWith(code)) {
                        return value
                    }
                }
            }
            return null
        }
    }
}
