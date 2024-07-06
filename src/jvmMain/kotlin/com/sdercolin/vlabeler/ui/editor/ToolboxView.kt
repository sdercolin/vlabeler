package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.Black80
import com.sdercolin.vlabeler.ui.theme.White20
import java.awt.Cursor

@Composable
fun BoxScope.ToolboxView(selectedTool: Tool, select: (Tool) -> Unit) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .background(color = Black80, shape = RoundedCornerShape(5.dp))
            .align(Alignment.BottomEnd),
    ) {
        Tool.entries.forEach { tool ->
            val checked = selectedTool == tool
            val backgroundColor = if (!checked) Color.Transparent else White20
            Box(
                modifier = Modifier.size(40.dp)
                    .background(backgroundColor, shape = RoundedCornerShape(5.dp))
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .plainClickable { select(tool) }
                    .padding(5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier.rotate(tool.iconRotate),
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}
