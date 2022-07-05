package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.util.toFrame
import com.sdercolin.vlabeler.util.toMillisecond

class EntryConverter(
    private val sampleRate: Float,
    private val resolution: Int
) {
    fun convertToPixel(entry: Entry, sampleFileLengthMillis: Float) = EntryInPixel(
        sample = entry.sample,
        name = entry.name,
        start = convertToPixel(entry.start),
        end = if (entry.end <= 0) {
            convertToPixel(sampleFileLengthMillis + entry.end)
        } else {
            convertToPixel(entry.end)
        },
        points = entry.points.map { convertToPixel(it) },
        extra = entry.extra
    )

    private fun convertToPixel(millis: Float) =
        toFrame(millis, sampleRate).div(resolution)

    fun convertToMillis(entry: EntryInPixel) = Entry(
        sample = entry.sample,
        name = entry.name,
        start = convertToMillis(entry.start),
        end = convertToMillis(entry.end),
        points = entry.points.map { convertToMillis(it) },
        extra = entry.extra
    )

    fun convertToMillis(px: Float) = toMillisecond(convertToFrame(px), sampleRate)
    fun convertToFrame(px: Float) = px.times(resolution)
}
