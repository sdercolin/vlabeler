package com.sdercolin.vlabeler.model.action

import androidx.compose.ui.input.pointer.PointerEventType
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.getNullableOrElse

enum class MouseClickAction(
    val displayedName: Strings,
    val defaultKeySet: KeySet?,
    val pointerEventType: PointerEventType
) : Action {
    MoveParameter(Strings.ActionMoveParameter, KeySet(Key.MouseLeftClick), PointerEventType.Press),
    MoveParameterWithPlaybackPreview(
        Strings.ActionMoveParameterWithPlaybackPreview,
        KeySet(Key.MouseLeftClick, setOf(Key.Alt)),
        PointerEventType.Press
    ),
    MoveParameterInvertingPrimary(
        Strings.ActionMoveParameterInvertingPrimary,
        KeySet(Key.MouseLeftClick, setOf(Key.Shift)),
        PointerEventType.Press
    ),
    PlayAudioSection(Strings.ActionPlayAudioSection, KeySet(Key.MouseRightClick), PointerEventType.Release);

    override val displayOrder: Int
        get() = values().indexOf(this)

    override val title: String
        get() = string(displayedName)

    companion object {

        fun getKeySets(keymaps: AppConf.Keymaps): List<Pair<KeySet, MouseClickAction>> = MouseClickAction.values()
            .mapNotNull { action ->
                val keySet = keymaps.mouseClickActionMap.getNullableOrElse(action) { action.defaultKeySet }
                keySet?.let { it to action }
            }
            .groupBy { it.first.mainKey }
            .flatMap { map -> map.value.sortedByDescending { it.first.subKeys.count() } }
    }
}

fun MouseClickAction?.canMoveParameter(): Boolean =
    this in listOf(
        MouseClickAction.MoveParameter,
        MouseClickAction.MoveParameterWithPlaybackPreview,
        MouseClickAction.MoveParameterInvertingPrimary
    )
