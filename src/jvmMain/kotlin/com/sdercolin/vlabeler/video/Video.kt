package com.sdercolin.vlabeler.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.ProjectStore

/**
 * Composable for video player.
 */
@Composable
fun Video(
    videoState: VideoState,
    playerState: PlayerState,
    projectStore: ProjectStore,
    appConf: AppConf,
) {
    val currentAudioPath = projectStore.project?.currentSampleFile?.absolutePath
    LaunchedEffect(currentAudioPath) {
        currentAudioPath?.let {
            videoState.locatePath(it)
            videoState.saveTime(true)
        }
    }

    if (videoState.videoPath != null) {
        LaunchedEffect(videoState.mode, currentAudioPath) {
            // reload when changing mode or sample file
            videoState.syncOperations += arrayOf(
                SyncOperation.Initialize,
                if (playerState.isPlaying) SyncOperation.OpenDuringPlay
                else SyncOperation.RecoverFromLastExit,
            )
        }
        LaunchedEffect(playerState.isPlaying) {
            videoState.syncOperations +=
                if (playerState.isPlaying) SyncOperation.PlayerStartPlay
                else SyncOperation.PlayerPause
        }
        LaunchedEffect(Unit) {
            videoState.syncOperations.removeLast() // HACK: do not trigger play/pause event at initialization
        }

        when (videoState.mode) {
            VideoState.Mode.Embedded -> EmbeddedVideo(videoState)
            VideoState.Mode.NewWindow -> NewWindowVideo(videoState, appConf)
            null -> throw UninitializedPropertyAccessException(
                "Video mode not chosen, available ones: ${VideoState.Mode.entries.toList()}",
            )
        }

        LaunchedEffect(videoState.syncOperations.toTypedArray()) {
            for (syncOperation in videoState.syncOperations) {
                syncOperation.invoke(videoState)
            }
            videoState.syncOperations.clear()
        }

        DisposableEffect(Unit) {
            onDispose {
                SyncOperation.Close.invoke(videoState)
            }
        }
    }
}
