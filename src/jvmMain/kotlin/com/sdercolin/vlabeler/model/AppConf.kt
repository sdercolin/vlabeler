package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.editor.SpectrogramColorPalette
import com.sdercolin.vlabeler.ui.string.LocalizedText
import com.sdercolin.vlabeler.ui.string.Strings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Basic configurations of app
 * @param painter Configurations about chart painting
 * @param editor Configurations about editor
 * @param autoSave Configurations about auto-save
 */
@Serializable
@Immutable
data class AppConf(
    val painter: Painter = Painter(),
    val editor: Editor = Editor(),
    val autoSave: AutoSave = AutoSave()
) {
    /**
     * Configurations about chart painting
     * @param canvasResolution Configurations about the canvas's resolution
     * @param maxDataChunkSize Max number of sample points in one chunk drawn in the painter
     * @param amplitude Configurations about amplitude (waveforms) painting
     * @param spectrogram Configurations about spectrogram painting
     */
    @Serializable
    @Immutable
    data class Painter(
        val canvasResolution: CanvasResolution = CanvasResolution(),
        val maxDataChunkSize: Int = DefaultMaxDataChunkSize,
        val amplitude: Amplitude = Amplitude(),
        val spectrogram: Spectrogram = Spectrogram()
    ) {
        companion object {
            const val DefaultMaxDataChunkSize = 441000
            const val MinMaxDataChunkSize = DefaultMaxDataChunkSize / 2
            const val MaxMaxDataChunkSize = DefaultMaxDataChunkSize * 3
        }
    }

    /**
     * Configurations about the canvas's resolution.
     * This resolution is defined as number of sample points included in 1 pixel.
     * @param default Default value used when editor is launched
     * @param step Linear step length when resolution is changed by "+" "-" buttons
     */
    @Serializable
    @Immutable
    data class CanvasResolution(
        val default: Int = DefaultDefault,
        val step: Int = DefaultStep
    ) {
        val min: Int get() = Min
        val max: Int get() = Max

        companion object {
            const val Max = 400
            const val Min = 10
            const val DefaultDefault = 100
            const val DefaultStep = 20
        }
    }

    /**
     * Configurations about amplitude(waveforms) painting
     * @param unitSize Frame size of one pixel used when drawing the waveform
     * @param intensityAccuracy Height of the container bitmap in pixel
     * @param yAxisBlankRate Height rate of the extra blank region displayed in both top and bottom to
     * the height of the waveform
     * @param color Color of the waveform
     * @param backgroundColor Background color of the waveform
     */
    @Serializable
    @Immutable
    data class Amplitude(
        val unitSize: Int = DefaultUnitSize,
        val intensityAccuracy: Int = DefaultIntensityAccuracy,
        val yAxisBlankRate: Float = DefaultYAxisBlankRate,
        val color: String = DefaultColor,
        val backgroundColor: String = DefaultBackgroundColor
    ) {
        companion object {
            const val DefaultUnitSize = 40
            const val MaxUnitSize = DefaultUnitSize * 10
            const val MinUnitSize = 1
            const val DefaultIntensityAccuracy = 1000
            const val MaxIntensityAccuracy = DefaultIntensityAccuracy * 5
            const val MinIntensityAccuracy = DefaultIntensityAccuracy / 5
            const val DefaultYAxisBlankRate = 0.1f
            const val MaxYAxisBlankRate = 1f
            const val MinYAxisBlankRate = 0f
            const val DefaultColor = "#FFF2F2F2"
            const val DefaultBackgroundColor = "#00000000"
        }
    }

    /**
     * Configurations about spectrogram painting
     * @param enabled True if spectrogram is calculated and shown
     * @param heightWeight Height weight of the spectrogram to the amplitude form (whose weight is 1)
     * @param frameSize Number of samples that each FFT-frame should have
     * @param maxFrequency Max frequency (Hz) displayed
     * @param minIntensity Min intensity (dB) displayed in the heatmap
     * @param maxIntensity Max intensity (dB) displayed in the heatmap
     * @param windowType Window type used in the Short-Time FT. See [WindowType] for options
     */
    @Serializable
    @Immutable
    data class Spectrogram(
        val enabled: Boolean = DefaultEnabled,
        val heightWeight: Float = DefaultHeightWeight,
        val pointPixelSize: Int = DefaultPointPixelSize,
        val frameSize: Int = DefaultFrameSize,
        val maxFrequency: Int = DefaultMaxFrequency,
        val minIntensity: Int = DefaultMinIntensity,
        val maxIntensity: Int = DefaultMaxIntensity,
        val windowType: WindowType = DefaultWindowType,
        val colorPalette: SpectrogramColorPalette.Presets = DefaultColorPalette
    ) {
        companion object {
            const val DefaultEnabled = true
            const val DefaultHeightWeight = 0.75f
            const val MaxHeightWeight = 5f
            const val MinHeightWeight = 0.1f
            const val DefaultPointPixelSize = 1
            const val MaxPointPixelSize = 40
            const val MinPointPixelSize = 1
            const val DefaultFrameSize = 300
            const val MaxFrameSize = 2048
            const val MinFrameSize = 64
            const val DefaultMaxFrequency = 15000
            const val MaxMaxFrequency = 48000
            const val MinMaxFrequency = 5000
            const val DefaultMinIntensity = 0
            const val DefaultMaxIntensity = 45
            val DefaultWindowType = WindowType.Hamming
            val DefaultColorPalette = SpectrogramColorPalette.Presets.Plain
        }
    }

    @Serializable
    @Immutable
    enum class WindowType {
        Hamming,
        Hanning,
        Rectangular,
        Triangular,
        Blackman,
        BlackmanHarris,
        Bartlett
    }

    /**
     * Configurations about editor behaviors
     * @param scissorsColor Color hex string of the scissors' cursor position
     * @param scissorsActions Actions taken with a successful scissors click
     * @param autoScroll Timings when `scroll to editable area` is automatically conducted
     */
    @Serializable
    @Immutable
    data class Editor(
        val playerCursorColor: String = DefaultPlayerCursorColor,
        val scissorsColor: String = DefaultScissorsColor,
        val scissorsActions: ScissorsActions = ScissorsActions(),
        val autoScroll: AutoScroll = AutoScroll()
    ) {
        companion object {
            const val DefaultPlayerCursorColor = "#FFFF00"
            const val DefaultScissorsColor = "#FFFFFF00"
        }
    }

    /**
     * Actions taken with a successful scissors click
     * @param goTo True if the editor goes to the given target entry
     * @param askForName True if a renaming dialog is opened for the target entry
     * @param play True if the target entry's audio is played
     */
    @Serializable
    @Immutable
    data class ScissorsActions(
        val goTo: Target = DefaultGoTo,
        val askForName: Target = DefaultAskForName,
        val play: Target = DefaultPlay
    ) {
        /**
         * Targets of the actions.
         * Either of the two entries created by the scissors' cut
         */
        @Serializable
        @Immutable
        enum class Target : LocalizedText {
            @SerialName("none")
            None {
                override val stringKey: Strings
                    get() = Strings.PreferencesEditorScissorsActionTargetNone
            },

            @SerialName("former")
            Former {
                override val stringKey: Strings
                    get() = Strings.PreferencesEditorScissorsActionTargetFormer
            },

            @SerialName("latter")
            Latter {
                override val stringKey: Strings
                    get() = Strings.PreferencesEditorScissorsActionTargetLatter
            }
        }

        companion object {
            val DefaultGoTo = Target.Former
            val DefaultAskForName = Target.Former
            val DefaultPlay = Target.Former
        }
    }

    /**
     * Define when should `scroll to editable area` automatically fire
     * @param onLoadedNewSample True if the action is conducted when a new sample file is loaded
     * @param onJumpedToEntry True if the action is conducted when jumped to an entry via entry list
     * @param onSwitchedInMultipleEditMode True if action is conducted in multiple edit mode when
     * switch to another entry
     */
    @Serializable
    @Immutable
    data class AutoScroll(
        val onLoadedNewSample: Boolean = DefaultOnLoadedNewSample,
        val onJumpedToEntry: Boolean = DefaultOnJumpedToEntry,
        val onSwitchedInMultipleEditMode: Boolean = DefaultOnSwitchedInMultipleEditMode
    ) {

        companion object {
            const val DefaultOnLoadedNewSample = true
            const val DefaultOnJumpedToEntry = true
            const val DefaultOnSwitchedInMultipleEditMode = true
        }
    }

    /**
     * Define when and how to conduct auto-save
     * @param target whether to conduct auto-save, and where to save
     * @param intervalSec interval between auto-save (in seconds)
     */
    @Serializable
    @Immutable
    data class AutoSave(
        val target: Target = DefaultTarget,
        val intervalSec: Int = DefaultIntervalSec
    ) {
        /**
         * Targets of the auto-save
         */
        @Serializable
        @Immutable
        enum class Target : LocalizedText {

            /**
             * Do not conduct auto-save
             */
            @SerialName("none")
            None {
                override val stringKey: Strings
                    get() = Strings.PreferencesAutoSaveTargetNone
            },

            /**
             * Save to the current project file
             */
            @SerialName("project")
            Project {
                override val stringKey: Strings
                    get() = Strings.PreferencesAutoSaveTargetProject
            },

            /**
             * Save to application record directory.
             * Will be discarded when the application is normally closed.
             */
            @SerialName("record")
            Record {
                override val stringKey: Strings
                    get() = Strings.PreferencesAutoSaveTargetRecord
            }
        }

        companion object {
            val DefaultTarget = Target.Record
            const val DefaultIntervalSec = 30
            const val MinIntervalSec = 1
        }
    }
}
