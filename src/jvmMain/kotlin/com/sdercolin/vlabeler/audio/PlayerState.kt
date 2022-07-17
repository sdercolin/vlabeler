package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlayerState {

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var framePosition: Float by mutableStateOf(0f)
        private set

    private var framePositionOffset: Float = 0f

    fun startPlaying() {
        isPlaying = true
    }

    fun stopPlaying() {
        isPlaying = false
    }

    fun resetFramePosition(offset: Float, position: Float) {
        framePositionOffset = position - offset
        framePosition = position
    }

    fun setFramePositionRelatively(position: Float) {
        framePosition = framePositionOffset + position
    }
}
