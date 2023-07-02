package com.sdercolin.vlabeler.ui.theme

import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pink = Color(0xfff48fb1)
val DarkPink = Color(0xffad375f)
val DarkGray = Color(0xff1e1e1e)
val Gray = Color(0xff252525)
val LightGray = Color(0xfff2f2f2)
val Black = Color.Black
val Black80 = Color(0xb3000000)
val Black50 = Color(0x7f000000)
val Black20 = Color(0x33000000)
val White = Color.White
val White80 = Color(0xb3ffffff)
val White50 = Color(0x7fffffff)
val White20 = Color(0x33ffffff)
val DarkYellow = Color(0xffe89f17)
val DarkRed = Color(0xffcf6679)
val DarkGreen = Color(0xff4caf50)

@Composable
fun getSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colors.primary,
    uncheckedThumbColor = MaterialTheme.colors.onSurface,
    uncheckedTrackColor = MaterialTheme.colors.onSurface,
)

@Composable
fun getCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colors.primary,
)
