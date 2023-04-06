package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
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

@Composable
fun PartialClickableText(
    text: String,
    clickables: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    clickableColor: Color = MaterialTheme.colors.primary,
    style: TextStyle = MaterialTheme.typography.body2,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotatedText = buildAnnotatedString {
        var lastEnd = 0
        clickables.forEach { (clickableText, _) ->
            withStyle(SpanStyle(color = color)) {
                append(text.substring(lastEnd, text.indexOf(clickableText, lastEnd)))
            }
            pushStringAnnotation(clickableText, clickableText)
            withStyle(
                SpanStyle(
                    color = clickableColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(clickableText)
            }
            pop()
            lastEnd = text.indexOf(clickableText, lastEnd) + clickableText.length
        }
        withStyle(SpanStyle(color = color)) {
            append(text.substring(lastEnd))
        }
    }
    ClickableText(
        modifier = modifier,
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(offset, offset).firstOrNull()?.let { annotation ->
                clickables.firstOrNull { it.first == annotation.item }?.second?.invoke()
            }
        },
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}
