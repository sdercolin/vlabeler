package com.sdercolin.vlabeler.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.util.toMillisecond

class VideoState(
    private val playerState: PlayerState,
    val closeManually: () -> Unit,
    val debug: Boolean = false,
) {
    var width: Dp by mutableStateOf(DefaultWidth)
    var height: Dp = width * AspectRatio

    val videoPlayer: MiniVideo = MiniVideo()
    var videoPath: String? = null
    var currentSampleRate = 0f
    var mode: Mode? by mutableStateOf(null)

    var lastSavedTime: Long? = null
        private set

    fun log(message: String) {
        if (debug) {
            println("##video## $message")
        }
    }

    private fun Float.toTime(): Long {
        return toMillisecond(this, currentSampleRate).toLong()
    }

    fun audioPlayerCurrentTime(): Long? {
        return playerState.framePosition?.toTime()
    }

    fun lastStartedTime(): Long? {
        return playerState.lastStartedFrame?.toFloat()?.toTime()
    }

    fun saveTime(reset: Boolean = false) {
        lastSavedTime = if (reset) null else videoPlayer.currentTime
        log("save time at $lastSavedTime")
    }

    companion object {
        val MinWidth = 200.dp
        val MaxWidth = 600.dp
        val DefaultWidth = 360.dp
        const val AspectRatio = 3f / 4f
    }
    enum class Mode {
        Embedded,
        NewWindow,
    }
}
