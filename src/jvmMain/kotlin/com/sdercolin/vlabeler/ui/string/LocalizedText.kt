package com.sdercolin.vlabeler.ui.string

import androidx.compose.runtime.Composable

interface LocalizedText {
    val stringKey: Strings

    @Composable
    fun getText(vararg args: Any?): String = string(stringKey, *args)
}
