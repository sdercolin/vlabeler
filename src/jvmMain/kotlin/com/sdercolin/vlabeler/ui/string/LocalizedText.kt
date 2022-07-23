package com.sdercolin.vlabeler.ui.string

interface LocalizedText {
    val stringKey: Strings
    fun getText(vararg args: Any?): String = string(stringKey, *args)
}
