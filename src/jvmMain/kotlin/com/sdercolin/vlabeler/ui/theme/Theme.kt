package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val colors = darkColors(
    primary = Pink,
    primaryVariant = Pink,
    background = DarkGray,
    surface = Gray,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = LightGray,
    onSurface = LightGray,
    onError = Black
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = colors,
    typography = Typography(),
    content = content
)
