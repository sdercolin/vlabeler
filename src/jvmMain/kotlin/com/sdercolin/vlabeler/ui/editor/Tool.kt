package com.sdercolin.vlabeler.ui.editor

import com.sdercolin.vlabeler.ui.string.Strings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Tool(val stringKey: Strings) {
    @SerialName("cursor")
    Cursor(Strings.MenuEditToolsCursor),

    @SerialName("scissors")
    Scissors(Strings.MenuEditToolsScissors)
}
