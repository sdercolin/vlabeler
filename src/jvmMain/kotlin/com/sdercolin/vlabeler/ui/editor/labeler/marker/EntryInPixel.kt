package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.END_POINT_INDEX
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.START_POINT_INDEX

@Immutable
data class EntryInPixel(
    val index: Int,
    val sample: String,
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>,
    val extras: List<String?>,
    val notes: EntryNotes,
) {

    fun validateImplicit(labelerConf: LabelerConf): EntryInPixel {
        val start = if (labelerConf.useImplicitStart) {
            points.minOrNull() ?: start
        } else {
            start
        }
        val end = if (labelerConf.useImplicitEnd) {
            points.maxOrNull() ?: end
        } else {
            end
        }
        return copy(start = start, end = end)
    }

    fun moved(dx: Float) = copy(
        start = start + dx,
        end = end + dx,
        points = points.map { it + dx },
    )

    fun validate(canvasWidthInPixel: Float) = copy(
        start = start.coerceAtMost(canvasWidthInPixel),
        end = end.coerceAtMost(canvasWidthInPixel),
        points = points.map { it.coerceAtMost(canvasWidthInPixel) },
    )

    fun getPoint(index: Int) = when (index) {
        START_POINT_INDEX -> start
        END_POINT_INDEX -> end
        else -> points[index]
    }

    fun isValidCutPosition(position: Float) = position > start && position < end

    fun collapsed(leftBorder: Float = 0f, rightBorder: Float = Float.POSITIVE_INFINITY): EntryInPixel {
        val start = start.coerceAtLeast(leftBorder).coerceAtMost(rightBorder)
        val end = end.coerceAtMost(rightBorder).coerceAtLeast(leftBorder)
        val points = points.map { it.coerceAtLeast(start).coerceAtMost(end) }
        return copy(start = start, end = end, points = points)
    }

    fun getActualStart(labelerConf: LabelerConf) = if (labelerConf.useImplicitStart) {
        points[labelerConf.fields.indexOfFirst { it.replaceStart }]
    } else {
        start
    }

    fun setActualStart(labelerConf: LabelerConf, value: Float) = if (labelerConf.useImplicitStart) {
        val points = points.toMutableList()
        points[labelerConf.fields.indexOfFirst { it.replaceStart }] = value
        copy(points = points)
    } else {
        copy(start = value)
    }

    fun getActualEnd(labelerConf: LabelerConf) = if (labelerConf.useImplicitEnd) {
        points[labelerConf.fields.indexOfFirst { it.replaceEnd }]
    } else {
        end
    }

    fun setActualEnd(labelerConf: LabelerConf, value: Float) = if (labelerConf.useImplicitEnd) {
        val points = points.toMutableList()
        points[labelerConf.fields.indexOfFirst { it.replaceEnd }] = value
        copy(points = points)
    } else {
        copy(end = value)
    }

    fun getActualMiddlePoints(labelerConf: LabelerConf) = labelerConf.fields.withIndex()
        .filterNot { it.value.replaceStart || it.value.replaceEnd }
        .map { it.index }
        .map { points[it] }
}
