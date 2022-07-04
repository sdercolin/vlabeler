package com.sdercolin.vlabeler.ui.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.Black50

@Composable
fun CircularProgress(darkenBackground: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxSize()
            .run { if (darkenBackground) background(color = Black50) else this }
            .plainClickable(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
@Preview
private fun Preview() = CircularProgress()
