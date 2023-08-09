package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toStringTrimmed

@Composable
fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    leadingContent: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    errorPrompt: @Composable ((String) -> String?)? = null,
    enabled: Boolean = true,
    fixedWidth: Boolean = false,
) {
    val error = errorPrompt?.invoke(value)
    val borderColor = if (error != null) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(5.dp),
                )
                .padding(vertical = 15.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()
            BasicTextField(
                modifier = if (fixedWidth) {
                    Modifier.weight(1f)
                } else {
                    Modifier.widthIn(min = 80.dp).width(IntrinsicSize.Min)
                },
                value = value,
                textStyle = MaterialTheme.typography.body2
                    .copy(
                        color = MaterialTheme.colors.onBackground
                            .runIf(!enabled) { copy(alpha = 0.2f) },
                    ),
                onValueChange = onValueChange,
                maxLines = 1,
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                enabled = enabled,
            )
        }
        if (!error.isNullOrEmpty()) {
            BasicText(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(color = MaterialTheme.colors.error)
                    .padding(vertical = 5.dp, horizontal = 10.dp),
                text = error,
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onError),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun IntegerInputBox(
    enabled: Boolean,
    intValue: Int,
    onValueChange: (Int) -> Unit,
    min: Int?,
    max: Int?,
    getInvalidPrompt: (Int) -> Strings?,
    leadingContent: @Composable RowScope.() -> Unit = {},
) {
    var value by remember(intValue) { mutableStateOf(intValue.toString()) }

    val getErrorPrompt = @Composable { newValue: String ->
        val parsed = newValue.toIntOrNull()
        if (parsed == null) {
            string(Strings.CommonInputErrorPromptInteger)
        } else {
            getPromptWithRange(min, max, parsed) ?: getInvalidPrompt(parsed)
                ?.let { string(it) }
        }
    }

    InputBox(
        enabled = enabled,
        value = value,
        onValueChange = { newValue ->
            value = newValue
            newValue.toIntOrNull()?.let {
                if (it in (min ?: Int.MIN_VALUE)..(max ?: Int.MAX_VALUE) && getInvalidPrompt(it) == null) {
                    onValueChange(it)
                }
            }
        },
        leadingContent = leadingContent,
        errorPrompt = getErrorPrompt,
    )
}

@Composable
fun FloatInputBox(
    enabled: Boolean,
    floatValue: Float,
    onValueChange: (Float) -> Unit,
    min: Float?,
    max: Float?,
    getInvalidPrompt: (Float) -> Strings?,
    leadingContent: @Composable RowScope.() -> Unit = {},
) {
    var value by remember { mutableStateOf(floatValue.toStringTrimmed()) }
    LaunchedEffect(floatValue) {
        if (value.toFloatOrNull() != floatValue) {
            value = floatValue.toStringTrimmed()
        }
    }
    val getErrorPrompt = @Composable { newValue: String ->
        val parsed = newValue.toFloatOrNull()
        if (parsed == null) {
            string(Strings.CommonInputErrorPromptNumber)
        } else {
            getPromptWithRange(min, max, parsed) ?: getInvalidPrompt(parsed)
                ?.let { string(it) }
        }
    }

    InputBox(
        enabled = enabled,
        value = value,
        onValueChange = { newValue ->
            value = newValue
            newValue.toFloatOrNull()?.let {
                if (it in (min ?: Float.MIN_VALUE)..(max ?: Float.MAX_VALUE) && getInvalidPrompt(it) == null) {
                    onValueChange(it)
                }
            }
        },
        leadingContent = leadingContent,
        errorPrompt = getErrorPrompt,
    )
}

@Composable
fun TextInputBox(
    enabled: Boolean,
    text: String,
    onValueChange: (String) -> Unit,
    getInvalidPrompt: (String) -> Strings?,
    leadingContent: @Composable RowScope.() -> Unit = {},
) {
    var value by remember { mutableStateOf(text) }
    val getErrorPrompt = @Composable { newValue: String ->
        getInvalidPrompt(newValue)?.let { string(it) }
    }

    InputBox(
        enabled = enabled,
        value = value,
        onValueChange = { newValue ->
            value = newValue
            if (getInvalidPrompt(newValue) == null) {
                onValueChange(newValue)
            }
        },
        leadingContent = leadingContent,
        errorPrompt = getErrorPrompt,
    )
}

@Composable
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
