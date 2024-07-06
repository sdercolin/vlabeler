@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model.key

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.window.MenuBarScope
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.env.isNativeCtrlPressed
import com.sdercolin.vlabeler.env.isNativeMetaPressed
import com.sdercolin.vlabeler.env.released
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A serializable set of [Key]s.
 *
 * @property mainKey The main key of the set. If this is null, the set is incomplete.
 * @property subKeys The sub keys of the set.
 */
@Serializable(with = KeySet.KeySetSerializer::class)
@Immutable
data class KeySet(
    val mainKey: Key?,
    val subKeys: Set<Key> = setOf(),
) {

    fun isValid(): Boolean {
        if (mainKey != null && mainKey.isMainKey.not()) return false
        if (subKeys.distinct().size != subKeys.size) return false
        if (subKeys.any { it.isMainKey }) return false
        if (mainKey == null && subKeys.isEmpty()) return false
        return true
    }

    val isComplete get() = mainKey != null

    val displayedKeyName: String
        get() {
            val names = mutableListOf<String>()
            names += subKeys.toList().sortedBy { Key.entries.indexOf(it) }.map { it.displayedName }
            if (mainKey != null) names += mainKey.displayedName
            return names.joinToString(if (isMacOS) "" else "+")
        }

    private val hasCtrl = subKeys.contains(Key.Ctrl)
    private val hasAlt = subKeys.contains(Key.Alt)
    private val hasShift = subKeys.contains(Key.Shift)
    private val hasWin = subKeys.contains(Key.Windows)
    private val hasMappedCtrl = if (isMacOS) hasWin else hasCtrl
    private val hasMappedWin = if (isMacOS) hasCtrl else hasWin

    /**
     * Convert this [KeySet] to a [KeyShortcut] used by Compose [MenuBarScope].
     */
    fun toShortCut(): KeyShortcut? {
        if (isValid().not()) return null
        val mainKey = mainKey?.actualKeys?.firstOrNull() ?: return null

        return KeyShortcut(
            mainKey,
            ctrl = hasMappedCtrl,
            alt = hasAlt,
            shift = hasShift,
            meta = hasMappedWin,
        )
    }

    /**
     * Check if this [KeySet] should catch the given [KeyEvent].
     */
    fun shouldCatch(event: KeyEvent): Boolean {
        if (mainKey != null) {
            if (event.key !in mainKey.actualKeys) return false
        }
        if (hasMappedWin != event.isMetaPressed) return false
        if (hasMappedCtrl != event.isCtrlPressed) return false
        if (hasAlt != event.isAltPressed) return false
        if (hasShift != event.isShiftPressed) return false
        return true
    }

    object KeySetSerializer : KSerializer<KeySet> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("KeySet", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): KeySet {
            val text = decoder.decodeString()
            val keys = text.split("+").map { Key.valueOf(it) }
            val mainKey = keys.firstOrNull { it.isMainKey }
            val subKeys = keys.filter { it.isMainKey.not() }.toSet()
            return KeySet(mainKey, subKeys)
        }

        override fun serialize(encoder: Encoder, value: KeySet) {
            val subKeysText = value.subKeys.toList()
                .sortedBy { Key.entries.indexOf(it) }
                .joinToString("+") { it.name }
                .ifEmpty { null }
            val mainKeyText = value.mainKey?.name?.ifEmpty { null }
            val text = listOfNotNull(subKeysText, mainKeyText).joinToString("+")
            encoder.encodeString(text)
        }
    }

    companion object {

        /**
         * Create a [KeySet] from a [KeyEvent].
         */
        fun fromKeyEvent(keyEvent: KeyEvent): KeySet {
            val mainKey = Key.fromActualKey(keyEvent.key).takeUnless { keyEvent.released }
            val subKeys = mutableSetOf<Key>()
            if (keyEvent.isNativeCtrlPressed || mainKey == Key.Ctrl) {
                subKeys += Key.Ctrl
            }
            if (keyEvent.isNativeMetaPressed || mainKey == Key.Windows) {
                subKeys += Key.Windows
            }
            if (keyEvent.isShiftPressed || mainKey == Key.Shift) {
                subKeys += Key.Shift
            }
            if (keyEvent.isAltPressed || mainKey == Key.Alt) {
                subKeys += Key.Alt
            }
            return KeySet(mainKey?.takeIf { it.isMainKey }, subKeys)
        }
    }
}
