package com.sdercolin.vlabeler.ui.common

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    style: TextStyle = MaterialTheme.typography.body2
) {
    androidx.compose.foundation.text.ClickableText(
        modifier = modifier,
        text = buildAnnotatedString {
            append(text)
            addStyle(
                style = SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline,
                ),
                start = 0,
                end = text.length,
            )
        },
        onClick = { onClick() },
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
