package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.editor.SpectrogramColorPalette
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedText
import com.sdercolin.vlabeler.ui.string.Strings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Basic configurations of app
 * @param painter Configurations about chart painting
 * @param editor Configurations about editor
 * @param view Configurations about views
 * @param autoSave Configurations about auto-save
 * @param playback Configurations about audio playback
 * @param keymaps Custom keymap
 * @param history Configurations about edit history (undo/redo)
 */
@Serializable
@Immutable
data class AppConf(
    val painter: Painter = Painter(),
    val editor: Editor = Editor(),
    val view: View = View(),
    val autoSave: AutoSave = AutoSave(),
    val playback: Playback = Playback(),
    val keymaps: Keymaps = Keymaps(),
    val history: History = History(),
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
        val spectrogram: Spectrogram = Spectrogram(),
    ) {
        val amplitudeHeightRatio: Float
            get() = 1f / (1f + spectrogram.heightWeight)

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
        val step: Int = DefaultStep,
    ) {
        val min: Int get() = Min
        val max: Int get() = Max

        companion object {
            const val Max = 400
            const val Min = 10
            const val DefaultDefault = 40
            const val DefaultStep = 20
        }
    }

    /**
     * Configurations about amplitude (waveforms) painting
     * @param resampleDownToHz Maximum sample rate for loading. If the audio has a higher sample rate, it will be
     * resampled down to this value. If set to 0, the original sample rate is used
     * @param unitSize Frames of one pixel used when drawing the waveform
     * @param intensityAccuracy Height of the container bitmap in pixel
     * @param yAxisBlankRate Height rate of the extra blank region displayed in both top and bottom to
     * the height of the waveform
     * @param color Color of the waveform
     * @param backgroundColor Background color of the waveform
     */
    @Serializable
    @Immutable
    data class Amplitude(
        val resampleDownToHz: Int = DefaultResampleDownToHz,
        val normalize: Boolean = DefaultNormalize,
        val unitSize: Int = DefaultUnitSize,
        val intensityAccuracy: Int = DefaultIntensityAccuracy,
        val yAxisBlankRate: Float = DefaultYAxisBlankRate,
        val color: String = DefaultColor,
        val backgroundColor: String = DefaultBackgroundColor,
    ) {
        companion object {
            const val DefaultResampleDownToHz = 44100
            const val MinResampleDownToHz = 0
            const val DefaultNormalize = false
            const val DefaultUnitSize = 60
            const val MaxUnitSize = DefaultUnitSize * 10
            const val MinUnitSize = 1
            const val DefaultIntensityAccuracy = 1000
            const val MaxIntensityAccuracy = DefaultIntensityAccuracy * 5
            const val MinIntensityAccuracy = DefaultIntensityAccuracy / 5
            const val DefaultYAxisBlankRate = 0.1f
            const val MaxYAxisBlankRate = 3f
            const val MinYAxisBlankRate = 0f
            const val DefaultColor = "#FFF2F2F2"
            const val DefaultBackgroundColor = "#00000000"
        }
    }

    /**
     * Configurations about spectrogram painting
     * @param enabled True if spectrogram is calculated and shown
     * @param heightWeight Height weight of the spectrogram to the amplitude form (whose weight is 1)
     * @param pointDensity Points drawn into one pixel
     * @param standardHopSize Distance as the number of samples for which the window is slided when move to the next
     * frame. This value is used for cases with sample rate 48000 Hz. For other sample rates it is calculated
     * linear-proportionally.
     * @param standardWindowSize Number of samples in the window. This value is used for cases with sample rate 48000
     * Hz. For other sample rates it is calculated exponential-proportionally (base is 2).
     * @param windowType Window type used in the Short-Time FT. See [WindowType] for options
     * @param melScaleStep Step of the mel scale for interpolation on the frequency axis
     * @param maxFrequency Max frequency (Hz) displayed
     * @param minIntensity Min intensity (dB) displayed in the heatmap
     * @param maxIntensity Max intensity (dB) displayed in the heatmap
     */
    @Serializable
    @Immutable
    data class Spectrogram(
        val enabled: Boolean = DefaultEnabled,
        val heightWeight: Float = DefaultHeightWeight,
        val pointDensity: Int = DefaultPointDensity,
        val standardHopSize: Int = DefaultStandardHopSize,
        val standardWindowSize: Int = DefaultStandardWindowSize,
        val windowType: WindowType = DefaultWindowType,
        val melScaleStep: Int = DefaultMelScaleStep,
        val maxFrequency: Int = DefaultMaxFrequency,
        val minIntensity: Int = DefaultMinIntensity,
        val maxIntensity: Int = DefaultMaxIntensity,
        val colorPalette: SpectrogramColorPalette.Presets = DefaultColorPalette,
    ) {
        companion object {
            const val DefaultEnabled = true
            const val DefaultHeightWeight = 0.75f
            const val MaxHeightWeight = 5f
            const val MinHeightWeight = 0.1f
            const val DefaultPointDensity = 2
            const val MaxPointDensity = 30
            const val MinPointDensity = 1
            const val DefaultStandardHopSize = 110
            const val MaxStandardHopSize = 2048
            const val MinStandardHopSize = 1
            const val DefaultStandardWindowSize = 512
            const val MaxStandardWindowSize = 4096
            const val MinStandardWindowSize = 128
            const val DefaultMaxFrequency = 20000
            const val DefaultMelScaleStep = 10
            const val MaxMelScaleStep = 100
            const val MinMelScaleStep = 1
            const val MaxMaxFrequency = 48000
            const val MinMaxFrequency = 5000
            const val DefaultMinIntensity = -20
            const val DefaultMaxIntensity = 55
            val DefaultWindowType = WindowType.BlackmanHarris
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
        Bartlett,
    }

    /**
     * Configurations about editor behaviors
     * @param scissorsColor Color hex string of the scissors' cursor position
     * @param scissorsActions Actions taken with a successful scissors click
     * @param autoScroll Timings when `scroll to editable area` is automatically conducted
     * @param showDone When true, the done button/icon is shown in the editor and entry lists
     * @param autoDone When true, the editor is automatically setting "done" status of entries
     * @param showStar When true, the star button/icon is shown in the editor and entry lists
     * @param showTag When true, the tag or "New tag" button is shown in the editor and entry lists
     * @param continuousLabelNames Appearance of the label names in the editor for continuous labelers
     */
    @Serializable
    @Immutable
    data class Editor(
        val playerCursorColor: String = DefaultPlayerCursorColor,
        val scissorsColor: String = DefaultScissorsColor,
        val scissorsActions: ScissorsActions = ScissorsActions(),
        val autoScroll: AutoScroll = AutoScroll(),
        val lockedDrag: LockedDrag = DefaultLockedDrag,
        val lockedSettingParameterWithCursor: Boolean = DefaultLockedSettingParameterWithCursor,
        val showDone: Boolean = DefaultShowDone,
        val autoDone: Boolean = DefaultAutoDone,
        val showStar: Boolean = DefaultShowStar,
        val showTag: Boolean = DefaultShowTag,
        val continuousLabelNames: ContinuousLabelNames = ContinuousLabelNames(),
    ) {

        /**
         * Condition for locked drag
         */
        @Serializable
        @Immutable
        enum class LockedDrag(override val stringKey: Strings) : LocalizedText {
            @SerialName("Labeler")
            UseLabeler(Strings.PreferencesEditorPlayerLockedDragUseLabeler),

            @SerialName("Start")
            UseStart(Strings.PreferencesEditorPlayerLockedDragUseStart),

            @SerialName("Never")
            Never(Strings.PreferencesEditorPlayerLockedDragNever),
        }

        companion object {
            const val DefaultPlayerCursorColor = "#FFFF00"
            const val DefaultScissorsColor = "#FFFFFF00"
            val DefaultLockedDrag = LockedDrag.UseLabeler
            const val DefaultLockedSettingParameterWithCursor = true
            const val DefaultShowDone = true
            const val DefaultAutoDone = true
            const val DefaultShowStar = true
            const val DefaultShowTag = true
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
        val play: Target = DefaultPlay,
    ) {
        /**
         * Targets of the actions.
         * Either of the two entries created by the scissors' cut
         */
        @Serializable
        @Immutable
        enum class Target(override val stringKey: Strings) : LocalizedText {

            @SerialName("None")
            None(Strings.PreferencesEditorScissorsActionTargetNone),

            @SerialName("Former")
            Former(Strings.PreferencesEditorScissorsActionTargetFormer),

            @SerialName("Latter")
            Latter(Strings.PreferencesEditorScissorsActionTargetLatter),
        }

        companion object {
            val DefaultGoTo = Target.Former
            val DefaultAskForName = Target.Former
            val DefaultPlay = Target.Former
        }
    }

    /**
     * Configurations about the appearance of the label names in the editor for continuous labelers
     * @param color Color hex string of the label names
     * @param size Font size of the label names
     * @param position Position of the label names at corners. `Bottom` here means the bottom of waveforms
     */
    @Serializable
    @Immutable
    data class ContinuousLabelNames(
        val color: String = DefaultColor,
        val size: FontSize = DefaultSize,
        val position: ViewCornerPosition = DefaultPosition,
    ) {

        companion object {
            const val DefaultColor = "#E89F17"
            val DefaultSize = FontSize.Small
            val DefaultPosition = ViewCornerPosition.TopRight
        }
    }

    /**
     * Configurations about views
     * @param language Language of the app
     * @param hideSampleExtension When true, the extension of sample file names is hidden in the editor and entry lists
     * @param accentColor Color hex string of the accent color
     * @param accentColorVariant Color hex string of the accent color variant
     * @param pinnedEntryListPosition Position of the pinned entry list in the window
     */
    @Serializable
    @Immutable
    data class View(
        val language: Language = DefaultLanguage,
        val hideSampleExtension: Boolean = DefaultHideSampleExtension,
        val accentColor: String = DefaultAccentColor,
        val accentColorVariant: String = DefaultAccentColorVariant,
        val pinnedEntryListPosition: ViewPosition = DefaultPinnedEntryListPosition,
    ) {

        companion object {

            val DefaultLanguage = Language.English
            const val DefaultHideSampleExtension = true

            /**
             * Equals to [com.sdercolin.vlabeler.ui.theme.Pink]
             */
            const val DefaultAccentColor = "#F48FB1"

            /**
             * Equals to [com.sdercolin.vlabeler.ui.theme.DarkPink]
             */
            const val DefaultAccentColorVariant = "#AD375F"
            val DefaultPinnedEntryListPosition = ViewPosition.Right
        }
    }

    /**
     * Position options of views
     */
    @Immutable
    enum class ViewPosition(override val stringKey: Strings) : LocalizedText {
        Left(Strings.PreferencesViewPositionLeft),
        Right(Strings.PreferencesViewPositionRight),
        Top(Strings.PreferencesViewPositionTop),
        Bottom(Strings.PreferencesViewPositionBottom),
    }

    /**
     * Position options of views at corners
     */
    @Immutable
    enum class ViewCornerPosition(
        override val stringKey: Strings,
        val left: Boolean,
        val top: Boolean,
    ) : LocalizedText {
        TopLeft(Strings.PreferencesViewCornerPositionTopLeft, left = true, top = true),
        TopRight(Strings.PreferencesViewCornerPositionTopRight, left = false, top = true),
        BottomLeft(Strings.PreferencesViewCornerPositionBottomLeft, left = true, top = false),
        BottomRight(Strings.PreferencesViewCornerPositionBottomRight, left = false, top = false),
    }

    /**
     * Font size options
     */
    @Immutable
    enum class FontSize(override val stringKey: Strings) : LocalizedText {
        Small(Strings.PreferencesFontSizeSmall),
        Medium(Strings.PreferencesFontSizeMedium),
        Large(Strings.PreferencesFontSizeLarge),
    }

    /**
     * Define when should `scroll to editable area` automatically fire
     * @param onLoadedNewSample True if the action is conducted when a new sample file is loaded
     * @param onJumpedToEntry True if the action is conducted when jumped to an entry via entry list
     * @param onSwitchedInMultipleEditMode True if action is conducted in multiple edit mode when
     * switch to another entry
     * @param onSwitched True if action is conducted when switched to another entry
     */
    @Serializable
    @Immutable
    data class AutoScroll(
        val onLoadedNewSample: Boolean = DefaultOnLoadedNewSample,
        val onJumpedToEntry: Boolean = DefaultOnJumpedToEntry,
        val onSwitchedInMultipleEditMode: Boolean = DefaultOnSwitchedInMultipleEditMode,
        val onSwitched: Boolean = DefaultOnSwitched,
    ) {

        companion object {
            const val DefaultOnLoadedNewSample = true
            const val DefaultOnJumpedToEntry = true
            const val DefaultOnSwitchedInMultipleEditMode = true
            const val DefaultOnSwitched = false
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
        val intervalSec: Int = DefaultIntervalSec,
    ) {
        /**
         * Targets of the auto-save
         */
        @Serializable
        @Immutable
        enum class Target(override val stringKey: Strings) : LocalizedText {

            /**
             * Do not conduct auto-save
             */
            @SerialName("none")
            None(Strings.PreferencesAutoSaveTargetNone),

            /**
             * Save to the current project file
             */
            @SerialName("project")
            Project(Strings.PreferencesAutoSaveTargetProject),

            /**
             * Save to application record directory.
             * Will be discarded when the application is normally closed.
             */
            @SerialName("record")
            Record(Strings.PreferencesAutoSaveTargetRecord)
        }

        companion object {
            val DefaultTarget = Target.Record
            const val DefaultIntervalSec = 30
            const val MinIntervalSec = 1
        }
    }

    /**
     * Configurations about playback
     * @param playOnDragging Configurations about playback preview on dragging
     */
    @Serializable
    @Immutable
    data class Playback(
        val playOnDragging: PlayOnDragging = PlayOnDragging(),
    )

    /**
     * Configurations about playback preview on dragging
     * @param enabled True if the preview is enabled
     * @param rangeRadiusMillis Radius of the preview half-range (in milliseconds)
     * @param eventQueueSize Max size of retained drag events
     */
    @Serializable
    @Immutable
    data class PlayOnDragging(
        val enabled: Boolean = DefaultPlayOnDraggingEnabled,
        val rangeRadiusMillis: Int = DefaultPlayOnDraggingRangeRadiusMillis,
        val eventQueueSize: Int = DefaultPlayOnDraggingEventQueueSize,
    ) {
        companion object {
            const val DefaultPlayOnDraggingEnabled = true
            const val DefaultPlayOnDraggingRangeRadiusMillis = 10
            const val MaxPlayOnDraggingRangeRadiusMillis = 100
            const val MinPlayOnDraggingRangeRadiusMillis = 1
            const val DefaultPlayOnDraggingEventQueueSize = 5
            const val MaxPlayOnDraggingEventQueueSize = 100
            const val MinPlayOnDraggingEventQueueSize = 1
        }
    }

    /**
     * Custom keymaps
     * @param keyActionMap Custom keymap for [KeyAction]s
     * @param mouseClickActionMap Custom keymap for [MouseClickAction]s
     * @param mouseScrollActionMap Custom keymap for [MouseScrollAction]s
     */
    @Serializable
    @Immutable
    data class Keymaps(
        val keyActionMap: Map<KeyAction, KeySet?> = mapOf(),
        val mouseClickActionMap: Map<MouseClickAction, KeySet?> = mapOf(),
        val mouseScrollActionMap: Map<MouseScrollAction, KeySet?> = mapOf(),
    )

    /**
     * Configurations about edit history (undo/redo)
     * @param maxSize Max size of the edit history
     * @param squashIndex Ignore changes that contain a different [Project.currentIndex]
     */
    @Serializable
    @Immutable
    data class History(
        val maxSize: Int = DefaultMaxSize,
        val squashIndex: Boolean = DefaultSquashIndex,
    ) {
        companion object {
            const val DefaultMaxSize = 100
            const val MinMaxSize = 1
            const val DefaultSquashIndex = true
        }
    }
}
