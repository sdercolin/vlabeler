package com.sdercolin.vlabeler.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.util.or

@Composable
fun VideoMain(
    videoState: VideoState,
    playerState: PlayerState,
) {
    videoState.log("called popup video")

    val isPlaying: Boolean by mutableStateOf(playerState.isPlaying)
    if (videoState.miniVideo.loaded) {
        if (isPlaying) {
            playerState.syncToken
                .takeIf { it != -1L }?.let { videoState.syncTime(it.toFloat()) }
        } else {
            videoState.miniVideo.mediaPlayer.controls().setPause(true)
            videoState.log("pause")
        }
    }

    if (videoState.videoPath != "") {
        remember {
            videoState
                .log("start popup video")
                .setPlayerState(playerState)
        }
        when {
            videoState.embeddedMode -> embeddedMode(videoState)
            else -> newWindowMode(videoState)
        }

        DisposableEffect(Unit) {
            onDispose { videoState.miniVideo.loaded = false }
        }
    }
}
