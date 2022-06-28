package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.Black50

@Composable
fun CircularProgress() {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50).plainClickable(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
