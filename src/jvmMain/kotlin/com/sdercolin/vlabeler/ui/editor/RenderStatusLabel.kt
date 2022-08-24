package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black80
import com.sdercolin.vlabeler.ui.theme.DarkYellow

@Composable
fun BoxScope.RenderStatusLabel(renderStatus: Pair<Int, Int>) {
    if (renderStatus.first < renderStatus.second) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(color = Black80, shape = RoundedCornerShape(5.dp))
                .padding(10.dp)
                .align(Alignment.BottomStart),
        ) {
            Text(
                text = string(Strings.EditorRenderStatusLabel, renderStatus.first, renderStatus.second),
                style = MaterialTheme.typography.caption,
                color = DarkYellow,
            )
        }
    }
}
