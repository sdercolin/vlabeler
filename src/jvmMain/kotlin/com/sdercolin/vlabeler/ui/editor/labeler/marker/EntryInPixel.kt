package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.StartPointIndex

@Immutable
data class EntryInPixel(
    val index: Int,
    val sample: String,
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>,
    val extras: List<String>,
    val notes: EntryNotes,
) {

    fun moved(dx: Float) = copy(
        start = start + dx,
        end = end + dx,
        points = points.map { it + dx },
    )

    fun validate(canvasWidthInPixel: Int) = copy(
        start = start.coerceAtMost(canvasWidthInPixel.toFloat()),
        end = end.coerceAtMost(canvasWidthInPixel.toFloat()),
        points = points.map { it.coerceAtMost(canvasWidthInPixel.toFloat()) },
    )

    fun getPoint(index: Int) = when (index) {
        StartPointIndex -> start
        EndPointIndex -> end
        else -> points[index]
    }

    fun isValidCutPosition(position: Float) = position > start && position < end

    fun collapsed(leftBorder: Float = 0f, rightBorder: Float = Float.POSITIVE_INFINITY): EntryInPixel {
        val start = start.coerceAtLeast(leftBorder).coerceAtMost(rightBorder)
        val end = end.coerceAtMost(rightBorder).coerceAtLeast(leftBorder)
        val points = points.map { it.coerceAtLeast(start).coerceAtMost(end) }
        return copy(start = start, end = end, points = points)
    }
}
