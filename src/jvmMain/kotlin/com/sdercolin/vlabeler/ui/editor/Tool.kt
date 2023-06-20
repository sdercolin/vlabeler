package com.sdercolin.vlabeler.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.ui.graphics.vector.ImageVector
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.theme.icon.ToolPlaybackArrowRight
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Tool(
    val stringKey: Strings,
    val keyAction: KeyAction,
    val cursorPath: String?,
    val icon: ImageVector,
    val iconRotate: Float,
) {
    @SerialName("cursor")
    Cursor(
        stringKey = Strings.MenuEditToolsCursor,
        keyAction = KeyAction.UseToolCursor,
        cursorPath = null,
        icon = Icons.Default.Height,
        iconRotate = 90f,
    ),

    @SerialName("scissors")
    Scissors(
        stringKey = Strings.MenuEditToolsScissors,
        keyAction = KeyAction.UseToolScissors,
        cursorPath = "img/scissors_tool.png",
        icon = Icons.Default.ContentCut,
        iconRotate = 270f,
    ),

    @SerialName("pan")
    Pan(
        stringKey = Strings.MenuEditToolsPan,
        keyAction = KeyAction.UseToolPan,
        cursorPath = "img/pan_tool.png",
        icon = Icons.Default.PanTool,
        iconRotate = 0f,
    ),

    @SerialName("playback")
    Playback(
        stringKey = Strings.MenuEditToolsPlayback,
        keyAction = KeyAction.UseToolPlayback,
        cursorPath = "img/playback_tool.png",
        icon = ToolPlaybackArrowRight,
        iconRotate = 0f,
    ),
}
