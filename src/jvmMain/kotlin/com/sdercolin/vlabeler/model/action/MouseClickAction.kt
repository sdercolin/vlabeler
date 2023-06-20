package com.sdercolin.vlabeler.model.action

import androidx.compose.ui.input.pointer.PointerEventType
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import com.sdercolin.vlabeler.util.getNullableOrElse

/**
 * Action that is triggered by mouse click.
 */
enum class MouseClickAction(
    val displayedName: Strings,
    val defaultKeySet: KeySet?,
    val pointerEventType: PointerEventType,
    val tool: Tool = Tool.Cursor,
) : Action {
    MoveParameter(Strings.ActionMoveParameter, KeySet(Key.MouseLeftClick), PointerEventType.Press),
    MoveParameterWithPlaybackPreview(
        Strings.ActionMoveParameterWithPlaybackPreview,
        KeySet(Key.MouseLeftClick, setOf(Key.Alt)),
        PointerEventType.Press,
    ),
    MoveParameterIgnoringConstraints(
        Strings.ActionMoveParameterIgnoringConstraints,
        KeySet(Key.MouseLeftClick, setOf(Key.Ctrl)),
        PointerEventType.Press,
    ),
    MoveParameterInvertingPrimary(
        Strings.ActionMoveParameterInvertingLocked,
        KeySet(Key.MouseLeftClick, setOf(Key.Shift)),
        PointerEventType.Press,
    ),
    PlayAudioSection(Strings.ActionPlayAudioSection, KeySet(Key.MouseRightClick), PointerEventType.Release),
    PlayAudioUntilEnd(
        Strings.ActionPlayAudioUntilEnd,
        KeySet(Key.MouseLeftClick),
        PointerEventType.Release,
        Tool.Playback,
    ),
    PlayAudioUntilScreenEnd(
        Strings.ActionPlayAudioUntilScreenEnd,
        KeySet(Key.MouseRightClick),
        PointerEventType.Release,
        Tool.Playback,
    ),
    PlayAudioFromStart(
        Strings.ActionPlayAudioFromStart,
        KeySet(Key.MouseLeftClick, setOf(Key.Shift)),
        PointerEventType.Release,
        Tool.Playback,
    ),
    PlayAudioFromScreenStart(
        Strings.ActionPlayAudioFromScreenStart,
        KeySet(Key.MouseRightClick, setOf(Key.Shift)),
        PointerEventType.Release,
        Tool.Playback,
    ),
    ;

    override val displayOrder: Int
        get() = values().indexOf(this)

    override fun getTitle(language: Language): String = stringCertain(displayedName, language)

    override val conflictGroupHash: Int = tool.hashCode()

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
        MouseClickAction.MoveParameterInvertingPrimary,
        MouseClickAction.MoveParameterIgnoringConstraints,
    )
