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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppViewState
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.Black80
import com.sdercolin.vlabeler.ui.theme.White20

@Composable
fun BoxScope.ToolboxView(viewState: AppViewState) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .background(color = Black80, shape = RoundedCornerShape(5.dp))
            .align(Alignment.BottomEnd)
    ) {
        Tool.values().forEach { tool ->
            val checked = viewState.tool == tool
            val backgroundColor = if (!checked) Color.Transparent else White20
            Box(
                modifier = Modifier.size(40.dp)
                    .background(backgroundColor, shape = RoundedCornerShape(5.dp))
                    .plainClickable { viewState.tool = tool }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (tool) {
                    Tool.Cursor -> Icons.Default.Height
                    Tool.Scissors -> Icons.Default.ContentCut
                }
                val rotate = when (tool) {
                    Tool.Cursor -> 90f
                    Tool.Scissors -> 0f
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotate),
                    tint = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}
