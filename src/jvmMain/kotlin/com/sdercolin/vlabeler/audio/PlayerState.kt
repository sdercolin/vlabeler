package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlayerState {

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var framePosition: Int by mutableStateOf(0)
        private set

    fun startPlaying() {
        isPlaying = true
    }

    fun stopPlaying() {
        isPlaying = false
    }

    fun changeFramePosition(position: Int) {
        framePosition = position
    }
}
