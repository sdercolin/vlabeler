package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColors(
    primary = Color(0xfff48fb1),
    primaryVariant = Color(0xfff48fb1),
    background = Color(0xff1e1e1e),
    surface = Color(0xFF252525),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xfff2f2f2),
    onSurface = Color(0xfff2f2f2),
    onError = Color.Black
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = colors,
    typography = Typography(),
    content = content
)