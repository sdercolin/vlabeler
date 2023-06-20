package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.canMoveParameter
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.isLeftClick
import com.sdercolin.vlabeler.ui.common.isRightClick
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.contains
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.getScreenRange
import com.sdercolin.vlabeler.util.length
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

const val RegionAlpha = 0.3f
val EditableOutsideRegionColor = White
const val UneditableRegionAlpha = 0.9f
val UneditableRegionColor = Black
const val IdleLineAlpha = 0.7f
const val StrokeWidth = 2f
val LabelSize = DpSize(40.dp, 25.dp)
val LabelShiftUp = 11.dp
const val LabelMaxChunkLength = 5000

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MarkerPointEventContainer(
    screenRange: FloatRange?,
    keyboardState: KeyboardState,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState,
    content: @Composable BoxScope.() -> Unit,
) {
    val tool = editorState.tool
    val playByCursor = remember(appState.appConf.playback.playOnDragging, appState.player) {
        if (appState.appConf.playback.playOnDragging.enabled) {
            { appState.player.playByCursor(it) }
        } else {
            { _: Float -> }
        }
    }
    val playSection = remember(appState.player) {
        { start: Float, end: Float? -> appState.player.playSection(start, end) }
    }
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Move) { event ->
                if (tool == Tool.Cursor &&
                    state.cursorState.value.mouse == MarkerCursorState.Mouse.Dragging &&
                    event.buttons.areAnyPressed.not()
                ) {
                    state.handleMouseRelease(
                        tool,
                        event,
                        editorState::submitEntries,
                        playSection,
                        editorState::cutEntry,
                        keyboardState,
                        screenRange,
                    )
                    return@onPointerEvent
                }
                state.handleMouseMove(
                    tool,
                    event,
                    editorState::updateEntries,
                    screenRange,
                    playByCursor,
                    horizontalScrollState,
                    coroutineScope,
                )
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                state.handleMousePress(tool, keyboardState, event, state.labelerConf, appState.appConf)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                state.handleMouseRelease(
                    tool,
                    event,
                    editorState::submitEntries,
                    appState.player::playSection,
                    editorState::cutEntry,
                    keyboardState,
                    screenRange,
                )
            },
        content = content,
    )
}

@Composable
fun MarkerCanvas(
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState,
) {
    FieldBorderCanvas(horizontalScrollState, state, appState.appConf.editor)
    LaunchAdjustScrollPosition(
        appState.appConf,
        state.entriesInPixel,
        editorState.project.currentModuleIndex,
        editorState.project.currentModule.currentIndex,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel,
    )
    LaunchedEffect(horizontalScrollState.maxValue, horizontalScrollState.value) {
        editorState.scrollOnResolutionChangeViewModel.scroll(horizontalScrollState)
    }
    LaunchedEffect(editorState.tool) {
        state.switchTool(editorState.tool)
    }
    LaunchedEffect(editorState.keyboardViewModel, state) {
        editorState.keyboardViewModel.keyboardActionFlow.collectLatest {
            if (appState.isEditorActive.not()) return@collectLatest
            if (editorState.handleSetPropertyKeyAction(it)) return@collectLatest
            if (state.mayHandleScissorsKeyAction(it, editorState::cutEntry)) return@collectLatest
            val updated = state.getUpdatedEntriesByKeyAction(it, appState.appConf, editorState.project.labelerConf)
                ?: return@collectLatest
            state.editEntryIfNeeded(updated, editorState::submitEntries)
        }
    }
}

@Composable
fun MarkerLabels(
    screenRange: FloatRange?,
    appState: AppState,
    state: MarkerState,
) {
    val requestRename: (Int) -> Unit = remember(appState) {
        { appState.openEditEntryNameDialog(it, InputEntryNameDialogPurpose.Rename) }
    }
    val jumpToEntry: (Int) -> Unit = remember(appState) {
        { appState.jumpToEntry(it) }
    }
    val onHovered: (Int, Boolean) -> Unit = remember(state) {
        { index, hovered -> state.onLabelHovered(index, hovered) }
    }

    val chunkCount = ceil(state.canvasParams.lengthInPixel / LabelMaxChunkLength).toInt()
    val chunkLength = state.canvasParams.lengthInPixel / chunkCount

    val chunkVisibleList = List(chunkCount) {
        val range = (it * chunkLength)..((it + 1) * chunkLength)
        screenRange?.contains(range) == true
    }
    FieldLabels(state, chunkCount, chunkLength, chunkVisibleList)
    if (state.labelerConf.continuous) {
        NameLabels(
            appState.appConf,
            state,
            requestRename,
            jumpToEntry,
            onHovered,
            chunkCount,
            chunkLength,
            chunkVisibleList,
        )
    }
}

@Composable
private fun FieldBorderCanvas(
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorConf: AppConf.Editor,
) {
    val screenRange = horizontalScrollState.getScreenRange(state.canvasParams.lengthInPixel)

    Canvas(Modifier.fillMaxSize()) {
        screenRange ?: return@Canvas
        try {
            val entriesInPixel = state.entriesInPixel
            val labelerConf = state.labelerConf
            val (start, startField) = if (labelerConf.useImplicitStart) {
                val fieldIndexToReplaceStart = labelerConf.fields.indexOfFirst { it.replaceStart }
                entriesInPixel.first().points[fieldIndexToReplaceStart] to labelerConf.fields[fieldIndexToReplaceStart]
            } else {
                entriesInPixel.first().start to null
            }
            val (end, endField) = if (labelerConf.useImplicitEnd) {
                val fieldIndexToReplaceEnd = labelerConf.fields.indexOfFirst { it.replaceEnd }
                entriesInPixel.last().points[fieldIndexToReplaceEnd] to labelerConf.fields[fieldIndexToReplaceEnd]
            } else {
                entriesInPixel.last().end to null
            }
            val canvasActualWidth = state.canvasParams.lengthInPixel
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val cursorState = state.cursorState
            state.canvasHeightState.value = canvasHeight

            // Draw left border
            if (leftBorder >= 0 && (0f..leftBorder in screenRange)) {
                val leftBorderColor = UneditableRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                drawRect(
                    color = leftBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset.Zero,
                    size = Size(width = relativeLeftBorder, height = canvasHeight),
                )
                drawLine(
                    color = leftBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeLeftBorder, 0f),
                    end = Offset(relativeLeftBorder, canvasHeight),
                    strokeWidth = StrokeWidth,
                )
            }

            // Draw start
            if (leftBorder..start in screenRange) {
                val startColor = startField?.color?.toColor() ?: EditableOutsideRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                val relativeStart = start - screenRange.start
                val coercedLeftBorder = relativeLeftBorder.coerceAtLeast(0f)
                val coercedStart = relativeStart.coerceAtMost(screenRange.length)
                drawRect(
                    color = startColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedLeftBorder, 0f),
                    size = Size(width = coercedStart - coercedLeftBorder, height = canvasHeight),
                )
                val startLineAlpha = if (cursorState.value.usingStartPoint) 1f else IdleLineAlpha
                drawLine(
                    color = startColor.copy(alpha = startLineAlpha),
                    start = Offset(coercedStart, 0f),
                    end = Offset(coercedStart, canvasHeight),
                    strokeWidth = StrokeWidth,
                )
            }

            // Draw custom fields and borders
            for (entryIndex in state.entriesInPixel.indices) {
                val entryInPixel = state.entriesInPixel[entryIndex]
                if (entryIndex != 0) {
                    val border = state.entryBorders[entryIndex - 1]
                    val borderColor = EditableOutsideRegionColor
                    val pointIndex = cursorState.value.pointIndex
                    val borderLineAlpha =
                        if (state.isBorderIndex(pointIndex) &&
                            state.getEntryIndexesByBorderIndex(pointIndex).second == entryIndex
                        ) 1f else IdleLineAlpha
                    if (border in screenRange) {
                        val relativeBorder = border - screenRange.start
                        drawLine(
                            color = borderColor.copy(alpha = borderLineAlpha),
                            start = Offset(relativeBorder, 0f),
                            end = Offset(relativeBorder, canvasHeight),
                            strokeWidth = StrokeWidth,
                        )
                    }
                }
                for (fieldIndex in labelerConf.fields.indices) {
                    val field = labelerConf.fields[fieldIndex]
                    if (field.replaceStart || field.replaceEnd) continue
                    val x = entryInPixel.points[fieldIndex]
                    val waveformsHeight = canvasHeight * state.waveformsHeightRatio
                    val height = waveformsHeight * field.height
                    val top = waveformsHeight - height
                    val color = field.color.toColor()
                    val fillTargetIndex = when (field.filling) {
                        "start" -> StartPointIndex
                        "end" -> EndPointIndex
                        null -> null
                        else -> labelerConf.fields.withIndex().find { it.value.name == field.filling }?.index
                    }
                    if (fillTargetIndex != null) {
                        val targetX = entryInPixel.getPoint(fillTargetIndex)
                        val left = min(targetX, x)
                        val width = abs(targetX - x)
                        val right = left + width
                        if (left..right in screenRange) {
                            val coercedRelativeLeft = (left - screenRange.start).coerceAtLeast(0f)
                            val coercedRelativeRight = (right - screenRange.start).coerceAtMost(screenRange.length)
                            drawRect(
                                color = color,
                                alpha = RegionAlpha,
                                topLeft = Offset(coercedRelativeLeft, top),
                                size = Size(width = coercedRelativeRight - coercedRelativeLeft, height = height),
                            )
                        }
                    }

                    val pointIndex = fieldIndex + entryIndex * (state.labelerConf.fields.size + 1)
                    val lineAlpha = if (cursorState.value.pointIndex != pointIndex) IdleLineAlpha else 1f
                    if (x in screenRange) {
                        val relativeX = x - screenRange.start
                        drawLine(
                            color = color.copy(alpha = lineAlpha),
                            start = Offset(relativeX, top),
                            end = Offset(relativeX, canvasHeight),
                            strokeWidth = StrokeWidth,
                        )
                    }
                }
            }

            // Draw end
            if (end..rightBorder in screenRange) {
                val endColor = endField?.color?.toColor() ?: EditableOutsideRegionColor
                val relativeEnd = end - screenRange.start
                val relativeRightBorder = rightBorder - screenRange.start
                val coercedEnd = relativeEnd.coerceAtLeast(0f)
                val coercedRightBorder = relativeRightBorder.coerceAtMost(screenRange.length)
                drawRect(
                    color = endColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedEnd, 0f),
                    size = Size(width = coercedRightBorder - coercedEnd, height = canvasHeight),
                )
                val endLineAlpha = if (cursorState.value.usingEndPoint) 1f else IdleLineAlpha
                drawLine(
                    color = endColor.copy(alpha = endLineAlpha),
                    start = Offset(coercedEnd, 0f),
                    end = Offset(coercedEnd, canvasHeight),
                    strokeWidth = StrokeWidth,
                )
            }

            // Draw right border
            if (rightBorder < canvasActualWidth && (rightBorder..canvasActualWidth in screenRange)) {
                val rightBorderColor = UneditableRegionColor
                val relativeRightBorder = rightBorder - screenRange.start
                drawRect(
                    color = rightBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset(relativeRightBorder, 0f),
                    size = Size(width = screenRange.endInclusive - relativeRightBorder, height = canvasHeight),
                )
                drawLine(
                    color = rightBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeRightBorder, 0f),
                    end = Offset(relativeRightBorder, canvasHeight),
                    strokeWidth = StrokeWidth,
                )
            }

            // Draw scissors
            state.scissorsState.value?.let { scissors ->
                val position = scissors.position
                if (scissors.disabled.not() && position != null && state.isValidCutPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = editorConf.scissorsColor.toColor(),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = StrokeWidth,
                        )
                    }
                }
            }

            // Draw playback cursor
            state.playbackState.value?.let { playbackState ->
                val position = playbackState.position
                if (position != null && state.isValidPlaybackPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = editorConf.playerCursorColor.toColor(),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = StrokeWidth,
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            if (isDebug) throw t
            Log.debug(t)
        }
    }
}

private fun MarkerState.handleMouseMove(
    tool: Tool,
    event: PointerEvent,
    editEntries: (List<IndexedEntry>, Set<Int>) -> Unit,
    screenRange: FloatRange?,
    playByCursor: (Float) -> Unit,
    scrollState: ScrollState,
    scope: CoroutineScope,
) {
    screenRange ?: return
    when (tool) {
        Tool.Cursor -> handleCursorMove(event, editEntries, screenRange, playByCursor)
        Tool.Scissors -> handleScissorsMove(event, screenRange)
        Tool.Pan -> handlePanMove(event, scrollState, scope)
        Tool.Playback -> handlePlaybackMove(event, screenRange)
    }
}

private fun MarkerState.handleCursorMove(
    event: PointerEvent,
    editEntries: (List<IndexedEntry>, Set<Int>) -> Unit,
    screenRange: FloatRange,
    playByCursor: (Float) -> Unit,
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x
    val actualX = x + screenRange.start
    cursorState.update { copy(position = actualX) }
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (cursorState.value.mouse == MarkerCursorState.Mouse.Dragging) {
        val forcedDrag = cursorState.value.forcedDrag
        val updated = if (cursorState.value.lockedDrag) {
            getLockedDraggedEntries(cursorState.value.pointIndex, actualX, forcedDrag)
        } else {
            getDraggedEntries(cursorState.value.pointIndex, actualX, forcedDrag)
        }
        editEntryIfNeeded(updated, editEntries)
        if (cursorState.value.previewOnDragging) {
            playByCursor(entryConverter.convertToFrame(actualX))
        }
    } else {
        val newPointIndex = getPointIndexForHovering(
            x = actualX,
            y = y,
            conf = labelerConf,
            canvasHeight = canvasHeightState.value,
            waveformsHeightRatio = waveformsHeightRatio,
            density = canvasParams.density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp,
        )
        if (newPointIndex == NonePointIndex) {
            cursorState.update { moveToNothing() }
        } else {
            cursorState.update { moveToHover(newPointIndex) }
        }
    }
}

private fun MarkerState.handleScissorsMove(
    event: PointerEvent,
    screenRange: FloatRange,
) {
    val scissorsState = scissorsState
    val x = event.changes.first().position.x + screenRange.start
    val position = x.takeIf { isValidCutPosition(it) }
    scissorsState.updateNonNull { copy(position = position) }
}

private fun MarkerState.handlePanMove(
    event: PointerEvent,
    scrollState: ScrollState,
    scope: CoroutineScope,
) {
    if (panState.value?.isDragging != true) return
    val x = event.changes.first().positionChange().x
    scope.launch {
        scrollState.scrollBy(-x)
    }
}

private fun MarkerState.handlePlaybackMove(
    event: PointerEvent,
    screenRange: FloatRange,
) {
    val playbackState = playbackState
    val x = event.changes.first().position.x + screenRange.start
    val position = x.takeIf { isValidPlaybackPosition(it) }
    playbackState.updateNonNull { copy(position = position) }
}

private fun MarkerState.handleMousePress(
    tool: Tool,
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf,
    appConf: AppConf,
) {
    when (tool) {
        Tool.Cursor -> handleCursorPress(keyboardState, event, labelerConf, appConf)
        Tool.Scissors -> Unit
        Tool.Pan -> handlePanPress(event)
        Tool.Playback -> Unit
    }
}

private fun MarkerState.handleCursorPress(
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf,
    appConf: AppConf,
) {
    val action = keyboardState.getEnabledMouseClickAction(event) ?: return
    if (action.canMoveParameter()) {
        val cursorStateValue = cursorState.value
        if (cursorStateValue.mouse == MarkerCursorState.Mouse.Hovering) {
            val invertLockedDrag = action == MouseClickAction.MoveParameterInvertingPrimary
            val lockedDrag = when (appConf.editor.lockedDrag) {
                AppConf.Editor.LockedDrag.UseLabeler -> {
                    val lockedDragByBaseField =
                        labelerConf.lockedDrag.useDragBase &&
                            labelerConf.fields.getOrNull(cursorStateValue.pointIndex)?.dragBase == true
                    val lockedDragByStart =
                        labelerConf.lockedDrag.useStart && cursorStateValue.usingStartPoint
                    lockedDragByBaseField || lockedDragByStart
                }
                AppConf.Editor.LockedDrag.UseStart -> cursorStateValue.usingStartPoint
                else -> false
            } xor invertLockedDrag
            val withPreview = action == MouseClickAction.MoveParameterWithPlaybackPreview
            val forcedDrag = action == MouseClickAction.MoveParameterIgnoringConstraints
            cursorState.update { startDragging(lockedDrag, withPreview, forcedDrag) }
        }
    }
}

private fun MarkerState.handlePanPress(event: PointerEvent) {
    if (event.isLeftClick) {
        panState.updateNonNull { copy(isDragging = true) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun MarkerState.handleMouseRelease(
    tool: Tool,
    event: PointerEvent,
    submitEntry: () -> Unit,
    playSampleSection: (startFrame: Float, endFrame: Float?) -> Unit,
    cutEntry: (Int, Float) -> Unit,
    keyboardState: KeyboardState,
    screenRange: FloatRange?,
) {
    screenRange ?: return
    val caughtAction = keyboardState.getEnabledMouseClickAction(event)
    val handled = when (tool) {
        Tool.Cursor -> handleCursorRelease(submitEntry, caughtAction)
        Tool.Scissors -> handleScissorsRelease(cutEntry, event)
        Tool.Pan -> handlePanRelease()
        Tool.Playback -> handlePlaybackRelease(playSampleSection, screenRange, event)
    }
    if (!handled && caughtAction == MouseClickAction.PlayAudioSection) {
        val x = event.changes.first().position.x
        val actualX = x + screenRange.start
        val clickedRange = getClickedAudioRange(actualX, leftBorder, rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: canvasParams.dataLength.toFloat()
            playSampleSection(start, end)
        }
    }
}

private fun MarkerState.handleCursorRelease(
    submitEntry: () -> Unit,
    action: MouseClickAction?
): Boolean = when (action) {
    MouseClickAction.MoveParameter,
    MouseClickAction.MoveParameterIgnoringConstraints,
    MouseClickAction.MoveParameterInvertingPrimary,
    MouseClickAction.MoveParameterWithPlaybackPreview -> {
        cursorState.update { finishDragging() }
        submitEntry()
        true
    }
    else -> false
}

private fun MarkerState.handleScissorsRelease(
    cutEntry: (Int, Float) -> Unit,
    event: PointerEvent,
): Boolean {
    val scissorsState = scissorsState
    val position = scissorsState.value?.position ?: return false
    if (event.isRightClick) return false
    handleScissorsCut(position, cutEntry)
    return true
}

private fun MarkerState.handlePanRelease(): Boolean {
    if (panState.value?.isDragging != true) return false
    panState.updateNonNull { copy(isDragging = false) }
    return true
}

private fun MarkerState.handlePlaybackRelease(
    playSampleSection: (startFrame: Float, endFrame: Float?) -> Unit,
    screenRange: FloatRange,
    event: PointerEvent,
): Boolean {
    val playbackState = playbackState
    val position = playbackState.value?.position ?: return false
    val startFrame = entryConverter.convertToFrame(position)
    val endFrame = if (event.isRightClick) {
        entryConverter.convertToFrame(screenRange.endInclusive)
    } else {
        null
    }
    playSampleSection(startFrame, endFrame)
    return true
}

private fun MarkerState.mayHandleScissorsKeyAction(action: KeyAction, cutEntry: (Int, Float) -> Unit): Boolean {
    if (action != KeyAction.ScissorsCut) return false
    val position = scissorsState.value?.position ?: return false
    handleScissorsCut(position, cutEntry)
    return true
}

private fun MarkerState.handleScissorsCut(position: Float, cutEntry: (Int, Float) -> Unit) {
    val timePosition = entryConverter.convertToMillis(position)
    val entryIndex = getEntryIndexByCutPosition(position)
    cutEntry(entryIndex, timePosition)
}

private fun MarkerState.editEntryIfNeeded(
    updated: List<EntryInPixel>,
    editEntries: (List<IndexedEntry>, Set<Int>) -> Unit,
) {
    if (updated != entriesInPixel) {
        val updatedInMillis = updated.map { entryConverter.convertToMillis(it) }
        val edited = updated - entriesInPixel.toSet()
        if (edited.isEmpty()) return

        val editedIndexes = edited.map { it.index }.toMutableSet()

        if (labelerConf.continuous) {
            val leftEntry = entriesInCurrentGroup.getPreviousOrNull { it.index == updated.firstOrNull()?.index }
            val firstEdited = edited.first()
            val firstOriginal = entriesInPixel.firstOrNull { it.index == firstEdited.index }
            val leftBorderEdited = firstEdited.start != firstOriginal?.start
            if (leftEntry != null && leftBorderEdited) {
                editedIndexes.add(leftEntry.index)
            }

            val rightEntry = entriesInCurrentGroup.getNextOrNull { it.index == updated.lastOrNull()?.index }
            val lastEdited = edited.last()
            val lastOriginal = entriesInPixel.lastOrNull { it.index == lastEdited.index }
            val rightBorderEdited = lastEdited.end != lastOriginal?.end
            if (rightEntry != null && rightBorderEdited) {
                editedIndexes.add(rightEntry.index)
            }
        }
        editEntries(updatedInMillis, editedIndexes)
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    appConf: AppConf,
    entriesInPixel: List<EntryInPixel>,
    currentModuleIndex: Int,
    currentIndex: Int,
    canvasLength: Float,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel,
) {
    val isLabelOnLeft = appConf.editor.continuousLabelNames.position.left
    LaunchedEffect(
        isLabelOnLeft,
        entriesInPixel,
        currentModuleIndex,
        currentIndex,
        canvasLength,
        horizontalScrollState.maxValue,
    ) {
        scrollFitViewModel.update(isLabelOnLeft, horizontalScrollState, canvasLength, entriesInPixel, currentIndex)
    }
}
