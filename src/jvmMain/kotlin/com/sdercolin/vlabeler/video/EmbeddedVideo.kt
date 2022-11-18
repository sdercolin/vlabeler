package com.sdercolin.vlabeler.video

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import java.awt.Cursor

@Composable
fun embeddedMode(videoState: VideoState) {
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(13, -75),
    ) {
        Box( // slightly larger area for resizing
            modifier = Modifier
                .size(videoState.width + 5.dp, videoState.height)
                .run {
                    this.draggable(
                        state = rememberDraggableState {
                        },
                        orientation = Orientation.Horizontal,
                        startDragImmediately = false,
                        onDragStopped = { videoState.onResizeWidth(it, density) }
                    ).pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                }
                .background(Color.Transparent)
        ) {
            videoState.Core()
        }
    }
}

fun VideoState.onResizeWidth(delta: Float, density: Density) {
        runCatching {
            with(density) {
                setWidthProportional(width + delta.toDp())
                rememberSize()
                syncTime()
            }
        }.onFailure {
        }
}

fun VideoState.setWidthProportional(newWidth: Dp): VideoState {
    width = newWidth
        .coerceAtLeast(VideoState.MinWidth)
        .coerceAtMost(VideoState.MaxWidth)
    height = width * 3 / 4
    log("resize width to", width)
    return this
}
