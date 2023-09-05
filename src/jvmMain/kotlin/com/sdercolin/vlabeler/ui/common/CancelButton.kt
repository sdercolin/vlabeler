package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Composable
fun CancelButton(onClick: () -> Unit) = TextButton(
    onClick = { onClick() },
) {
    Text(string(Strings.CommonCancel))
}
