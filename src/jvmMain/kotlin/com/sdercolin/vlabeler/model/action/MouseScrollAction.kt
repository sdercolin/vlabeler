package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.getNullableOrElse

enum class MouseScrollAction(
    val displayedName: Strings,
    val defaultKeySet: KeySet?,
    val editable: Boolean = true
) : Action {
    SwitchSample(Strings.ActionSwitchSample, KeySet.subKeys(Key.Ctrl)),
    SwitchEntry(Strings.ActionSwitchEntry, KeySet.None),
    ZoomCanvas(Strings.ActionZoomCanvas, KeySet.subKeys(Key.Shift, Key.Ctrl)),
    ScrollCanvas(Strings.ActionScrollCanvas, KeySet.subKeys(Key.Shift), editable = false);

    override val displayOrder: Int
        get() = values().indexOf(this)

    override val title: String
        get() = string(displayedName)

    companion object {

        fun getKeySets(keymaps: AppConf.Keymaps): List<Pair<KeySet, MouseScrollAction>> = MouseScrollAction.values()
            .mapNotNull { action ->
                val keySet = keymaps.mouseScrollActionMap.getNullableOrElse(action) { action.defaultKeySet }
                keySet?.let { it to action }
            }
            .groupBy { it.first.mainKey }
            .flatMap { map -> map.value.sortedByDescending { it.first.subKeys.minus(Key.None).count() } }
    }
}
