package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.theme.White20

@Composable
fun <T> ToggleButtonGroup(
    selected: T,
    options: List<T>,
    onSelectedChange: (T) -> Unit,
    buttonContent: @Composable BoxScope.(T) -> Unit,
) {
    val radius = 4.dp
    Row(
        modifier = Modifier.height(IntrinsicSize.Min)
            .border(
                width = 1.dp,
                color = White20,
                shape = RoundedCornerShape(radius),
            ),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val isFirst = index == 0
            val isLast = index == options.size - 1
            val shape = RoundedCornerShape(
                topStart = if (isFirst) radius else 0.dp,
                bottomStart = if (isFirst) radius else 0.dp,
                topEnd = if (isLast) radius else 0.dp,
                bottomEnd = if (isLast) radius else 0.dp,
            )
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 1.dp)
                        .background(color = White20),
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) White20 else Color.Transparent,
                        shape = shape,
                    )
                    .clickable { onSelectedChange(option) },
            ) {
                buttonContent(option)
            }
        }
    }
}
