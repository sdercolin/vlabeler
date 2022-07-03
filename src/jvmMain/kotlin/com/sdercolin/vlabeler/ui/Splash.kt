package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.common.CircularProgress

@Composable
fun Splash() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        CircularProgress()
    }
}
