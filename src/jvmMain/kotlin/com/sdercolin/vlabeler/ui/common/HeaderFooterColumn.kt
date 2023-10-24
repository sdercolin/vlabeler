package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun HeaderFooterColumn(
    modifier: Modifier = Modifier,
    contentScrollable: Boolean = true,
    header: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Layout(
        content = {
            Column {
                header()
            }
            if (contentScrollable) {
                AutoWrapScrollableColumn {
                    content()
                }
            } else {
                Column {
                    content()
                }
            }
            Column {
                footer()
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->

        var remainingHeight = constraints.maxHeight
        val headerPlaceable = measurables[0].measure(constraints)
        remainingHeight -= headerPlaceable.height
        val footerPlaceable = measurables[2].measure(constraints.copy(maxHeight = remainingHeight))
        remainingHeight -= footerPlaceable.height
        val contentPlaceable = measurables[1].measure(constraints.copy(maxHeight = remainingHeight))

        val width = maxOf(headerPlaceable.width, footerPlaceable.width, contentPlaceable.width)
        val height = headerPlaceable.height + contentPlaceable.height + footerPlaceable.height

        layout(width, height) {
            headerPlaceable.placeRelative(0, 0)
            contentPlaceable.placeRelative(0, headerPlaceable.height)
            footerPlaceable.placeRelative(0, headerPlaceable.height + contentPlaceable.height)
        }
    }
}
