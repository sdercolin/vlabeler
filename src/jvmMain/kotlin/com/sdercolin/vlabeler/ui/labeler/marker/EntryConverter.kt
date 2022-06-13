package com.sdercolin.vlabeler.ui.labeler.marker

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.util.toFrame
import com.sdercolin.vlabeler.util.toMillisecond

class EntryConverter(
    private val sampleRate: Float,
    private val resolution: Int
) {
    fun convertToPixel(entry: Entry) = EntryInPixel(
        name = entry.name,
        start = convertToPixel(entry.start),
        end = convertToPixel(entry.end),
        points = entry.points.map { convertToPixel(it) }
    )

    private fun convertToPixel(millis: Float) = toFrame(millis, sampleRate).div(resolution)

    fun convertToMillis(entry: EntryInPixel) = Entry(
        name = entry.name,
        start = convertToMillis(entry.start),
        end = convertToMillis(entry.end),
        points = entry.points.map { convertToMillis(it) }
    )

    private fun convertToMillis(px: Float) = toMillisecond(convertToFrame(px), sampleRate)
    fun convertToFrame(px: Float) = px.times(resolution)
}