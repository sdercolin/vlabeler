package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import com.sdercolin.vlabeler.util.getNullableOrElse

/**
 * Action that is triggered by mouse scroll.
 *
 * @property displayedName The name that is displayed in the UI.
 * @property defaultKeySet The default key set that triggers this action.
 * @property editable Whether the key set is editable.
 */
enum class MouseScrollAction(
    val displayedName: Strings,
    val defaultKeySet: KeySet?,
    val editable: Boolean = true,
) : Action {
    GoToNextSample(Strings.ActionGoToNextSample, KeySet(Key.MouseScrollDown, setOf(Key.Ctrl))),
    GoToPreviousSample(Strings.ActionGoToPreviousSample, KeySet(Key.MouseScrollUp, setOf(Key.Ctrl))),
    GoToNextEntry(Strings.ActionGoToNextEntry, KeySet(Key.MouseScrollDown)),
    GoToPreviousEntry(Strings.ActionGoToPreviousEntry, KeySet(Key.MouseScrollUp)),
    ZoomInCanvas(Strings.ActionZoomInCanvas, KeySet(Key.MouseScrollUp, setOf(Key.Shift, Key.Ctrl))),
    ZoomOutCanvas(Strings.ActionZoomOutCanvas, KeySet(Key.MouseScrollDown, setOf(Key.Shift, Key.Ctrl))),
    ScrollLeftCanvas(Strings.ActionScrollCanvasLeft, KeySet(Key.MouseScrollUp, setOf(Key.Shift)), editable = false),
    ScrollRightCanvas(Strings.ActionScrollCanvasRight, KeySet(Key.MouseScrollDown, setOf(Key.Shift)), editable = false),
    ;

    override val displayOrder: Int
        get() = entries.indexOf(this)

    override fun getTitle(language: Language): String = stringCertain(displayedName, language)

    companion object {

        fun getKeySets(keymaps: AppConf.Keymaps): List<Pair<KeySet, MouseScrollAction>> = MouseScrollAction.entries
            .mapNotNull { action ->
                val keySet = keymaps.mouseScrollActionMap.getNullableOrElse(action) { action.defaultKeySet }
                keySet?.let { it to action }
            }
            .groupBy { it.first.mainKey }
            .flatMap { map -> map.value.sortedByDescending { it.first.subKeys.count() } }
    }
}
