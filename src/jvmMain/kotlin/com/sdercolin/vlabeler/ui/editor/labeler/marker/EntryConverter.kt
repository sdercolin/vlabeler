package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.toFrame
import com.sdercolin.vlabeler.util.toMillisecond

class EntryConverter(
    private val sampleRate: Float,
    private val resolution: Int,
) {
    fun convertToPixel(entry: IndexedEntry, sampleFileLengthMillis: Float) = EntryInPixel(
        index = entry.index,
        sample = entry.sample,
        name = entry.name,
        start = convertToPixel(entry.start),
        end = if (entry.entry.needSyncCompatibly) {
            convertToPixel(sampleFileLengthMillis + entry.end)
        } else {
            convertToPixel(entry.end)
        },
        points = entry.points.map { convertToPixel(it) },
        extras = entry.extras,
        notes = entry.entry.notes,
    )

    fun convertToPixel(millis: Float) =
        toFrame(millis, sampleRate).div(resolution)

    fun convertToMillis(entry: EntryInPixel) = IndexedEntry(
        index = entry.index,
        entry = Entry(
            sample = entry.sample,
            name = entry.name,
            start = convertToMillis(entry.start),
            end = convertToMillis(entry.end),
            points = entry.points.map { convertToMillis(it) },
            extras = entry.extras,
            notes = entry.notes,
        ),
    )

    fun convertToMillis(px: Float) = toMillisecond(convertToFrame(px), sampleRate)
    fun convertToFrame(px: Float) = px.times(resolution)
}
