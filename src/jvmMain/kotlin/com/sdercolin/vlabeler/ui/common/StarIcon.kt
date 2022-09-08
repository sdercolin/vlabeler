package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White20

@Composable
fun StarIcon(star: Boolean, modifier: Modifier = Modifier) = Icon(
    modifier = modifier,
    imageVector = if (star) Icons.Default.Star else Icons.Default.StarBorder,
    contentDescription = null,
    tint = if (star) DarkYellow else White20,
)
