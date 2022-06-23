@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.MouseState
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.update
import kotlin.math.abs
import kotlin.math.min

@Immutable
data class MarkerState(
    val mouse: MouseState = MouseState.None,
    val pointIndex: Int = NonePointIndex, // starts from 0 for custom points
    val lockedDrag: Boolean = false
) {

    fun startDragging(lockedDrag: Boolean) = copy(mouse = MouseState.Dragging, lockedDrag = lockedDrag)
    fun finishDragging() = copy(mouse = MouseState.None, lockedDrag = false)

    fun moveToNothing() = copy(pointIndex = NonePointIndex, mouse = MouseState.None)
    fun moveToHover(index: Int) = copy(pointIndex = index, mouse = MouseState.Hovering)

    enum class MouseState {
        Dragging,
        Hovering,
        None
    }

    val usingStartPoint get() = pointIndex == StartPointIndex
    val usingEndPoint get() = pointIndex == EndPointIndex

    companion object {
        const val NonePointIndex = -3
        const val StartPointIndex = -2
        const val EndPointIndex = -1
    }
}

private const val RegionAlpha = 0.3f
private val EditableOutsideRegionColor = White
private const val UneditableRegionAlpha = 0.9f
private val UneditableRegionColor = Black
private const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
private val LabelSize = DpSize(40.dp, 25.dp)
private val LabelShiftUp = 8.dp
private val NameLabelLeftMargin = 5.dp
private val NameLabelTopMargin = 2.dp

@Composable
fun MarkerCanvas(
    entry: Entry,
    entriesInSample: List<Entry>,
    currentIndexInSample: Int,
    sampleLengthMillis: Float,
    isBusy: Boolean,
    editEntry: (Entry) -> Unit,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    appConf: AppConf,
    labelerConf: LabelerConf,
    canvasParams: CanvasParams,
    sampleRate: Float,
    horizontalScrollState: ScrollState,
    keyboardViewModel: KeyboardViewModel,
    scrollFitViewModel: ScrollFitViewModel
) {
    val entryConverter = EntryConverter(sampleRate, canvasParams.resolution)
    val entryInPixel = entryConverter.convertToPixel(entry, sampleLengthMillis).validate(canvasParams.lengthInPixel)
    val entriesInSampleInPixel = remember(entry, canvasParams) {
        entriesInSample.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val leftBorder = remember(entry, canvasParams) {
        (
            if (labelerConf.continuous) {
                entriesInSampleInPixel.getOrNull(currentIndexInSample - 1)?.start?.plus(1)
            } else null
            )
            ?: 0f
    }
    val rightBorder = remember(entry, canvasParams) {
        (
            if (labelerConf.continuous) {
                entriesInSampleInPixel.getOrNull(currentIndexInSample + 1)?.end?.minus(1)
            } else null
            )
            ?: canvasParams.lengthInPixel.toFloat()
    }

    val state = remember(isBusy) { mutableStateOf(MarkerState()) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val waveformsHeightRatio = remember(appConf.painter.spectrogram) {
        val spectrogram = appConf.painter.spectrogram
        val totalWeight = 1f + if (spectrogram.enabled) spectrogram.heightWeight else 0f
        1f / totalWeight
    }
    FieldBorderCanvas(
        entryInPixel,
        canvasParams,
        leftBorder,
        rightBorder,
        waveformsHeightRatio,
        isBusy,
        editEntry,
        submitEntry,
        playSampleSection,
        labelerConf,
        canvasHeightState,
        state,
        keyboardViewModel,
        entryConverter
    )
    FieldLabelCanvas(canvasParams, waveformsHeightRatio, state.value, labelerConf, entryInPixel)
    if (labelerConf.continuous) {
        val leftName = entriesInSample.getOrNull(currentIndexInSample - 1)?.name
        val rightName = entriesInSample.getOrNull(currentIndexInSample + 1)?.name
        NameLabelCanvas(canvasParams, entryInPixel, leftName, rightName, leftBorder, rightBorder)
    }
    LaunchAdjustScrollPosition(entryInPixel, canvasParams.lengthInPixel, horizontalScrollState, scrollFitViewModel)
}

@Composable
private fun FieldBorderCanvas(
    entryInPixel: EntryInPixel,
    canvasParams: CanvasParams,
    leftBorder: Float,
    rightBorder: Float,
    waveformsHeightRatio: Float,
    isBusy: Boolean,
    editEntry: (Entry) -> Unit,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    labelerConf: LabelerConf,
    canvasHeightState: MutableState<Float>,
    state: MutableState<MarkerState>,
    keyboardViewModel: KeyboardViewModel,
    entryConverter: EntryConverter
) {
    val keyboardState by keyboardViewModel.keyboardStateFlow.collectAsState()

    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .onPointerEvent(PointerEventType.Move) { event ->
                if (isBusy) return@onPointerEvent
                handleMouseMove(
                    event,
                    canvasParams,
                    leftBorder,
                    rightBorder,
                    canvasHeightState,
                    state,
                    entryInPixel,
                    labelerConf,
                    editEntry,
                    entryConverter,
                    waveformsHeightRatio
                )
            }
            .onPointerEvent(PointerEventType.Press) {
                if (isBusy) return@onPointerEvent
                handleMousePress(keyboardState, state, labelerConf)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                if (isBusy) return@onPointerEvent
                handleMouseRelease(
                    event,
                    entryInPixel,
                    leftBorder,
                    rightBorder,
                    submitEntry,
                    playSampleSection,
                    canvasParams,
                    state,
                    keyboardState,
                    entryConverter
                )
            }
    ) {
        val start = entryInPixel.start
        val end = entryInPixel.end
        val canvasWidth = size.width
        val canvasHeight = size.height
        canvasHeightState.value = canvasHeight

        // Draw left border
        if (leftBorder > 0) {
            val leftBorderColor = UneditableRegionColor
            drawRect(
                color = leftBorderColor,
                alpha = UneditableRegionAlpha,
                topLeft = Offset.Zero,
                size = Size(width = leftBorder, height = canvasHeight)
            )
            drawLine(
                color = leftBorderColor.copy(alpha = IdleLineAlpha),
                start = Offset(leftBorder, 0f),
                end = Offset(leftBorder, canvasHeight),
                strokeWidth = StrokeWidth
            )
        }

        // Draw start
        val startColor = EditableOutsideRegionColor
        drawRect(
            color = startColor,
            alpha = RegionAlpha,
            topLeft = Offset(leftBorder, 0f),
            size = Size(width = start - leftBorder, height = canvasHeight)
        )
        val startLineAlpha = if (state.value.usingStartPoint) 1f else IdleLineAlpha
        drawLine(
            color = startColor.copy(alpha = startLineAlpha),
            start = Offset(start, 0f),
            end = Offset(start, canvasHeight),
            strokeWidth = StrokeWidth
        )

        // Draw custom fields
        for (i in labelerConf.fields.indices) {
            val field = labelerConf.fields[i]
            val x = entryInPixel.getCustomPoint(i)
            val waveformsHeight = canvasHeight * waveformsHeightRatio
            val height = waveformsHeight * field.height
            val top = waveformsHeight - height
            val color = parseColor(field.color)
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
                drawRect(
                    color = color,
                    alpha = RegionAlpha,
                    topLeft = Offset(left, top),
                    size = Size(width = width, height = height)
                )
            }
            val lineAlpha = if (state.value.pointIndex != i) IdleLineAlpha else 1f
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = Offset(x, top),
                end = Offset(x, canvasHeight),
                strokeWidth = StrokeWidth
            )
        }

        // Draw end
        val endColor = EditableOutsideRegionColor
        drawRect(
            color = endColor,
            alpha = RegionAlpha,
            topLeft = Offset(end, 0f),
            size = Size(width = rightBorder - end, height = canvasHeight)
        )
        val endLineAlpha = if (state.value.usingEndPoint) 1f else IdleLineAlpha
        drawLine(
            color = endColor.copy(alpha = endLineAlpha),
            start = Offset(end, 0f),
            end = Offset(end, canvasHeight),
            strokeWidth = StrokeWidth
        )

        // Draw right border
        if (rightBorder < canvasWidth) {
            val rightBorderColor = UneditableRegionColor
            drawRect(
                color = rightBorderColor,
                alpha = UneditableRegionAlpha,
                topLeft = Offset(rightBorder, 0f),
                size = Size(width = canvasWidth - rightBorder, height = canvasHeight)
            )
            drawLine(
                color = rightBorderColor.copy(alpha = IdleLineAlpha),
                start = Offset(rightBorder, 0f),
                end = Offset(rightBorder, canvasHeight),
                strokeWidth = StrokeWidth
            )
        }
    }
}

@Composable
private fun FieldLabelCanvas(
    canvasParams: CanvasParams,
    waveformsHeightRatio: Float,
    state: MarkerState,
    conf: LabelerConf,
    entryInPixel: EntryInPixel
) = FieldLabelCanvasLayout(
    modifier = Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp),
    waveformsHeightRatio = waveformsHeightRatio,
    fields = conf.fields,
    entry = entryInPixel
) {
    for (i in conf.fields.indices) {
        val field = conf.fields[i]
        val alpha = if (state.pointIndex != i) IdleLineAlpha else 1f
        Box(
            modifier = Modifier.requiredSize(LabelSize),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = field.label,
                textAlign = TextAlign.Center,
                color = parseColor(field.color).copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.caption.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun FieldLabelCanvasLayout(
    modifier: Modifier,
    waveformsHeightRatio: Float,
    fields: List<LabelerConf.Field>,
    entry: EntryInPixel,
    content: @Composable () -> Unit
) {
    val labelShiftUp = with(LocalDensity.current) { LabelShiftUp.toPx() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val field = fields[index]
                val x = entry.getCustomPoint(index) - (constraints.maxWidth) / 2
                val canvasHeight = constraints.maxHeight.toFloat()
                val waveformsHeight = canvasHeight * waveformsHeightRatio
                val restCanvasHeight = canvasHeight - waveformsHeight
                val height = waveformsHeight * field.height + restCanvasHeight
                val y = canvasHeight - height - labelShiftUp - canvasHeight / 2
                placeable.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun NameLabelCanvas(
    canvasParams: CanvasParams,
    entryInPixel: EntryInPixel,
    leftName: String?,
    rightName: String?,
    leftBorder: Float,
    rightBorder: Float
) = NameLabelCanvasLayout(
    modifier = Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp),
    entry = entryInPixel,
    leftBorder = leftBorder.takeIf { leftName != null },
    rightBorder = rightBorder.takeIf { rightName != null }
) {
    if (leftName != null) {
        NameLabel(leftName, Black)
    }
    NameLabel(entryInPixel.name, DarkYellow)
    if (rightName != null) {
        NameLabel(rightName, Black)
    }
}

@Composable
private fun NameLabel(name: String, color: Color) {
    Text(
        modifier = Modifier.widthIn(max = 100.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun NameLabelCanvasLayout(
    modifier: Modifier,
    entry: EntryInPixel,
    leftBorder: Float?,
    rightBorder: Float?,
    content: @Composable () -> Unit
) {
    val labelTopMargin = with(LocalDensity.current) { NameLabelTopMargin.toPx() }
    val labelLeftMargin = with(LocalDensity.current) { NameLabelLeftMargin.toPx() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = labelTopMargin.toInt()
            val xs = listOfNotNull(
                leftBorder,
                entry.start,
                rightBorder?.let { entry.end }
            )
            placeables.forEachIndexed { index, placeable ->
                val x = xs[index] + labelLeftMargin
                placeable.place(x.toInt(), y)
            }
        }
    }
}

private fun handleMouseMove(
    event: PointerEvent,
    canvasParams: CanvasParams,
    leftBorder: Float,
    rightBorder: Float,
    canvasHeightState: MutableState<Float>,
    state: MutableState<MarkerState>,
    entryInPixel: EntryInPixel,
    labelerConf: LabelerConf,
    editEntry: (Entry) -> Unit,
    entryConverter: EntryConverter,
    waveformsHeightRatio: Float
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x.coerceIn(0f, canvasParams.lengthInPixel.toFloat())
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (state.value.mouse == MouseState.Dragging) {
        val newEntryInPixel = if (state.value.lockedDrag) {
            entryInPixel.lockedDrag(state.value.pointIndex, x, leftBorder, rightBorder)
        } else {
            entryInPixel.drag(state.value.pointIndex, x, leftBorder, rightBorder, labelerConf)
        }
        if (newEntryInPixel != entryInPixel) {
            editEntry(entryConverter.convertToMillis(newEntryInPixel))
        }
    } else {
        val newPointIndex = entryInPixel.getPointIndexForHovering(
            x = x,
            y = y,
            conf = labelerConf,
            canvasHeight = canvasHeightState.value,
            waveformsHeightRatio = waveformsHeightRatio,
            density = canvasParams.density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp
        )
        if (newPointIndex == NonePointIndex) {
            state.update { moveToNothing() }
        } else {
            state.update { moveToHover(newPointIndex) }
        }
    }
}

private fun handleMousePress(
    keyboardState: KeyboardState,
    state: MutableState<MarkerState>,
    labelerConf: LabelerConf
) {
    if (!keyboardState.isCtrlPressed) {
        if (state.value.mouse == MouseState.Hovering) {
            val lockedDragByBaseField =
                labelerConf.lockedDrag.useDragBase &&
                    labelerConf.fields.getOrNull(state.value.pointIndex)?.dragBase == true
            val lockedDragByStart =
                labelerConf.lockedDrag.useStart && state.value.usingStartPoint
            val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor keyboardState.isShiftPressed
            state.update { startDragging(lockedDrag) }
        }
    }
}

private fun handleMouseRelease(
    event: PointerEvent,
    entryInPixel: EntryInPixel,
    leftBorder: Float,
    rightBorder: Float,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    canvasParams: CanvasParams,
    state: MutableState<MarkerState>,
    keyboardState: KeyboardState,
    entryConverter: EntryConverter
) {
    if (keyboardState.isCtrlPressed) {
        val x = event.changes.first().position.x
        val clickedRange = entryInPixel.getClickedAudioRange(x, leftBorder, rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: canvasParams.dataLength.toFloat()
            playSampleSection(start, end)
        }
    } else {
        submitEntry()
        state.update { finishDragging() }
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    entryInPixel: EntryInPixel,
    canvasLength: Int,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel
) {
    LaunchedEffect(entryInPixel.name, canvasLength, horizontalScrollState.maxValue) {
        val scrollMax = horizontalScrollState.maxValue
        val screenLength = canvasLength.toFloat() - scrollMax
        val start = entryInPixel.start
        val end = entryInPixel.end
        val center = (start + end) / 2
        val target = (center - screenLength / 2).toInt().coerceAtMost(scrollMax).coerceAtLeast(0)
        scrollFitViewModel.update(target)
    }
}
