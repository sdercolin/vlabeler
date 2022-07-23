package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import toStringTrimmed

@Composable
fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    leadingContent: @Composable RowScope.() -> Unit = {},
    errorPrompt: ((String) -> String?)? = null
) {
    val error = errorPrompt?.invoke(value)
    val borderColor = if (error != null) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(vertical = 15.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent()
            BasicTextField(
                modifier = Modifier.widthIn(min = 120.dp),
                value = value,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
                onValueChange = onValueChange,
                maxLines = 1,
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground)
            )
        }
        if (error != null) {
            BasicText(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(color = MaterialTheme.colors.error)
                    .padding(vertical = 5.dp, horizontal = 10.dp),
                text = error,
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onError),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IntegerInputBox(
    intValue: Int,
    onValueChange: (Int) -> Unit,
    min: Int?,
    max: Int?,
    leadingContent: @Composable RowScope.() -> Unit = {}
) {
    var value by remember(intValue) { mutableStateOf(intValue.toString()) }

    InputBox(
        value = value,
        onValueChange = { newValue ->
            value = newValue
            newValue.toIntOrNull()?.let {
                onValueChange(it)
            }
        },
        leadingContent = leadingContent,
        errorPrompt = {
            val parsed = it.toIntOrNull()
            if (parsed == null) {
                string(Strings.CommonInputErrorPromptInteger)
            } else {
                getPromptWithRange(min, max, parsed)
            }
        }
    )
}

@Composable
fun FloatInputBox(
    floatValue: Float,
    onValueChange: (Float) -> Unit,
    min: Float?,
    max: Float?,
    leadingContent: @Composable RowScope.() -> Unit = {}
) {
    var value by remember { mutableStateOf(floatValue.toStringTrimmed()) }
    LaunchedEffect(floatValue) {
        if (value.toFloatOrNull() != floatValue) {
            value = floatValue.toStringTrimmed()
        }
    }

    InputBox(
        value = value,
        onValueChange = { newValue ->
            value = newValue
            newValue.toFloatOrNull()?.let {
                onValueChange(it)
            }
        },
        leadingContent = leadingContent,
        errorPrompt = {
            val parsed = it.toFloatOrNull()
            if (parsed == null) {
                string(Strings.CommonInputErrorPromptNumber)
            } else {
                getPromptWithRange(min, max, parsed)
            }
        }
    )
}

private fun <T : Comparable<T>> getPromptWithRange(min: T?, max: T?, parsed: T) = when {
    (min != null && max != null) && (parsed < min || parsed > max) -> {
        string(Strings.CommonInputErrorPromptNumberRange, min.toString(), max.toString())
    }
    min != null && parsed < min -> {
        string(Strings.CommonInputErrorPromptNumberMin, min.toString())
    }
    max != null && parsed > max -> {
        string(Strings.CommonInputErrorPromptNumberMax, max.toString())
    }
    else -> null
}
