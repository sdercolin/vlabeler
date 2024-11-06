@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.KeyEvent
import com.sdercolin.vlabeler.env.released
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.key.KeySet
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame

private var window: ComposeWindow? = null

/**
 * Composable for displaying the video player in a new window.
 */
@Composable
fun NewWindowVideo(videoState: VideoState, appConf: AppConf) {
    val keymap = appConf.keymaps.keyActionMap
    DisposableEffect(Unit) {
        window = ComposeWindow().apply {
            size = Dimension(videoState.width.value.toInt(), videoState.height.value.toInt())
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            addWindowListener(
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        videoState.exit()
                    }
                },
            )
            setContent(
                onKeyEvent = {
                    if (it.released) {
                        // We have to catch `down` event here, otherwise it sometimes fires again with the input to open
                        // this window from the window's menu, which leads to unexpected closing.
                        return@setContent false
                    }
                    if (KeyAction.ToggleVideoPopupNewWindow.shouldCatch(keymap, it)) {
                        videoState.exit()
                        return@setContent true
                    }
                    /* Disabled due to compose UI error
                    if (KeyAction.ToggleVideoPopupEmbedded.shouldCatch(keymap, it)) {
                        videoState.setEmbeddedMode()
                        return@setContent true
                    }*/
                    false
                },
            ) {
                VideoPanel(videoState, Modifier.fillMaxSize())
            }
            isVisible = true
        }
        onDispose { window?.dispose() }
    }
}

private fun KeyAction.shouldCatch(keymap: Map<KeyAction, KeySet?>, keyEvent: KeyEvent): Boolean {
    val keySet = keymap[this] ?: defaultKeySet ?: return false
    return keySet.shouldCatch(keyEvent)
}
