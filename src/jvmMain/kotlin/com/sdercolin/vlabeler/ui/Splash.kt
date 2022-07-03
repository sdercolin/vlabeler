package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.common.CircularProgress

@Composable
fun Splash() {
    Surface(Modifier.fillMaxSize()) {
        CircularProgress(darkenBackground = false)
    }
}
