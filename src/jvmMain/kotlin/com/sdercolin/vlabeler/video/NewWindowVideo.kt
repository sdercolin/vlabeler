package com.sdercolin.vlabeler.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.sdercolin.vlabeler.model.action.KeyAction
import java.awt.Dimension
import javax.swing.JFrame

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun newWindowMode(videoState: VideoState) {
    DisposableEffect(Unit) {
        videoState
            .closeWindow()
            .newWindow {
                size = Dimension(videoState.width.value.toInt(), videoState.height.value.toInt())
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                setContent(
                    onKeyEvent = {
                        if (
                            it.type == KeyEventType.KeyDown &&
                            KeyAction.ToggleVideoPopupEmbedded.defaultKeySet?.shouldCatch(
                                it, false,
                            ) == true
                        ) {
                            videoState.setEmbedded(true)
                        }
                        true
                    },
                ) {
                    videoState.Core(Modifier.fillMaxSize())
                }
                isVisible = true
            }
        onDispose { videoState.closeWindow() }
    }
}

fun VideoState.newWindow(block: ComposeWindow.() -> Unit): VideoState {
    log("window open")
    window = ComposeWindow().apply(block)
    return this
}

fun VideoState.closeWindow(): VideoState {
    window?.run{
        log("window close")
        dispose()
    }
    return this
}
