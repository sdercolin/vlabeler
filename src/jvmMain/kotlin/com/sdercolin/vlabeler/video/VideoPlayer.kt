package com.sdercolin.vlabeler.video

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isMacOS
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import java.awt.Component

data class MediaPlayerComponent<T : Component>(val component: T)

class MiniVideo {
    var mediaPlayerComponent: MediaPlayerComponent<*>? = null
        private set
    private lateinit var mediaPlayer: MediaPlayer
    val currentTime
        get() = mediaPlayer.status().time()

    fun init() {
        Log.info("VideoPlayer init")

        NativeDiscovery().discover()
        if (isMacOS) {
            with(CallbackMediaPlayerComponent(null, null, InputEvents.NONE, true, null)) {
                mediaPlayerComponent = MediaPlayerComponent(this)
                mediaPlayer = mediaPlayer()
            }
        } else {
            with(EmbeddedMediaPlayerComponent(null, null, null, InputEvents.NONE, null)) {
                mediaPlayerComponent = MediaPlayerComponent(this)
                mediaPlayer = mediaPlayer()
            }
        }
    }

    fun load(url: String): MiniVideo {
        mediaPlayer.media().startPaused(url)
        Log.info("VideoPlayer loaded file \"$url\"")
        return this
    }

    fun startAt(time: Long): MiniVideo {
        mediaPlayer.controls().setTime(time)
        Log.info("VideoPlayer play at ${time}ms")
        return this
    }

    fun pause(): MiniVideo {
        mediaPlayer.controls().setPause(true)
        return this
    }

    fun play(): MiniVideo {
        mediaPlayer.controls().setPause(false)
        return this
    }

    fun mute(): MiniVideo {
        mediaPlayer.audio().isMute = true
        return this
    }
}
