package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AutoWrapScrollableColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    scrollbarWidth: Dp = 15.dp,
    scrollbarSpacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints {
        val outerMaxHeight = maxHeight
        Layout(
            content = {
                Column(modifier.verticalScroll(scrollState)) {
                    content()
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight().width(scrollbarWidth),
                    adapter = rememberScrollbarAdapter(scrollState),
                )
            },
        ) { measurables, constraints ->
            val maxHeight = outerMaxHeight.toPx().toInt()
            val desiredContentHeight = measurables.first().maxIntrinsicHeight(maxHeight)

            val shouldScroll = desiredContentHeight > maxHeight
            val scrollbarOccupiedWidth = if (shouldScroll) (scrollbarWidth + scrollbarSpacing).toPx().toInt() else 0
            val actualContent = measurables.first().measure(
                constraints.copy(
                    maxWidth = constraints.maxWidth - scrollbarOccupiedWidth,
                    maxHeight = maxHeight,
                ),
            )
            val scrollbar = measurables[1].measure(constraints.copy(maxHeight = maxHeight))

            layout(
                width = constraints.maxWidth,
                height = actualContent.height,
            ) {
                actualContent.placeRelative(0, 0)
                if (shouldScroll) {
                    scrollbar.placeRelative(constraints.maxWidth - scrollbarWidth.toPx().toInt(), 0)
                }
            }
        }
    }
}
