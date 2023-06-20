@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.common

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent

val PointerEvent.isLeftClick: Boolean
    get() = this.button == PointerButton.Primary

val PointerEvent.isRightClick: Boolean
    get() = this.button == PointerButton.Secondary
