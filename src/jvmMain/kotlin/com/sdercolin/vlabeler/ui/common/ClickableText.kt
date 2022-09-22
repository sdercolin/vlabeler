package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun SingleClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    style: TextStyle = MaterialTheme.typography.body2,
    enabled: Boolean = true,
) {
    if (enabled) {
        ClickableText(
            modifier = modifier,
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = color,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(text)
                }
            },
            onClick = { onClick() },
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            Text(
                modifier = modifier,
                text = text,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
