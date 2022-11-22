package com.sdercolin.vlabeler.video

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.or

enum class SyncCause {
    // upon popup
    OpenWhenIdle,
    OpenDuringPlay,
    RecoverFromLastExit,

    // after popup
    PlayerStartPlay,
    PlayerPause,
    ChangeEmbeddingMode,
    ResizeWindow,
    CloseWhenIdle,
    CloseDuringPlay,

    Nothing,
}

fun coveredBy(vararg causes: SyncCause) : Nothing = TODO("this case is covered by $causes, do not use it directly")

/**
 * might perform a sync operation immediately, and/or
 * returns a sync operation to perform later
 */
fun VideoState.sync(cause: SyncCause, time: Long? = null): (time: Long?) -> Unit {
    log("sync caused by $cause")

    when (cause) {
        SyncCause.OpenWhenIdle -> coveredBy(SyncCause.RecoverFromLastExit)
        SyncCause.OpenDuringPlay -> {
            require(time != null) { "no time to play at" }
            return { latestTime -> miniVideo.load(videoPath).mute().startAt(latestTime.or(time)).play() }
        }
        SyncCause.RecoverFromLastExit -> {
            return { miniVideo.load(videoPath).mute().startAt(miniVideo.lastTime ?: 0).play() } // use play() because pause() causes bug, doesn't affect much
        }

        SyncCause.PlayerStartPlay -> {
            require(time != null) {"no time to play at"}
            miniVideo.play().startAt(time)
            return {}
        }
        SyncCause.PlayerPause -> coveredBy(SyncCause.CloseDuringPlay)
        SyncCause.ChangeEmbeddingMode -> coveredBy(SyncCause.OpenDuringPlay, SyncCause.RecoverFromLastExit)
        SyncCause.ResizeWindow -> {
            return {}
        }
        SyncCause.CloseWhenIdle -> {
            miniVideo.saveTime(true)
            return {}
        }
        SyncCause.CloseDuringPlay -> {
            miniVideo.pause().saveTime()
            return {}
        }
        SyncCause.Nothing -> {
            return {}
        }
    }
}

fun MiniVideo.load(url: String): MiniVideo {
    mediaPlayer.media().startPaused(url)
    Log.info("VideoPlayer loaded file \"$url\"")
    return this
}

fun MiniVideo.startAt(time: Long): MiniVideo {
    mediaPlayer.controls().setTime(time)
    Log.info("VideoPlayer play at ${time}ms")
    return this
}

fun MiniVideo.pause(): MiniVideo {
    mediaPlayer.controls().setPause(true)
    return this
}

fun MiniVideo.play(): MiniVideo {
    mediaPlayer.controls().setPause(false)
    return this
}

fun MiniVideo.mute(): MiniVideo {
    mediaPlayer.audio().isMute = true
    return this
}

