package com.sdercolin.vlabeler.video

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isMacOS
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import java.awt.Component

class MediaPlayerComponent<T : Component>(val component: T)

/**
 * A wrapper of vlcj MediaPlayer.
 */
class VideoPlayer {
    var mediaPlayerComponent: MediaPlayerComponent<*>? = null
        private set
    private var mediaPlayer: MediaPlayer? = null
    val currentTime
        get() = mediaPlayer?.status()?.time()

    fun init() = runCatching {
        Log.info("VideoPlayer init")

        NativeDiscovery().discover()
        if (isMacOS) {
            val component = CallbackMediaPlayerComponent(null, null, InputEvents.NONE, true, null)
            mediaPlayerComponent = MediaPlayerComponent(component)
            mediaPlayer = component.mediaPlayer()
        } else {
            val component = EmbeddedMediaPlayerComponent(null, null, null, InputEvents.NONE, null)
            mediaPlayerComponent = MediaPlayerComponent(component)
            mediaPlayer = component.mediaPlayer()
        }
    }.onFailure {
        mediaPlayerComponent = null
        mediaPlayer = null
        Log.error(it)
    }

    fun load(url: String): VideoPlayer {
        mediaPlayer?.let {
            it.media().startPaused(url)
            Log.info("VideoPlayer loaded file \"$url\"")
        }
        return this
    }

    fun startAt(time: Long): VideoPlayer {
        mediaPlayer?.let {
            it.controls().setTime(time)
            Log.info("VideoPlayer play at ${time}ms")
        }
        return this
    }

    fun pause(): VideoPlayer {
        mediaPlayer?.controls()?.setPause(true)
        return this
    }

    fun play(): VideoPlayer {
        mediaPlayer?.controls()?.setPause(false)
        return this
    }

    fun mute(): VideoPlayer {
        mediaPlayer?.audio()?.isMute = true
        return this
    }
}
