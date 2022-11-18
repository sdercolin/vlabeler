package com.sdercolin.vlabeler.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.util.or
import com.sdercolin.vlabeler.util.toMillisecond

class VideoState(val debug: Boolean = true) {
    var width: Dp by mutableStateOf(RememberWidth)
    var height: Dp = RememberHeight
    var window: ComposeWindow? = null
    var embeddedMode: Boolean by mutableStateOf(true)
    val miniVideo = MiniVideo()
    var currentSampleRate = 0f
    var videoPath: String = ""
    var playerState: PlayerState? = null

    fun log(message: String, attribute: Any = "", unit: String = ""): VideoState {
        if (debug)
            println("##video## $message $attribute$unit")
        return this
    }

    fun setPlayerState(playerState: PlayerState): VideoState {
        this.playerState = playerState
        return this
    }

    fun getSyncTime(fallbackFrame: Float = -1f): Long? {
        return playerState?.framePosition.or(fallbackFrame).let {
            if (it == -1f) return null
            return toMillisecond(it, currentSampleRate).toLong()
        }
    }

    fun syncTime(fallbackFrame: Float = -1f): VideoState {
        val time = getSyncTime(fallbackFrame)
        time?.let{
            miniVideo.mediaPlayer.controls().setPause(false)
            miniVideo.mediaPlayer.controls().setTime(it)
            log("sync time to", it, "ms")
        }
        return this
    }

    fun rememberSize(): VideoState {
        RememberWidth = width
        RememberHeight = height
        return this
    }

    fun locateVideoPath(samplePath: String): VideoState {
        videoPath = samplePath.substringBeforeLast('.') + ".mp4"
        log("using video of", videoPath)
        return this
    }

    companion object {
        val MinWidth = 200.dp
        val MaxWidth = 500.dp
        var RememberWidth = 360.dp
        var RememberHeight = RememberWidth * 3 / 4
    }
}
