package com.sdercolin.vlabeler.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.util.toMillisecond
import java.awt.Container

class VideoState(val debug: Boolean = true) {
    var width: Dp by mutableStateOf(RememberWidth)
    var height: Dp = RememberHeight

    var window: ComposeWindow? = null
    lateinit var container: Container

    var playerState: PlayerState? = null
    var videoPath: String = ""
    var currentSampleRate = 0f
    var embeddedMode: Boolean by mutableStateOf(true)

    val miniVideo = MiniVideo()
    var afterPopup: Boolean = false
    val currentTime: Long
        get() = miniVideo.mediaPlayer.status().time()
    lateinit var syncOp: (time: Long?) -> Unit

    fun log(message: String): VideoState {
        if (debug)
            println("##video## $message")
        return this
    }

    fun setPlayerReference(playerState: PlayerState): VideoState {
        this.playerState = playerState
        return this
    }

    fun newestTime(): Long? {
        return playerState?.framePosition?.let {
            toMillisecond(it, currentSampleRate).toLong()
        }
    }

    fun rememberSize(): VideoState {
        RememberWidth = width
        RememberHeight = height
        return this
    }

    fun locateVideoPath(samplePath: String): VideoState {
        videoPath = samplePath.substringBeforeLast('.') + ".mp4"
        return this
    }

    fun setEmbedded(embeddedMode: Boolean) {
        if (this.embeddedMode != embeddedMode) {
            log("Video ${ if (embeddedMode) "embedded" else "new window" } mode")
            afterPopup = false // should re-popup after changing embeddedMode
            this.embeddedMode = embeddedMode
        }
    }

    companion object {
        val MinWidth = 200.dp
        val MaxWidth = 600.dp
        var RememberWidth = 360.dp
        var RememberHeight = RememberWidth * 3 / 4
    }
}
