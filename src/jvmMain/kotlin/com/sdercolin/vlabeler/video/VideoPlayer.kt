// see https://github.com/JetBrains/compose-jb/pull/1738 for direct control of awt/swing
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.sdercolin.vlabeler.video

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isMacOS
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.InputEvents
import java.awt.BorderLayout
import javax.swing.JPanel

class MiniVideo() {
    var mediaPlayerComponent: Any? = null
    lateinit var mediaPlayer: MediaPlayer
    var lastTime: Long? = null
        private set
    lateinit var toggleCallback: (on: Boolean) -> Unit

    fun init(toggleCallback: (on: Boolean) -> Unit) {
        Log.info("VideoPlayer init")

        NativeDiscovery().discover()
        // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
        mediaPlayerComponent = when {
            isMacOS -> CallbackMediaPlayerComponent(null, null, InputEvents.NONE, true, null)
            else -> EmbeddedMediaPlayerComponent(null, null, null, InputEvents.NONE, null)
        }
        mediaPlayer = getMediaPlayer(mediaPlayerComponent!!)

        this.toggleCallback = toggleCallback
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

    fun saveTime(reset: Boolean = false) {
        lastTime = if (reset) null else mediaPlayer.status().time()
    }
}

/**
 * A lightweight version of androidx.compose.ui.awt.SwingPanel
 * used to display the video player. Slightly faster and fewer bugs
 */
@Composable
fun VideoState.Core(modifier: Modifier) {
    val density = LocalDensity.current.density
    Box(
        modifier = modifier
            .onGloballyPositioned { childCoordinates ->
                log("applying resize...")

                val coordinates = childCoordinates.parentCoordinates!!
                val location = coordinates.localToWindow(Offset.Zero).round()
                val size = coordinates.size
                container.setBounds(
                    (location.x / density).toInt(),
                    (location.y / density).toInt(),
                    (size.width / density).toInt(),
                    (size.height / density).toInt(),
                )
                container.validate()
            },
    )

    val root = LocalLayerContainer.current
    DisposableEffect(Unit) {
        container = JPanel().apply {
            layout = BorderLayout()
            add(
                if (isMacOS)
                    miniVideo.mediaPlayerComponent as CallbackMediaPlayerComponent
                else
                    miniVideo.mediaPlayerComponent as EmbeddedMediaPlayerComponent,
            )
            isVisible = true
        }
        root.add(container)

        onDispose {
            runCatching { root.remove(container) }
        }
    }

    SideEffect {
        syncOp(newestTime())
    }
}
