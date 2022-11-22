package com.sdercolin.vlabeler.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.toMillisecond
import com.sdercolin.vlabeler.util.or

@Composable
fun VideoMain(
    videoState: VideoState,
    playerState: PlayerState
) {
    require(videoState.videoPath != "") { "video path not located" }

    val cause = when (listOf(playerState.isPlaying, videoState.afterPopup)) {
        listOf(true, false) -> SyncCause.OpenDuringPlay
        listOf(true, true) -> SyncCause.PlayerStartPlay
        listOf(false, false) -> SyncCause.RecoverFromLastExit
        listOf(false, true) -> SyncCause.CloseDuringPlay
        else -> SyncCause.Nothing
    }
    videoState.syncOp = videoState.sync(cause, playerState.startFrameToken?.let {
        toMillisecond(it.toFloat(), videoState.currentSampleRate).toLong()
    }.or(videoState.currentTime))

    when (videoState.embeddedMode) {
        true -> embeddedMode(videoState)
        false -> newWindowMode(videoState)
    }
    videoState.afterPopup = true
    DisposableEffect(Unit) {
        onDispose {
            videoState.afterPopup = false
            videoState.miniVideo.toggleCallback(false)
        }
    }
}
