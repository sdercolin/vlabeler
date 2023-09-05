package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.White80

@Composable
fun ExtraIcon(modifier: Modifier = Modifier) = Icon(
    modifier = modifier,
    imageVector = Icons.Default.Info,
    contentDescription = null,
    tint = White80,
)
