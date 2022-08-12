package com.sdercolin.vlabeler.model.key

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.env.released
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class KeySet(
    val mainKey: Key?,
    val subKeys: Set<Key> = setOf()
) {

    fun isValid(): Boolean {
        if (mainKey != null && mainKey.isMainKey.not()) return false
        if (subKeys.distinct().size != subKeys.size) return false
        if (subKeys.any { it.isMainKey }) return false
        return true
    }

    val isComplete get() = mainKey != null

    private val hasCtrl = subKeys.contains(Key.Ctrl)
    private val hasAlt = subKeys.contains(Key.Alt)
    private val hasShift = subKeys.contains(Key.Shift)
    private val hasWin = subKeys.contains(Key.Windows)
    private val hasMappedCtrl = if (isMacOS) hasWin else hasCtrl
    private val hasMappedWin = if (isMacOS) hasCtrl else hasWin

    fun toShortCut(): KeyShortcut? {
        if (isValid().not()) return null
        val mainKey = mainKey ?: return null

        return KeyShortcut(
            mainKey.actualKeys.first(),
            ctrl = hasMappedCtrl,
            alt = hasAlt,
            shift = hasShift,
            meta = hasMappedWin
        )
    }

    fun shouldCatch(event: KeyEvent): Boolean {
        if (event.released.not()) return false
        if (mainKey != null) {
            if (event.key !in mainKey.actualKeys) return false
        }
        if (hasMappedWin && !event.isMetaPressed) return false
        if (hasMappedCtrl && !event.isCtrlPressed) return false
        if (hasAlt && !event.isAltPressed) return false
        if (hasShift && !event.isShiftPressed) return false
        return true
    }
}
