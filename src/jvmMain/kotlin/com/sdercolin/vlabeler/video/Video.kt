package com.sdercolin.vlabeler.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.sdercolin.vlabeler.audio.PlayerState

private var syncOperations: MutableList<SyncOperation> = mutableListOf()

@Composable
fun Video(
    videoState: VideoState,
    playerState: PlayerState,
) {
    LaunchedEffect(videoState.mode) {
        syncOperations += arrayOf(
            SyncOperation.Initialize,
            if (playerState.isPlaying) SyncOperation.OpenDuringPlay
            else SyncOperation.RecoverFromLastExit,
        )
    }
    LaunchedEffect(playerState.isPlaying) {
        syncOperations +=
            if (playerState.isPlaying) SyncOperation.PlayerStartPlay
            else SyncOperation.PlayerPause
    }
    LaunchedEffect(Unit) {
        syncOperations.removeLast() // HACK: do not trigger play/pause event at initialization
    }

    when (videoState.mode) {
        VideoState.Mode.Embedded -> EmbeddedVideo(videoState)
        VideoState.Mode.NewWindow -> NewWindowVideo(videoState)
        null -> throw UninitializedPropertyAccessException(
            "Video mode not chosen, available ones: ${ VideoState.Mode.values().toList() }",
        )
    }

    LaunchedEffect(syncOperations.toTypedArray()) {
        for (syncOperation in syncOperations) {
            syncOperation.invoke(videoState)
        }
        syncOperations.clear()
    }

    DisposableEffect(Unit) {
        onDispose {
            SyncOperation.Close.invoke(videoState)
        }
    }
}
