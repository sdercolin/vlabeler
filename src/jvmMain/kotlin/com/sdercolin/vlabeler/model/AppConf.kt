package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.MouseScrollAction
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.model.palette.ColorPaletteDefinition
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.repository.update.model.UpdateChannel
import com.sdercolin.vlabeler.ui.string.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Basic configurations of this app.
 *
 * @param painter Configurations about chart painting.
 * @param editor Configurations about editor.
 * @param view Configurations about views.
 * @param autoSave Configurations about auto-save.
 * @param autoReload Configurations about auto-reload.
 * @param playback Configurations about audio playback.
 * @param keymaps Custom keymap.
 * @param history Configurations about edit history (undo/redo).
 * @param misc Other miscellaneous configurations.
 */
@Serializable
@Immutable
data class AppConf(
    val painter: Painter = Painter(),
    val editor: Editor = Editor(),
    val view: View = View(),
    val autoSave: AutoSave = AutoSave(),
    val autoReload: AutoReload = AutoReload(),
    val playback: Playback = Playback(),
    val keymaps: Keymaps = Keymaps(),
    val history: History = History(),
    val misc: Misc = Misc(),
) {
    /**
     * Configurations about chart painting.
     *
     * @param canvasResolution Configurations about the canvas's resolution.
     * @param maxDataChunkSize Max number of sample points in one chunk drawn in the painter.
     * @param amplitude Configurations about amplitude (waveforms) painting.
     * @param spectrogram Configurations about spectrogram painting.
     * @param conversion Configurations about audio file conversion.
     */
    @Serializable
    @Immutable
    data class Painter(
        val canvasResolution: CanvasResolution = CanvasResolution(),
        val maxDataChunkSize: Int = DEFAULT_MAX_DATA_CHUNK_SIZE,
        val amplitude: Amplitude = Amplitude(),
        val spectrogram: Spectrogram = Spectrogram(),
        val power: Power = Power(),
        val fundamental: Fundamental = Fundamental(),
        val conversion: Conversion = Conversion(),
    ) {
        val amplitudeHeightRatio: Float
            get() = 1f /
                (
                    1f + (if (spectrogram.enabled) spectrogram.heightWeight else 0f) +
                        (if (power.enabled) power.heightWeight else 0f) +
                        (if (fundamental.enabled) fundamental.heightWeight else 0f)
                    )

        companion object {
            const val DEFAULT_MAX_DATA_CHUNK_SIZE = 441000
            const val MIN_MAX_DATA_CHUNK_SIZE = DEFAULT_MAX_DATA_CHUNK_SIZE / 2
            const val MAX_MAX_DATA_CHUNK_SIZE = DEFAULT_MAX_DATA_CHUNK_SIZE * 3
        }
    }

    /**
     * Configurations about the canvas's resolution. This resolution is defined as number of sample points included in 1
     * pixel.
     *
     * @param default Default value used when editor is launched.
     * @param step Linear step length when resolution is changed by "+" "-" buttons.
     */
    @Serializable
    @Immutable
    data class CanvasResolution(
        val default: Int = DEFAULT_DEFAULT,
        val step: Int = DEFAULT_STEP,
    ) {
        val min: Int get() = MIN
        val max: Int get() = MAX

        companion object {
            const val MAX = 400
            const val MIN = 10
            const val DEFAULT_DEFAULT = 40
            const val DEFAULT_STEP = 20
        }
    }

    /**
     * Configurations about amplitude (waveforms) painting.
     *
     * @param resampleDownToHz Maximum sample rate for loading. If the audio has a higher sample rate, it will be
     *    resampled down to this value. If set to 0, the original sample rate is used.
     * @param unitSize Frames of one pixel used when drawing the waveform.
     * @param intensityAccuracy Height of the container bitmap in pixel.
     * @param yAxisBlankRate Height rate of the extra blank region displayed in both top and bottom to the height of the
     *    waveform.
     * @param color Color of the waveform.
     * @param backgroundColor Background color of the waveform.
     */
    @Serializable
    @Immutable
    data class Amplitude(
        val resampleDownToHz: Int = DEFAULT_RESAMPLE_DOWN_TO_HZ,
        val normalize: Boolean = DEFAULT_NORMALIZE,
        val unitSize: Int = DEFAULT_UNIT_SIZE,
        val intensityAccuracy: Int = DEFAULT_INTENSITY_ACCURACY,
        val yAxisBlankRate: Float = DEFAULT_YAXIS_BLANK_RATE,
        val color: String = DEFAULT_COLOR,
        val backgroundColor: String = DEFAULT_BACKGROUND_COLOR,
    ) {
        companion object {
            const val DEFAULT_RESAMPLE_DOWN_TO_HZ = 44100
            const val MIN_RESAMPLE_DOWN_TO_HZ = 0
            const val DEFAULT_NORMALIZE = false
            const val DEFAULT_UNIT_SIZE = 60
            const val MAX_UNIT_SIZE = DEFAULT_UNIT_SIZE * 10
            const val MIN_UNIT_SIZE = 1
            const val DEFAULT_INTENSITY_ACCURACY = 500
            const val MAX_INTENSITY_ACCURACY = DEFAULT_INTENSITY_ACCURACY * 5
            const val MIN_INTENSITY_ACCURACY = DEFAULT_INTENSITY_ACCURACY / 5
            const val DEFAULT_YAXIS_BLANK_RATE = 0.1f
            const val MAX_YAXIS_BLANK_RATE = 3f
            const val MIN_YAXIS_BLANK_RATE = 0f
            const val DEFAULT_COLOR = "#FFF2F2F2"
            const val DEFAULT_BACKGROUND_COLOR = "#00000000"
        }
    }

    /**
     * Configurations about spectrogram painting.
     *
     * @param enabled True if spectrogram is calculated and shown.
     * @param heightWeight Height weight of the spectrogram to the amplitude form (whose weight is 1).
     * @param pointDensity Points drawn into one pixel.
     * @param standardHopSize Distance as the number of samples for which the window is slided when move to the next
     *    frame. This value is used for cases with sample rate 48000 Hz. For other sample rates it is calculated
     *    linear-proportionally.
     * @param standardWindowSize Number of samples in the window. This value is used for cases with sample rate 48000
     *    Hz. For other sample rates it is calculated exponential-proportionally (base is 2).
     * @param windowType Window type used in the Short-Time FT. See [WindowType] for options.
     * @param melScaleStep Step of the mel scale for interpolation on the frequency axis.
     * @param maxFrequency Max frequency (Hz) displayed.
     * @param minIntensity Min intensity (dB) displayed in the heatmap.
     * @param maxIntensity Max intensity (dB) displayed in the heatmap.
     * @param colorPalette Color palette name used in the heatmap. See [ColorPaletteDefinition] for details.
     * @param useHighAlphaContrast True if the alpha value of the color is used repeatedly in the heatmap, so that the
     *    heatmap looks more contrasted. Only color palettes with alpha values are affected.
     */
    @Serializable
    @Immutable
    data class Spectrogram(
        val enabled: Boolean = DEFAULT_ENABLED,
        val heightWeight: Float = DEFAULT_HEIGHT_WEIGHT,
        val pointDensity: Int = DEFAULT_POINT_DENSITY,
        val standardHopSize: Int = DEFAULT_STANDARD_HOP_SIZE,
        val standardWindowSize: Int = DEFAULT_STANDARD_WINDOW_SIZE,
        val windowType: WindowType = DEFAULT_WINDOW_TYPE,
        val melScaleStep: Int = DEFAULT_MEL_SCALE_STEP,
        val maxFrequency: Int = DEFAULT_MAX_FREQUENCY,
        val minIntensity: Int = DEFAULT_MIN_INTENSITY,
        val maxIntensity: Int = DEFAULT_MAX_INTENSITY,
        val colorPalette: String = DEFAULT_COLOR_PALETTE,
        val useHighAlphaContrast: Boolean = DEFAULT_USE_HIGH_ALPHA_CONTRAST,
    ) {
        companion object {
            const val DEFAULT_ENABLED = true
            const val DEFAULT_HEIGHT_WEIGHT = 0.75f
            const val MAX_HEIGHT_WEIGHT = 5f
            const val MIN_HEIGHT_WEIGHT = 0.1f
            const val DEFAULT_POINT_DENSITY = 2
            const val MAX_POINT_DENSITY = 30
            const val MIN_POINT_DENSITY = 1
            const val DEFAULT_STANDARD_HOP_SIZE = 110
            const val MAX_STANDARD_HOP_SIZE = 2048
            const val MIN_STANDARD_HOP_SIZE = 1
            const val DEFAULT_STANDARD_WINDOW_SIZE = 512
            const val MAX_STANDARD_WINDOW_SIZE = 4096
            const val MIN_STANDARD_WINDOW_SIZE = 128
            const val DEFAULT_MAX_FREQUENCY = 20000
            const val DEFAULT_MEL_SCALE_STEP = 10
            const val MAX_MEL_SCALE_STEP = 100
            const val MIN_MEL_SCALE_STEP = 1
            const val MAX_MAX_FREQUENCY = 48000
            const val MIN_MAX_FREQUENCY = 5000
            const val DEFAULT_MIN_INTENSITY = -20
            const val DEFAULT_MAX_INTENSITY = 55
            val DEFAULT_WINDOW_TYPE = WindowType.BlackmanHarris
            val DEFAULT_COLOR_PALETTE = ColorPaletteDefinition.presets.first().name
            const val DEFAULT_USE_HIGH_ALPHA_CONTRAST = true
        }
    }

    /**
     * Configurations about power painting.
     *
     * @param enabled True if power is calculated and shown.
     * @param mergeChannels True if the power of all channels are merged into one.
     * @param heightWeight Height weight of the power to the amplitude form (whose weight is 1).
     * @param unitSize Frames of one pixel used when drawing the power.
     * @param windowSize Number of frames in the window.
     * @param minPower Min power (dB) displayed in the graph.
     * @param maxPower Max power (dB) displayed in the graph.
     * @param intensityAccuracy Height of the container bitmap in pixel.
     * @param color Color of the power graph.
     * @param backgroundColor Background color of the power graph.
     */
    @Serializable
    @Immutable
    data class Power(
        val enabled: Boolean = DEFAULT_ENABLED,
        val mergeChannels: Boolean = DEFAULT_MERGE_CHANNELS,
        val heightWeight: Float = DEFAULT_HEIGHT_WEIGHT,
        val unitSize: Int = DEFAULT_UNIT_SIZE,
        val windowSize: Int = DEFAULT_WINDOW_SIZE,
        val minPower: Float = DEFAULT_MIN_POWER,
        val maxPower: Float = DEFAULT_MAX_POWER,
        val intensityAccuracy: Int = DEFAULT_INTENSITY_ACCURACY,
        val color: String = DEFAULT_COLOR,
        val backgroundColor: String = DEFAULT_BACKGROUND_COLOR,
    ) {
        companion object {
            const val DEFAULT_ENABLED = false
            const val DEFAULT_MERGE_CHANNELS = true
            const val DEFAULT_HEIGHT_WEIGHT = 0.5f
            const val MAX_HEIGHT_WEIGHT = 5f
            const val MIN_HEIGHT_WEIGHT = 0.1f
            const val DEFAULT_UNIT_SIZE = 60
            const val MAX_UNIT_SIZE = DEFAULT_UNIT_SIZE * 10
            const val MIN_UNIT_SIZE = 1
            const val DEFAULT_WINDOW_SIZE = 300
            const val MAX_WINDOW_SIZE = DEFAULT_WINDOW_SIZE * 10
            const val MIN_WINDOW_SIZE = 1
            const val DEFAULT_MIN_POWER = -48f
            const val DEFAULT_MAX_POWER = 0.0f
            const val MIN_MIN_POWER = -192.66f
            const val MAX_MAX_POWER = 0.0f
            const val DEFAULT_INTENSITY_ACCURACY = 200
            const val MAX_INTENSITY_ACCURACY = DEFAULT_INTENSITY_ACCURACY * 5
            const val MIN_INTENSITY_ACCURACY = DEFAULT_INTENSITY_ACCURACY / 5
            const val DEFAULT_COLOR = "#FFF2F2F2"
            const val DEFAULT_BACKGROUND_COLOR = "#00000000"
        }
    }

    @Serializable
    @Immutable
    data class Fundamental(
        val enabled: Boolean = DEFAULT_ENABLED,
        val heightWeight: Float = DEFAULT_HEIGHT_WEIGHT,
        val semitoneResolution: Int = DEFAULT_SEMITONE_RESOLUTION,
        val minFundamental: Float = DEFAULT_MIN_FUNDAMENTAL,
        val maxFundamental: Float = DEFAULT_MAX_FUNDAMENTAL,
        val semitoneSampleNum: Int = DEFAULT_SEMITONE_SAMPLE_NUM,
        val maxHarmonicFrequency: Float = DEFAULT_MAX_HARMONIC_FREQUENCY,
        // hidden to users
        val erbsStep: Float = DEFAULT_ERBS_STEP,
        // hidden to users
        val minDisplayCorr: Float = DEFAULT_MIN_DISPLAY_CORR,
        // hidden to users
        val maxDisplayCorr: Float = DEFAULT_MAX_DISPLAY_CORR,
        val drawReferenceLine: Boolean = DEFAULT_DRAW_REFERENCE_LINE,
        val color: String = DEFAULT_COLOR,
        val referenceLineColor: String = DEFAULT_REFERENCE_LINE_COLOR,
        val backgroundColor: String = DEFAULT_BACKGROUND_COLOR,
    ) {
        companion object {
            const val DEFAULT_ENABLED = false
            const val DEFAULT_HEIGHT_WEIGHT = 0.5f
            const val MAX_HEIGHT_WEIGHT = 5f
            const val MIN_HEIGHT_WEIGHT = 0.1f
            const val DEFAULT_SEMITONE_RESOLUTION = 8
            const val MIN_SEMITONE_RESOLUTION = 1
            const val MAX_SEMITONE_RESOLUTION = 64
            const val DEFAULT_MIN_FUNDAMENTAL = 130.0f // C3
            const val DEFAULT_MAX_FUNDAMENTAL = 880.0f // A5
            const val MIN_FUNDAMENTAL = 16.351f // C0
            const val MAX_FUNDAMENTAL = 8372.0f // C9
            const val DEFAULT_SEMITONE_SAMPLE_NUM = 8
            const val MAX_SEMITONE_SAMPLE_NUM = 16
            const val DEFAULT_MAX_HARMONIC_FREQUENCY = 5000.0f
            const val MAX_MAX_HARMONIC_FREQUENCY = 22050.0f
            const val DEFAULT_ERBS_STEP = 0.1f
            const val DEFAULT_MIN_DISPLAY_CORR = 0.0f
            const val DEFAULT_MAX_DISPLAY_CORR = 0.5f
            const val DEFAULT_DRAW_REFERENCE_LINE = true
            const val DEFAULT_COLOR = "#FFFFC500"
            const val DEFAULT_REFERENCE_LINE_COLOR = "#FF555555"
            const val DEFAULT_BACKGROUND_COLOR = "#00000000"
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
     * Configurations about conversion.
     *
     * @param ffmpegPath Path to the ffmpeg executable.
     * @param ffmpegArgs Arguments passed to ffmpeg besides input and output file paths.
     * @param useConversionForWav True if the conversion is used for wav files as well.
     */
    @Serializable
    @Immutable
    data class Conversion(
        val ffmpegPath: String = DEFAULT_FFMPEG_PATH,
        val ffmpegArgs: String = DEFAULT_FFMPEG_ARGS,
        val useConversionForWav: Boolean = DEFAULT_USE_CONVERSION_FOR_WAV,
    ) {
        companion object {
            const val DEFAULT_FFMPEG_PATH = "ffmpeg"
            const val DEFAULT_FFMPEG_ARGS = "-acodec pcm_s16le -ac 1 -ar 44100"
            const val DEFAULT_USE_CONVERSION_FOR_WAV = false
        }
    }

    /**
     * Configurations about editor behaviors.
     *
     * @param scissorsColor Color hex string of the scissors' cursor position.
     * @param scissorsActions Actions taken with a successful scissors click.
     * @param useOnScreenScissors When true, the scissors process is handled on screen. Otherwise, it is handled in a
     *    dialog. Only effective when [Project.multipleEditMode] is true.
     * @param scissorsSubmitThreshold The dp number of the threshold to submit the scissors' cut after a click, when
     *    [useOnScreenScissors] is true.
     * @param autoScroll Timings when `scroll to editable area` is automatically conducted.
     * @param showDone When true, the done button/icon is shown in the editor and entry lists.
     * @param showStar When true, the star button/icon is shown in the editor and entry lists.
     * @param showTag When true, the tag or "New tag" button is shown in the editor and entry lists.
     * @param showExtra When true, the extra editor button/icon is shown in the editor and entry lists.
     * @param continuousLabelNames Appearance of the label names in the editor for continuous labelers.
     */
    @Serializable
    @Immutable
    data class Editor(
        val playerCursorColor: String = DEFAULT_PLAYER_CURSOR_COLOR,
        val scissorsColor: String = DEFAULT_SCISSORS_COLOR,
        val scissorsActions: ScissorsActions = ScissorsActions(),
        val useOnScreenScissors: Boolean = DEFAULT_USE_ON_SCREEN_SCISSORS,
        val scissorsSubmitThreshold: Int = DEFAULT_SCISSORS_SUBMIT_THRESHOLD,
        val autoScroll: AutoScroll = AutoScroll(),
        val lockedDrag: LockedDrag = DEFAULT_LOCKED_DRAG,
        val lockedSettingParameterWithCursor: Boolean = DEFAULT_LOCKED_SETTING_PARAMETER_WITH_CURSOR,
        val showDone: Boolean = DEFAULT_SHOW_DONE,
        val showStar: Boolean = DEFAULT_SHOW_STAR,
        val showTag: Boolean = DEFAULT_SHOW_TAG,
        val showExtra: Boolean = DEFAULT_SHOW_EXTRA,
        val continuousLabelNames: ContinuousLabelNames = ContinuousLabelNames(),
        val postEditNext: PostEditAction = PostEditAction.DEFAULT_NEXT,
        val postEditDone: PostEditAction = PostEditAction.DEFAULT_DONE,
    ) {

        /**
         * Condition for locked drag
         */
        @Serializable
        @Immutable
        enum class LockedDrag(override val stringKey: Strings) : LocalizedText {
            @SerialName("Labeler")
            UseLabeler(Strings.PreferencesEditorLockedDragUseLabeler),

            @SerialName("Start")
            UseStart(Strings.PreferencesEditorLockedDragUseStart),

            @SerialName("Never")
            Never(Strings.PreferencesEditorLockedDragNever),
        }

        companion object {
            const val DEFAULT_PLAYER_CURSOR_COLOR = "#FFFF00"
            const val DEFAULT_SCISSORS_COLOR = "#FFFFFF00"
            const val DEFAULT_USE_ON_SCREEN_SCISSORS = true
            const val DEFAULT_SCISSORS_SUBMIT_THRESHOLD = 10
            const val MIN_SCISSORS_SUBMIT_THRESHOLD = 1
            const val MAX_SCISSORS_SUBMIT_THRESHOLD = 50
            val DEFAULT_LOCKED_DRAG = LockedDrag.UseLabeler
            const val DEFAULT_LOCKED_SETTING_PARAMETER_WITH_CURSOR = true
            const val DEFAULT_SHOW_DONE = true
            const val DEFAULT_SHOW_STAR = true
            const val DEFAULT_SHOW_TAG = true
            const val DEFAULT_SHOW_EXTRA = true
        }
    }

    /**
     * Actions taken with a successful scissors click.
     *
     * @param goTo True if the editor goes to the given target entry.
     * @param askForName True if a renaming dialog is opened for the target entry.
     * @param play True if the target entry's audio is played.
     */
    @Serializable
    @Immutable
    data class ScissorsActions(
        val goTo: Target = DEFAULT_GO_TO,
        val askForName: Target = DEFAULT_ASK_FOR_NAME,
        val play: Target = DEFAULT_PLAY,
    ) {
        fun getTargetEntryIndex(currentEntryIndex: Int) = when (goTo) {
            AppConf.ScissorsActions.Target.Former -> currentEntryIndex
            AppConf.ScissorsActions.Target.Latter -> currentEntryIndex + 1
            else -> null
        }

        /**
         * Targets of the actions. Either of the two entries created by the scissors' cut.
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
            val DEFAULT_GO_TO = Target.Latter
            val DEFAULT_ASK_FOR_NAME = Target.Former
            val DEFAULT_PLAY = Target.Former
        }
    }

    /**
     * Configurations about the appearance of the label names in the editor for continuous labelers.
     *
     * @param color Color hex string of the label names.
     * @param size Font size of the label names.
     * @param position Position of the label names at corners. `Bottom` here means the bottom of waveforms.
     * @param backgroundColor Background color of the label names.
     * @param editableBackgroundColor Background color of the label names when editing.
     */
    @Serializable
    @Immutable
    data class ContinuousLabelNames(
        val color: String = DEFAULT_COLOR,
        val size: FontSize = DEFAULT_SIZE,
        val position: ViewPosition = DEFAULT_POSITION,
        val backgroundColor: String = DEFAULT_BACKGROUND_COLOR,
        val editableBackgroundColor: String = DEFAULT_EDITABLE_BACKGROUND_COLOR,
    ) {

        companion object {
            const val DEFAULT_COLOR = "#E89F17"
            val DEFAULT_SIZE = FontSize.Small
            val DEFAULT_POSITION = ViewPosition.TopRight
            const val DEFAULT_BACKGROUND_COLOR = "#00000000"
            const val DEFAULT_EDITABLE_BACKGROUND_COLOR = "#19FFFFFF"
        }
    }

    /**
     * Action config after editing.
     *
     * @param enabled True if the action is enabled.
     * @param field Trigger field of the action.
     * @param useDragging True if the action is conducted when dragging.
     * @param useCursorSet True if the action is conducted when set by cursor.
     */
    @Serializable
    @Immutable
    data class PostEditAction(
        val enabled: Boolean,
        val field: TriggerField,
        val useDragging: Boolean,
        val useCursorSet: Boolean,
    ) {

        /**
         * Trigger field of the [PostEditAction].
         */
        @Serializable
        @Immutable
        enum class TriggerField(override val stringKey: Strings) : LocalizedText {
            @SerialName("Labeler")
            UseLabeler(Strings.PreferencesEditorPostEditActionTriggerUseLabeler),

            @SerialName("Start")
            UseStart(Strings.PreferencesEditorPostEditActionTriggerUseStart),

            @SerialName("End")
            UseEnd(Strings.PreferencesEditorPostEditActionTriggerUseEnd),

            @SerialName("Any")
            UseAny(Strings.PreferencesEditorPostEditActionTriggerUseAny),
        }

        enum class Type {
            Next,
            Done
        }

        companion object {

            val DEFAULT_NEXT = PostEditAction(
                enabled = false,
                field = TriggerField.UseLabeler,
                useDragging = true,
                useCursorSet = true,
            )

            val DEFAULT_DONE = PostEditAction(
                enabled = true,
                field = TriggerField.UseAny,
                useDragging = true,
                useCursorSet = true,
            )
        }
    }

    /**
     * Configurations about views.
     *
     * @param language Language of the app.
     * @param fontFamilyName Name of the font family used in the app.
     * @param hideSampleExtension When true, the extension of sample file names is hidden in the editor and entry lists.
     * @param accentColor Color hex string of the accent color.
     * @param accentColorVariant Color hex string of the accent color variant.
     * @param pinnedEntryListPosition Position of the pinned entry list in the window.
     */
    @Serializable
    @Immutable
    data class View(
        val language: Language = DEFAULT_LANGUAGE,
        val fontFamilyName: String = DEFAULT_FONT_FAMILY_NAME,
        val hideSampleExtension: Boolean = DEFAULT_HIDE_SAMPLE_EXTENSION,
        val accentColor: String = DEFAULT_ACCENT_COLOR,
        val accentColorVariant: String = DEFAULT_ACCENT_COLOR_VARIANT,
        val pinnedEntryListPosition: ViewSidePosition = DEFAULT_PINNED_ENTRY_LIST_POSITION,
    ) {

        companion object {

            val DEFAULT_LANGUAGE = Language.English
            const val DEFAULT_HIDE_SAMPLE_EXTENSION = true

            val DEFAULT_FONT_FAMILY_NAME = FontRepository.FontOption.BuiltIn.Default.name

            /**
             * Equals to [com.sdercolin.vlabeler.ui.theme.Pink]
             */
            const val DEFAULT_ACCENT_COLOR = "#F48FB1"

            /**
             * Equals to [com.sdercolin.vlabeler.ui.theme.DarkPink]
             */
            const val DEFAULT_ACCENT_COLOR_VARIANT = "#AD375F"
            val DEFAULT_PINNED_ENTRY_LIST_POSITION = ViewSidePosition.Right
        }
    }

    /**
     * Position options of views that are attached to the sides of the window.
     */
    @Immutable
    enum class ViewSidePosition(override val stringKey: Strings) : LocalizedText {
        Left(Strings.PreferencesViewPositionLeft),
        Right(Strings.PreferencesViewPositionRight),
        Top(Strings.PreferencesViewPositionTop),
        Bottom(Strings.PreferencesViewPositionBottom),
    }

    /**
     * Position options of views.
     */
    @Immutable
    enum class ViewPosition(
        override val stringKey: Strings,
        val left: Boolean,
        val top: Boolean,
        val bottom: Boolean,
    ) : LocalizedText {
        TopLeft(Strings.PreferencesViewCornerPositionTopLeft, left = true, top = true, bottom = false),
        TopRight(Strings.PreferencesViewCornerPositionTopRight, left = false, top = true, bottom = false),
        CenterLeft(Strings.PreferencesViewCornerPositionCenterLeft, left = true, top = false, bottom = false),
        CenterRight(Strings.PreferencesViewCornerPositionCenterRight, left = false, top = false, bottom = false),
        BottomLeft(Strings.PreferencesViewCornerPositionBottomLeft, left = true, top = false, bottom = true),
        BottomRight(Strings.PreferencesViewCornerPositionBottomRight, left = false, top = false, bottom = true),
    }

    /**
     * Font size options.
     */
    @Immutable
    enum class FontSize(override val stringKey: Strings) : LocalizedText {
        Small(Strings.PreferencesFontSizeSmall),
        Medium(Strings.PreferencesFontSizeMedium),
        Large(Strings.PreferencesFontSizeLarge),
        ExtraLarge(Strings.PreferencesFontSizeExtraLarge),
    }

    /**
     * Define when should `scroll to editable area` automatically fire.
     *
     * @param onLoadedNewSample True if the action is conducted when a new sample file is loaded.
     * @param onJumpedToEntry True if the action is conducted when jumped to an entry via entry list.
     * @param onSwitchedInMultipleEditMode True if action is conducted in multiple edit mode when switch to another.
     *    entry.
     * @param onSwitched True if action is conducted when switched to another entry.
     */
    @Serializable
    @Immutable
    data class AutoScroll(
        val onLoadedNewSample: Boolean = DEFAULT_ON_LOADED_NEW_SAMPLE,
        val onJumpedToEntry: Boolean = DEFAULT_ON_JUMPED_TO_ENTRY,
        val onSwitchedInMultipleEditMode: Boolean = DEFAULT_ON_SWITCHED_IN_MULTIPLE_EDIT_MODE,
        val onSwitched: Boolean = DEFAULT_ON_SWITCHED,
    ) {

        companion object {
            const val DEFAULT_ON_LOADED_NEW_SAMPLE = true
            const val DEFAULT_ON_JUMPED_TO_ENTRY = true
            const val DEFAULT_ON_SWITCHED_IN_MULTIPLE_EDIT_MODE = true
            const val DEFAULT_ON_SWITCHED = false
        }
    }

    /**
     * Define when and how to conduct auto-save.
     *
     * @param target whether to conduct auto-save, and where to save.
     * @param intervalSec interval between auto-save (in seconds).
     */
    @Serializable
    @Immutable
    data class AutoSave(
        val target: Target = DEFAULT_TARGET,
        val intervalSec: Int = DEFAULT_INTERVAL_SEC,
    ) {
        /**
         * Targets of the auto-save.
         */
        @Serializable
        @Immutable
        enum class Target(override val stringKey: Strings) : LocalizedText {

            /**
             * Do not conduct auto-save.
             */
            @SerialName("none")
            None(Strings.PreferencesAutoSaveTargetNone),

            /**
             * Save to the current project file.
             */
            @SerialName("project")
            Project(Strings.PreferencesAutoSaveTargetProject),

            /**
             * Save to application record directory. Will be discarded when the application is normally closed.
             */
            @SerialName("record")
            Record(Strings.PreferencesAutoSaveTargetRecord)
        }

        companion object {
            val DEFAULT_TARGET = Target.Record
            const val DEFAULT_INTERVAL_SEC = 30
            const val MIN_INTERVAL_SEC = 1
        }
    }

    @Serializable
    @Immutable
    data class AutoReload(
        val behavior: Behavior = DEFAULT_BEHAVIOR,
    ) {

        /**
         * Behavior of the auto-reload.
         */
        @Serializable
        @Immutable
        enum class Behavior(override val stringKey: Strings) : LocalizedText {

            /**
             * Do not conduct auto-reload.
             */
            Disabled(Strings.PreferencesAutoReloadBehaviorDisabled),

            /**
             * Ask the user whether to reload with a preview.
             */
            AskWithDetails(Strings.PreferencesAutoReloadBehaviorAskWithDetails),

            /**
             * Ask the user whether to reload.
             */
            Ask(Strings.PreferencesAutoReloadBehaviorAsk),

            /**
             * Reload without asking.
             */
            Auto(Strings.PreferencesAutoReloadBehaviorAuto),
        }

        companion object {
            val DEFAULT_BEHAVIOR = Behavior.Ask
        }
    }

    /**
     * Configurations about playback.
     *
     * @param playOnDragging Configurations about playback preview on dragging.
     */
    @Serializable
    @Immutable
    data class Playback(
        val playOnDragging: PlayOnDragging = PlayOnDragging(),
    )

    /**
     * Configurations about playback preview on dragging.
     *
     * @param enabled True if the preview is enabled.
     * @param rangeRadiusMillis Radius of the preview half-range (in milliseconds).
     * @param eventQueueSize Max size of retained drag events.
     */
    @Serializable
    @Immutable
    data class PlayOnDragging(
        val enabled: Boolean = DEFAULT_PLAY_ON_DRAGGING_ENABLED,
        val rangeRadiusMillis: Int = DEFAULT_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS,
        val eventQueueSize: Int = DEFAULT_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE,
    ) {
        companion object {
            const val DEFAULT_PLAY_ON_DRAGGING_ENABLED = true
            const val DEFAULT_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS = 10
            const val MAX_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS = 100
            const val MIN_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS = 1
            const val DEFAULT_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE = 5
            const val MAX_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE = 100
            const val MIN_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE = 1
        }
    }

    /**
     * Custom keymaps.
     *
     * @param keyActionMap Custom keymap for [KeyAction]s.
     * @param mouseClickActionMap Custom keymap for [MouseClickAction]s.
     * @param mouseScrollActionMap Custom keymap for [MouseScrollAction]s.
     */
    @Serializable
    @Immutable
    data class Keymaps(
        val keyActionMap: Map<KeyAction, KeySet?> = mapOf(),
        val mouseClickActionMap: Map<MouseClickAction, KeySet?> = mapOf(),
        val mouseScrollActionMap: Map<MouseScrollAction, KeySet?> = mapOf(),
    )

    /**
     * Configurations about edit history (undo/redo).
     *
     * @param maxSize Max size of the edit history.
     * @param squashIndex Ignore changes that only contain different [Project.currentModuleIndex]s or
     *    [Module.currentIndex]s.
     */
    @Serializable
    @Immutable
    data class History(
        val maxSize: Int = DEFAULT_MAX_SIZE,
        val squashIndex: Boolean = DEFAULT_SQUASH_INDEX,
    ) {
        companion object {
            const val DEFAULT_MAX_SIZE = 100
            const val MIN_MAX_SIZE = 1
            const val DEFAULT_SQUASH_INDEX = true
        }
    }

    /**
     * Other miscellaneous configurations.
     *
     * @param updateChannel Update channel of the app.
     * @param useCustomFileDialog True if the custom file dialog is used instead of the system one.
     */
    @Serializable
    @Immutable
    data class Misc(
        val updateChannel: UpdateChannel = DEFAULT_UPDATE_CHANNEL,
        val useCustomFileDialog: Boolean = DEFAULT_USE_CUSTOM_FILE_DIALOG,
    ) {
        companion object {
            val DEFAULT_UPDATE_CHANNEL = UpdateChannel.Stable
            const val DEFAULT_USE_CUSTOM_FILE_DIALOG = false
        }
    }
}
