package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams

class MarkerState(
    val entry: Entry,
    val entriesInSample: List<Entry>,
    val labelerConf: LabelerConf,
    val currentIndexInSample: Int,
    val canvasParams: CanvasParams,
    val entryConverter: EntryConverter,
    val entryInPixel: EntryInPixel,
    val leftBorder: Float,
    val rightBorder: Float,
    val mouseState: MutableState<MarkerMouseState>,
    val canvasHeightState: MutableState<Float>,
    val waveformsHeightRatio: Float
)

@Composable
fun rememberMarkerState(
    sample: Sample,
    canvasParams: CanvasParams,
    editorState: EditorState,
    appState: AppState
): MarkerState {
    val sampleRate = sample.info.sampleRate
    val sampleLengthMillis = sample.info.lengthMillis
    val entry = editorState.editedEntry.entry
    val project = editorState.project
    val entriesInSample = project.entriesInCurrentSample
    val labelerConf = project.labelerConf
    val currentIndexInSample = project.currentEntryIndex
    val entryConverter = EntryConverter(sample.info.sampleRate, canvasParams.resolution)
    val entryInPixel =
        entryConverter.convertToPixel(entry, sampleLengthMillis).validate(canvasParams.lengthInPixel)
    val entriesInSampleInPixel = remember(entryInPixel) {
        entriesInSample.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val leftBorder = remember(entryInPixel) {
        (
            if (labelerConf.continuous) {
                entriesInSampleInPixel.getOrNull(currentIndexInSample - 1)?.start
            } else null
            )
            ?: 0f
    }
    val rightBorder = remember(entryInPixel) {
        (
            if (labelerConf.continuous) {
                entriesInSampleInPixel.getOrNull(currentIndexInSample + 1)?.end
            } else null
            )
            ?: canvasParams.lengthInPixel.toFloat()
    }
    val mouseState = remember { mutableStateOf(MarkerMouseState()) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val waveformsHeightRatio = remember(appState.appConf.painter.spectrogram) {
        val spectrogram = appState.appConf.painter.spectrogram
        val totalWeight = 1f + if (spectrogram.enabled) spectrogram.heightWeight else 0f
        1f / totalWeight
    }

    return remember(
        sampleRate,
        sampleLengthMillis,
        entry,
        entriesInSample,
        labelerConf,
        currentIndexInSample,
        canvasParams,
        entryConverter,
        entryInPixel,
        entriesInSampleInPixel,
        leftBorder,
        rightBorder,
        mouseState,
        canvasHeightState,
        waveformsHeightRatio
    ) {
        MarkerState(
            entry,
            entriesInSample,
            labelerConf,
            currentIndexInSample,
            canvasParams,
            entryConverter,
            entryInPixel,
            leftBorder,
            rightBorder,
            mouseState,
            canvasHeightState,
            waveformsHeightRatio
        )
    }
}
