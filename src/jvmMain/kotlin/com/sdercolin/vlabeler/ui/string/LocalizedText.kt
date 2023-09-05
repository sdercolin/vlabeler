package com.sdercolin.vlabeler.ui.string

import androidx.compose.runtime.Composable

interface LocalizedText {
    val stringKey: Strings
    val args: Array<out Any?> get() = emptyArray()

    @Composable
    fun getText(): String = string(stringKey, *args)

    fun getText(language: Language): String = stringCertain(stringKey, language, *args)
}
