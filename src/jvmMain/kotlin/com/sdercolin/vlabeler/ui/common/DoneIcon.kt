package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.DarkGreen
import com.sdercolin.vlabeler.ui.theme.White20

@Composable
fun DoneIcon(done: Boolean, modifier: Modifier = Modifier) = Icon(
    modifier = modifier,
    imageVector = Icons.Default.Done,
    contentDescription = null,
    tint = if (done) DarkGreen else White20,
)
