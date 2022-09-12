package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.isHexChar
import com.sdercolin.vlabeler.util.rgbHexString
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toColorOrNull
import com.sdercolin.vlabeler.util.toRgbColorOrNull

@Composable
fun ColorHexInputBox(
    value: String,
    defaultValue: String,
    onValidValue: (String) -> Unit,
    useAlpha: Boolean,
    enabled: Boolean = true,
) {
    fun getColor(text: String) = if (useAlpha) text.toColorOrNull() else text.toRgbColorOrNull()

    var text by remember {
        if (getColor(value) == null) {
            mutableStateOf(defaultValue)
        } else {
            mutableStateOf(value)
        }
    }

    LaunchedEffect(value) {
        val inputColor = getColor(value)
        if (inputColor != null && inputColor != getColor(text)) {
            text = value
        }
    }
    var colorPreview by remember(text) { mutableStateOf(getColor(text)) }
    val valueLength = if (useAlpha) 9 else 7

    InputBox(
        enabled = enabled,
        value = text,
        onValueChange = { newValue ->
            var sanitizedValue = newValue
            if (sanitizedValue.firstOrNull() != '#') {
                sanitizedValue = "#" + sanitizedValue.replace("#", "")
            }
            sanitizedValue = sanitizedValue.filter { it.isHexChar || it == '#' }
            sanitizedValue = sanitizedValue.take(valueLength)
            text = sanitizedValue
            getColor(text)?.let {
                colorPreview = it
                val hexString = if (useAlpha) it.argbHexString else it.rgbHexString
                onValidValue(hexString)
            }
        },
        leadingContent = {
            Box(Modifier.size(20.dp)) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(Resources.transparencyGridPng),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                )
                Box(Modifier.fillMaxSize().background(color = colorPreview ?: defaultValue.toColor()))
            }
            Spacer(Modifier.width(15.dp))
        },
    )
}
