package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.Black50

@Composable
fun DialogContainer(
    widthFraction: Float,
    heightFraction: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50).plainClickable(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth(widthFraction)
                .fillMaxHeight(heightFraction)
                .plainClickable(),
        ) {
            content()
        }
    }
}
