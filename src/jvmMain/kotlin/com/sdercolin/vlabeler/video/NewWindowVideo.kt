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
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame

private var window: ComposeWindow? = null

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewWindowVideo(videoState: VideoState) {
    DisposableEffect(Unit) {
        window = ComposeWindow().apply {
            size = Dimension(videoState.width.value.toInt(), videoState.height.value.toInt())
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            addWindowListener(
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        videoState.closeManually()
                    }
                },
            )
            setContent(
                onKeyEvent = {
                    if (
                        it.type == KeyEventType.KeyDown &&
                        KeyAction.ToggleVideoPopupEmbedded.defaultKeySet?.shouldCatch(
                            it,
                            false,
                        ) == true
                    ) {
                        videoState.mode = VideoState.Mode.Embedded
                    }
                    true
                },
            ) {
                VideoCore(videoState, Modifier.fillMaxSize())
            }
            isVisible = true
        }
        onDispose { window?.apply { dispose() } }
    }
}
