package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EntryConverterTest {

    @Test
    fun convertMillisToPixelDividesFramesByResolution() {
        // 100 ms at 44100 Hz = 4410 frames; at resolution 50 -> 88.2 px
        val converter = EntryConverter(sampleRate = 44100f, resolution = 50)
        assertEquals(88.2f, converter.convertToPixel(100f), 0.001f)
    }

    @Test
    fun convertPixelToMillisIsInverseOfConvertToPixel() {
        val converter = EntryConverter(sampleRate = 44100f, resolution = 50)
        val millis = 123.4f
        assertEquals(millis, converter.convertToMillis(converter.convertToPixel(millis)), 0.001f)
    }

    @Test
    fun convertPixelToFrameMultipliesByResolution() {
        val converter = EntryConverter(sampleRate = 44100f, resolution = 50)
        assertEquals(4410f, converter.convertToFrame(88.2f), 0.001f)
    }

    @Test
    fun oneMillisecondEqualsOnePixelWithSampleRate1000AndResolution1() {
        val converter = EntryConverter(sampleRate = 1000f, resolution = 1)
        assertEquals(250f, converter.convertToPixel(250f))
        assertEquals(250f, converter.convertToMillis(250f))
    }

    @Test
    fun convertEntryToPixelScalesAllValues() {
        // 1000 Hz at resolution 2 -> 1 px == 2 ms
        val converter = EntryConverter(sampleRate = 1000f, resolution = 2)
        val entry = IndexedEntry(
            entry = Entry(
                sample = "a.wav",
                name = "a",
                start = 100f,
                end = 500f,
                points = listOf(200f, 300f),
                extras = listOf("x"),
            ),
            index = 3,
        )
        val converted = converter.convertToPixel(entry, sampleFileLengthMillis = 1000f)
        assertEquals(3, converted.index)
        assertEquals("a.wav", converted.sample)
        assertEquals("a", converted.name)
        assertEquals(50f, converted.start)
        assertEquals(250f, converted.end)
        assertEquals(listOf(100f, 150f), converted.points)
        assertEquals(listOf("x"), converted.extras)
    }

    @Test
    fun convertEntryToPixelResolvesNegativeEndAgainstSampleLength() {
        // end < 0 means "relative to the end of the sample": -100 ms in a 1000 ms sample -> 900 ms -> 450 px
        val converter = EntryConverter(sampleRate = 1000f, resolution = 2)
        val entry = IndexedEntry(
            entry = Entry(
                sample = "a.wav",
                name = "a",
                start = 100f,
                end = -100f,
                points = listOf(),
                extras = listOf(),
            ),
            index = 0,
        )
        val converted = converter.convertToPixel(entry, sampleFileLengthMillis = 1000f)
        assertEquals(450f, converted.end)
    }

    @Test
    fun convertEntryToPixelResolvesZeroEndAgainstSampleLengthWhenNeedSync() {
        // needSync && end == 0 also means "relative to the end of the sample": 1000 ms -> 500 px
        val converter = EntryConverter(sampleRate = 1000f, resolution = 2)
        val entry = IndexedEntry(
            entry = Entry(
                sample = "a.wav",
                name = "a",
                start = 100f,
                end = 0f,
                points = listOf(),
                extras = listOf(),
                needSync = true,
            ),
            index = 0,
        )
        val converted = converter.convertToPixel(entry, sampleFileLengthMillis = 1000f)
        assertEquals(500f, converted.end)
    }

    @Test
    fun convertEntryToMillisScalesAllValuesBack() {
        val converter = EntryConverter(sampleRate = 1000f, resolution = 2)
        val entryInPixel = EntryInPixel(
            index = 5,
            sample = "a.wav",
            name = "a",
            start = 50f,
            end = 250f,
            points = listOf(100f, 150f),
            extras = listOf("x"),
            notes = EntryNotes(),
        )
        val converted = converter.convertToMillis(entryInPixel)
        assertEquals(5, converted.index)
        assertEquals("a.wav", converted.sample)
        assertEquals("a", converted.name)
        assertEquals(100f, converted.start)
        assertEquals(500f, converted.end)
        assertEquals(listOf(200f, 300f), converted.points)
        assertEquals(listOf("x"), converted.extras)
    }

    @Test
    fun entryRoundTripPreservesValues() {
        val converter = EntryConverter(sampleRate = 44100f, resolution = 40)
        val entry = IndexedEntry(
            entry = Entry(
                sample = "a.wav",
                name = "a",
                start = 100f,
                end = 500f,
                points = listOf(200f, 300f),
                extras = listOf(),
            ),
            index = 0,
        )
        val roundTrip = converter.convertToMillis(converter.convertToPixel(entry, sampleFileLengthMillis = 1000f))
        assertEquals(entry.start, roundTrip.start, 0.001f)
        assertEquals(entry.end, roundTrip.end, 0.001f)
        assertEquals(entry.points.size, roundTrip.points.size)
        entry.points.zip(roundTrip.points).forEach { (expected, actual) ->
            assertEquals(expected, actual, 0.001f)
        }
    }
}
