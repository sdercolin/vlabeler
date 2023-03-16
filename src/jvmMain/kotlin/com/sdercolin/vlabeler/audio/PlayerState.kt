package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.AppState
import kotlinx.coroutines.CoroutineScope

/**
 * The state of the audio player hosted in [AppState], and controlled by [Player].
 */
class PlayerState(appConf: AppConf, coroutineScope: CoroutineScope) {

    private val playerListener = object : Player.Listener {

        override fun onStartPlaying(fromFrame: Long?) {
            startPlaying(fromFrame ?: 0)
        }

        override fun onStopPlaying() {
            stopPlaying()
        }

        override fun onFramePositionChanged(position: Float) {
            setFramePosition(position)
        }
    }

    val player = Player(
        playbackConfig = appConf.playback,
        maxSampleRate = appConf.painter.amplitude.resampleDownToHz,
        coroutineScope = coroutineScope,
        state = this,
        listener = playerListener,
    )

    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var framePosition: Float? by mutableStateOf(null)
        private set

    var lastStartedFrame: Long? = null
        private set

    private fun startPlaying(startFrame: Long) {
        lastStartedFrame = startFrame
        isPlaying = true
        framePosition = null
    }

    private fun stopPlaying() {
        lastStartedFrame = null
        isPlaying = false
        framePosition = null
    }

    private fun setFramePosition(position: Float) {
        framePosition = position
    }
}
