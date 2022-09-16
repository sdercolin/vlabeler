package com.sdercolin.vlabeler.ui.string

var currentLanguage: Language = Language.English

enum class Language(val code: String, val displayName: String) {
    English("en", "English"),
    ChineseSimplified("zh-Hans", "简体中文"),
}
