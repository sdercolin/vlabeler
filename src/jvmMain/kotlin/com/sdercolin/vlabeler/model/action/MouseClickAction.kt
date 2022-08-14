package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.getNullableOrElse

enum class MouseClickAction(val displayedName: Strings, val defaultKeySet: KeySet?) : Action {
    MoveParameter(Strings.ActionMoveParameter, KeySet.None),
    MoveParameterWithPlaybackPreview(Strings.ActionMoveParameterWithPlaybackPreview, KeySet.subKeys(Key.Alt)),
    MoveParameterInvertingPrimary(Strings.ActionMoveParameterInvertingPrimary, KeySet.subKeys(Key.Shift)),
    PlayAudioSection(Strings.ActionPlayAudioSection, KeySet.subKeys(Key.Ctrl));

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
            .flatMap { map -> map.value.sortedByDescending { it.first.subKeys.minus(Key.None).count() } }
    }
}

fun MouseClickAction?.canMoveParameter(): Boolean =
    this in listOf(
        MouseClickAction.MoveParameter,
        MouseClickAction.MoveParameterWithPlaybackPreview,
        MouseClickAction.MoveParameterInvertingPrimary
    )
