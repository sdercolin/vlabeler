package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Composable
fun ConfirmButton(
    onClick: () -> Unit,
    text: String = string(Strings.CommonOkay),
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    if (autoFocus) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Button(
            modifier = modifier.focusRequester(focusRequester),
            onClick = { onClick() },
            enabled = enabled,
        ) {
            Text(text)
        }
    } else {
        Button(
            modifier = modifier,
            onClick = { onClick() },
            enabled = enabled,
        ) {
            Text(text)
        }
    }
}
