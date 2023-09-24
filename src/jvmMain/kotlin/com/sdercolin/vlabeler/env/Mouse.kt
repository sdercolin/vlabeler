@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.env

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import com.sdercolin.vlabeler.model.key.Key

/**
 * Converts a [PointerEvent] to a [Key] if it is a mouse click or scroll event.
 */
fun PointerEvent.toVirtualKey(): Key? {
    return when (type) {
        PointerEventType.Press, PointerEventType.Release -> when (button) {
            PointerButton.Primary -> {
                Key.MouseLeftClick
            }
            PointerButton.Secondary -> {
                Key.MouseRightClick
            }
            else -> null
        }
        PointerEventType.Scroll -> {
            val change = changes.firstOrNull()?.scrollDelta ?: return null
            if (change.x != 0f && change.y != 0f) {
                Log.debug("Warning: getting a mouse scroll event with both x:${change.x} and y:${change.y} changes")
            }
            when {
                change.x > 0 -> {
                    Key.MouseScrollDown
                }
                change.x < 0 -> {
                    Key.MouseScrollUp
                }
                change.y > 0 -> {
                    Key.MouseScrollDown
                }
                change.y < 0 -> {
                    Key.MouseScrollUp
                }
                else -> null
            }
        }
        else -> null
    }
}
