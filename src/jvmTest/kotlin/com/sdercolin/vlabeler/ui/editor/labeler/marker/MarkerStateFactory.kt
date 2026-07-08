package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.mutableStateOf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.parallel.SnapDrag
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull

/**
 * Builds a [MarkerState] with plain values in the same way as the production `rememberMarkerState` composable, so
 * that the pure computation logic of [MarkerState] can be tested without any Compose rendering.
 *
 * By default [sampleRate] is 1000 Hz and [resolution] is 1, so that 1 millisecond == 1 frame == 1 pixel, which makes
 * expected values in tests easy to compute by hand.
 */
object MarkerStateFactory {

    const val DEFAULT_SAMPLE_LENGTH_MILLIS = 1000f
    private const val RAW_FILE_PATH = "label.txt"

    fun entry(
        start: Float,
        end: Float,
        points: List<Float> = emptyList(),
        name: String = "entry",
        sample: String = "sample.wav",
    ) = Entry(
        sample = sample,
        name = name,
        start = start,
        end = end,
        points = points,
        extras = emptyList(),
    )

    fun parallelModule(name: String, entries: List<Entry>) = Module(
        name = name,
        sampleDirectoryPath = "/samples",
        entries = entries,
        currentIndex = 0,
        rawFilePath = RAW_FILE_PATH,
    )

    /**
     * @param labelerConf the labeler in use.
     * @param allEntries all entries of the current module (== the current group), in millis.
     * @param editedIndexes indexes in [allEntries] that are being edited (all of them by default).
     * @param parallelModules additional modules that are parallel to the current module (sharing the raw label file).
     */
    fun create(
        labelerConf: LabelerConf,
        allEntries: List<Entry>,
        editedIndexes: List<Int> = allEntries.indices.toList(),
        sampleRate: Float = 1000f,
        resolution: Int = 1,
        sampleLengthMillis: Float = DEFAULT_SAMPLE_LENGTH_MILLIS,
        parallelModules: List<Module> = emptyList(),
    ): MarkerState {
        val module = Module(
            name = "main",
            sampleDirectoryPath = "/samples",
            entries = allEntries,
            currentIndex = editedIndexes.first(),
            rawFilePath = RAW_FILE_PATH,
        )
        val project = Project(
            rootSampleDirectoryPath = "/",
            workingDirectoryPath = "/work",
            projectName = "test",
            cacheDirectoryPath = "/cache",
            originalLabelerConf = labelerConf,
            modules = listOf(module) + parallelModules,
            currentModuleIndex = 0,
            autoExport = false,
        )
        val canvasParams = CanvasParams(
            dataLength = (sampleLengthMillis * sampleRate / 1000).toInt(),
            chunkCount = 1,
            resolution = resolution,
        )
        val entryConverter = EntryConverter(sampleRate, resolution)
        val indexedEntries = allEntries.mapIndexed { index, entry -> IndexedEntry(entry, index) }
        val editedEntries = editedIndexes.map { indexedEntries[it] }
        val entriesInPixel = editedEntries.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
        val entriesInSampleInPixel = indexedEntries.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
        val previousEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getPreviousOrNull { it.index == entriesInPixel.first().index }
        } else {
            null
        }
        val leftBorder = previousEntry?.start ?: 0f
        val nextEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getNextOrNull { it.index == entriesInPixel.last().index }
        } else {
            null
        }
        val rightBorder = nextEntry?.end ?: canvasParams.lengthInPixel
        return MarkerState(
            entries = editedEntries,
            entriesInCurrentGroup = indexedEntries,
            labelerConf = labelerConf,
            canvasParams = canvasParams,
            sampleLengthMillis = sampleLengthMillis,
            entryConverter = entryConverter,
            entriesInPixel = entriesInPixel,
            entriesInSampleInPixel = entriesInSampleInPixel,
            leftBorder = leftBorder,
            rightBorder = rightBorder,
            cursorState = mutableStateOf(MarkerCursorState()),
            scissorsState = mutableStateOf(null),
            panState = mutableStateOf(null),
            playbackState = mutableStateOf(null),
            canvasHeightState = mutableStateOf(0f),
            waveformsHeightRatio = 0.8f,
            snapDrag = SnapDrag(project, canvasParams.lengthInPixel, entryConverter),
            project = project,
        )
    }
}
