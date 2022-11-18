// see https://github.com/JetBrains/compose-jb/pull/1738 for direct control of awt/swing
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.sdercolin.vlabeler.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.util.or
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import java.awt.BorderLayout
import java.awt.Container
import java.io.File
import javax.swing.JPanel

class MiniVideo() {
    var mediaPlayerComponent: Any? = null
    lateinit var mediaPlayer: MediaPlayer
    var loaded: Boolean by mutableStateOf(false)
    lateinit var container: Container

    fun Init() {
        NativeDiscovery().discover()
        // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
        mediaPlayerComponent = when {
            isMacOS -> CallbackMediaPlayerComponent(null, null, InputEvents.NONE, true, null)
            else -> EmbeddedMediaPlayerComponent(null, null, null, InputEvents.NONE, null)
        }
        mediaPlayer = getMediaPlayer(mediaPlayerComponent!!)
        Log.info("VideoPlayer init")
    }

    @Composable
    fun start(file: File, getSyncTime: () -> Long? = { 0 }, visible: Boolean = true) {
        val mrl: String = file.absolutePath
        start(mrl, getSyncTime, visible)
    }

    @Composable
    fun start(url: String, getSyncTime: () -> Long? = { 0 }, visible: Boolean = true) {
        LaunchedEffect(Unit) {
            // TODO: complete the logic
            if (!loaded) {
                mediaPlayer.audio().isMute = true
                mediaPlayer.media().startPaused(url)
                loaded = true
                Log.info("VideoPlayer load file \"$url\"")
            } else getSyncTime()?.let {
                mediaPlayer.media().play(url)
                mediaPlayer.controls().setTime(it)
                Log.info("VideoPlayer reload file \"$url\"")
            }
        }

        val root = LocalLayerContainer.current
        DisposableEffect(Unit) {
            Log.info("Video Player create ui")
            container = JPanel().apply {
                layout = BorderLayout()
                add(
                    if (isMacOS)
                        mediaPlayerComponent as CallbackMediaPlayerComponent
                    else
                        mediaPlayerComponent as EmbeddedMediaPlayerComponent
                )
                isVisible = visible
            }
            root.add(container)
            onDispose {
                mediaPlayer.controls().pause()
                runCatching { root.remove(container) }
                Log.info("VideoPlayer disabled")
            }
        }
    }

    /**
     * To return mediaPlayer from player components.
     * The method names are same, but they don't share the same parent/interface.
     * That's why need this method.
     */
    private fun getMediaPlayer(component: Any): MediaPlayer =
        when (component) {
            is CallbackMediaPlayerComponent -> component.mediaPlayer()
            is EmbeddedMediaPlayerComponent -> component.mediaPlayer()
            else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
        }
}

@Composable
fun VideoState.Core() {
    val density = LocalDensity.current.density
    Box(
        modifier = Modifier
            .size(width, height)
            .onGloballyPositioned { childCoordinates ->
                val coordinates = childCoordinates.parentCoordinates!!
                val location = coordinates.localToWindow(Offset.Zero).round()
                val size = coordinates.size
                miniVideo.container.setBounds(
                    (location.x / density).toInt(),
                    (location.y / density).toInt(),
                    (size.width / density).toInt(),
                    (size.height / density).toInt(),
                )
                log("applying resize...")
                if (miniVideo.loaded) {
                    miniVideo.container.validate()
                    miniVideo.container.isVisible = true
                    log("applying resize successful")
                }
            }
    )
    miniVideo.start(videoPath, { getSyncTime() }, true)
}
