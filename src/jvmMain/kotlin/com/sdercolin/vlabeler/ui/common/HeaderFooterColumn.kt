package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A column divided into three parts: header, content, and footer. When the height is not enough, the content part will
 * be folded, leaving the header and footer visible.
 *
 * When using this layout, the parent layout should wrap content height.
 *
 * @param modifier Modifier to be applied to the root layout.
 * @param contentScrollable Whether the content part should be scrollable when the height is not enough.
 * @param header The header part.
 * @param footer The footer part.
 * @param content The content part.
 */
@Composable
fun HeaderFooterColumn(
    modifier: Modifier = Modifier,
    contentScrollable: Boolean = true,
    scrollbarWidth: Dp = 15.dp,
    scrollbarSpacing: Dp = 10.dp,
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
                AutoWrapScrollableColumn(
                    scrollbarWidth = scrollbarWidth,
                    scrollbarSpacing = scrollbarSpacing,
                ) {
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
        val baseConstraints = constraints.copy(minHeight = 0)
        var remainingHeight = constraints.maxHeight
        val headerPlaceable = measurables[0].measure(baseConstraints)
        remainingHeight -= headerPlaceable.height.coerceAtMost(remainingHeight)
        val footerPlaceable = measurables[2].measure(baseConstraints.copy(maxHeight = remainingHeight))
        remainingHeight -= footerPlaceable.height.coerceAtMost(remainingHeight)
        val contentPlaceable = measurables[1].measure(
            baseConstraints.copy(
                maxHeight = remainingHeight,
                minHeight = constraints.minHeight.coerceAtMost(remainingHeight),
            ),
        )

        val width = maxOf(headerPlaceable.width, footerPlaceable.width, contentPlaceable.width)
        val height = headerPlaceable.height + contentPlaceable.height + footerPlaceable.height

        layout(width, height) {
            headerPlaceable.placeRelative(0, 0)
            contentPlaceable.placeRelative(0, headerPlaceable.height)
            footerPlaceable.placeRelative(0, headerPlaceable.height + contentPlaceable.height)
        }
    }
}
