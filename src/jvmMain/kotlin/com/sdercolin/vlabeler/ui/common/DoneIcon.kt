package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.DarkGreen
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.ui.theme.White80

@Composable
fun DoneIcon(done: Boolean, modifier: Modifier = Modifier) = Icon(
    modifier = modifier,
    imageVector = Icons.Default.Done,
    contentDescription = null,
    tint = if (done) DarkGreen else White20,
)

@Composable
fun DoneTriStateIcon(done: Boolean?, modifier: Modifier = Modifier) {
    val tint = when (done) {
        true -> DarkGreen
        false -> White80
        null -> White20
    }
    Icon(
        imageVector = Icons.Default.Done,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
