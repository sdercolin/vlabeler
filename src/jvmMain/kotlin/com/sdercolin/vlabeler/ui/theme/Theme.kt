package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = lightColors(
    primary = Color(0xffffb927),
    primaryVariant = Color(0xfff07500)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = colors,
    content = content
)