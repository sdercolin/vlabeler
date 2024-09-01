package com.sdercolin.vlabeler.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.BorderLayout
import javax.swing.JPanel

@Composable
fun VideoPanel(videoState: VideoState, modifier: Modifier) {
    SwingPanel(
        modifier = modifier,
        factory = {
            JPanel().apply {
                layout = BorderLayout()
                videoState.videoPlayer.mediaPlayerComponent?.component?.let { add(it) }
                isVisible = true
            }
        },
    )
}
