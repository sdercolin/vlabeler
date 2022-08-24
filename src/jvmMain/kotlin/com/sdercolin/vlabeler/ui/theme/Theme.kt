package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val colors = darkColors(
    primary = Pink,
    primaryVariant = DarkPink,
    background = DarkGray,
    surface = Gray,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = LightGray,
    onSurface = LightGray,
    onError = Black,
)

private val typography = Typography(
    button = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 1.25.sp,
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        letterSpacing = 0.sp,
    ),
    h5 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
    ),
    h6 = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
    ),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = colors,
    typography = typography,
    content = content,
)
