package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlayerState {

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var framePosition: Float? by mutableStateOf(null)
        private set

    fun startPlaying() {
        isPlaying = true
        framePosition = null
    }

    fun stopPlaying() {
        isPlaying = false
        framePosition = null
    }

    fun setFramePosition(position: Float) {
        framePosition = position
    }
}
