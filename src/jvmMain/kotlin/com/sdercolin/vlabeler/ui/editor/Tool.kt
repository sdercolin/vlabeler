package com.sdercolin.vlabeler.ui.editor

import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.string.Strings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Tool(val stringKey: Strings, val keyAction: KeyAction) {
    @SerialName("cursor")
    Cursor(Strings.MenuEditToolsCursor, KeyAction.UseToolCursor),

    @SerialName("scissors")
    Scissors(Strings.MenuEditToolsScissors, KeyAction.UseToolScissors)
}
