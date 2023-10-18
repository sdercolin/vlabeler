package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.ui.theme.White80

@Composable
fun StarIcon(star: Boolean, modifier: Modifier = Modifier) = Icon(
    modifier = modifier,
    imageVector = if (star) Icons.Default.Star else Icons.Default.StarBorder,
    contentDescription = null,
    tint = if (star) DarkYellow else White20,
)

@Composable
fun StarTriStateIcon(star: Boolean?, modifier: Modifier = Modifier) {
    val icon = when (star) {
        true -> Icons.Default.Star
        else -> Icons.Default.StarOutline
    }
    val tint = when (star) {
        true -> DarkYellow
        false -> White80
        null -> White20
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
