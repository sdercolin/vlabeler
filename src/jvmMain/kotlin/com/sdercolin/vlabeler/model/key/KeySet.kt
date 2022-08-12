package com.sdercolin.vlabeler.model.key

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.KeyShortcut
import com.sdercolin.vlabeler.env.isMacOS
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

    fun toShortCut(): KeyShortcut? {
        if (isValid().not()) return null
        val mainKey = mainKey ?: return null
        val hasCtrl = subKeys.contains(Key.Ctrl)
        val hasAlt = subKeys.contains(Key.Alt)
        val hasShift = subKeys.contains(Key.Shift)
        val hasWin = subKeys.contains(Key.Windows)
        val hasMappedCtrl = if (isMacOS) hasWin else hasCtrl
        val hasMappedWin = if (isMacOS) hasCtrl else hasWin
        return KeyShortcut(
            mainKey.actualKeys.first(),
            ctrl = hasMappedCtrl,
            alt = hasAlt,
            shift = hasShift,
            meta = hasMappedWin
        )
    }
}
