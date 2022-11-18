package com.sdercolin.vlabeler.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.action.KeyAction
import java.awt.Dimension
import javax.swing.JFrame

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun newWindowMode(videoState: VideoState) {
    DisposableEffect(Unit) {
        videoState
            .log("open video in new window:")
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
                            videoState.embeddedMode = true
                        }
                        true
                    },
                ) {
                    videoState.Core()
                }
                isVisible = true
            }
        onDispose { videoState.closeWindow() }
    }
}

fun VideoState.newWindow(block: ComposeWindow.() -> Unit): VideoState {
    window = ComposeWindow().apply(block)
    return this
}

fun VideoState.closeWindow(): VideoState {
    Log.info("window close")
    window?.dispose()
    return this
}
