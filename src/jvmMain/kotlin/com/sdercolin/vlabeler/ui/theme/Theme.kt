package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColors(
    primary = AppColor.Pink,
    primaryVariant = AppColor.Pink,
    background = AppColor.DarkGray,
    surface = AppColor.Gray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = AppColor.LightGray,
    onSurface = AppColor.LightGray,
    onError = Color.Black
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = colors,
    typography = Typography(),
    content = content
)