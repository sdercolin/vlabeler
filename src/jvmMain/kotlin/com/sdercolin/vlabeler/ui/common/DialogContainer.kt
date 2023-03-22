package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.util.runIf

@Composable
private fun DialogContainer(
    size: DialogContainerSize,
    wrapWidth: Boolean,
    wrapHeight: Boolean,
    onClickOutside: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50).plainClickable(onClickOutside),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.wrapContentSize()
                .runIf(!wrapHeight) { fillMaxHeight(size.fraction) }
                .runIf(!wrapWidth) { fillMaxWidth(size.fraction) }
                .plainClickable(),
        ) {
            content()
        }
    }
}

private enum class DialogContainerSize(val fraction: Float) {
    Small(0.5f),
    Medium(0.7f),
    Large(0.8f),
}

@Composable
fun SmallDialogContainer(
    wrapWidth: Boolean = false,
    wrapHeight: Boolean = false,
    onClickOutside: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    DialogContainer(DialogContainerSize.Small, wrapWidth, wrapHeight, onClickOutside, content)
}

@Composable
fun MediumDialogContainer(
    wrapWidth: Boolean = false,
    wrapHeight: Boolean = false,
    onClickOutside: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    DialogContainer(DialogContainerSize.Medium, wrapWidth, wrapHeight, onClickOutside, content)
}

@Composable
fun LargeDialogContainer(
    wrapWidth: Boolean = false,
    wrapHeight: Boolean = false,
    onClickOutside: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    DialogContainer(DialogContainerSize.Large, wrapWidth, wrapHeight, onClickOutside, content)
}
