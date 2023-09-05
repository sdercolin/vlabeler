package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseScrollActionKeyBind
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.ui.string.ClickableTag
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.divideWithBigDecimal
import com.sdercolin.vlabeler.util.multiplyWithBigDecimal
import java.awt.Desktop

object PreferencesPages {

    object Charts : PreferencesPage(Strings.PreferencesCharts, Strings.PreferencesChartsDescription) {

        override val children
            get() = listOf(
                ChartsCanvas,
                ChartsWaveform,
                ChartsSpectrogram,
                ChartsPower,
                ChartsConversion,
            )
    }

    object ChartsCanvas : PreferencesPage(Strings.PreferencesChartsCanvas, Strings.PreferencesChartsCanvasDescription) {

        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                group(
                    Strings.PreferencesChartsCanvasResolution,
                    Strings.PreferencesChartsCanvasResolutionDescription,
                ) {
                    withContext(
                        selector = { it.painter.canvasResolution },
                        updater = { copy(painter = painter.copy(canvasResolution = it)) },
                    ) {
                        integer(
                            title = Strings.PreferencesChartsCanvasResolutionDefault,
                            defaultValue = AppConf.CanvasResolution.DefaultDefault,
                            min = AppConf.CanvasResolution.Min,
                            max = AppConf.CanvasResolution.Max,
                            select = { it.default },
                            update = { copy(default = it) },
                        )
                        integer(
                            title = Strings.PreferencesChartsCanvasResolutionStep,
                            defaultValue = AppConf.CanvasResolution.DefaultStep,
                            min = 1,
                            select = { it.step },
                            update = { copy(step = it) },
                        )
                    }
                }
                withContext(
                    selector = { it.painter },
                    updater = { copy(painter = it) },
                ) {
                    integer(
                        title = Strings.PreferencesChartsMaxDataChunkSize,
                        description = Strings.PreferencesChartsMaxDataChunkSizeDescription,
                        defaultValue = AppConf.CanvasResolution.DefaultStep,
                        min = AppConf.Painter.MinMaxDataChunkSize,
                        max = AppConf.Painter.MaxMaxDataChunkSize,
                        select = { it.maxDataChunkSize },
                        update = { copy(maxDataChunkSize = it) },
                    )
                }
            }
    }

    object ChartsWaveform : PreferencesPage(
        Strings.PreferencesChartsWaveform,
        Strings.PreferencesChartsWaveformDescription,
    ) {

        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.painter.amplitude },
                    updater = { copy(painter = painter.copy(amplitude = it)) },
                ) {
                    integer(
                        title = Strings.PreferencesChartsWaveformResampleDownTo,
                        description = Strings.PreferencesChartsWaveformResampleDownToDescription,
                        defaultValue = AppConf.Amplitude.DefaultResampleDownToHz,
                        min = AppConf.Amplitude.MinResampleDownToHz,
                        select = { it.resampleDownToHz },
                        update = { copy(resampleDownToHz = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsWaveformNormalize,
                        description = Strings.PreferencesChartsWaveformNormalizeDescription,
                        defaultValue = AppConf.Amplitude.DefaultNormalize,
                        select = { it.normalize },
                        update = { copy(normalize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsWaveformUnitSize,
                        description = Strings.PreferencesChartsWaveformUnitSizeDescription,
                        defaultValue = AppConf.Amplitude.DefaultUnitSize,
                        min = AppConf.Amplitude.MinUnitSize,
                        max = AppConf.Amplitude.MaxUnitSize,
                        select = { it.unitSize },
                        update = { copy(unitSize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsWaveformIntensityAccuracy,
                        defaultValue = AppConf.Amplitude.DefaultIntensityAccuracy,
                        min = AppConf.Amplitude.MinIntensityAccuracy,
                        max = AppConf.Amplitude.MaxIntensityAccuracy,
                        select = { it.intensityAccuracy },
                        update = { copy(intensityAccuracy = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsWaveformYAxisBlankRate,
                        defaultValue = AppConf.Amplitude.DefaultYAxisBlankRate,
                        min = AppConf.Amplitude.MinYAxisBlankRate,
                        max = AppConf.Amplitude.MaxYAxisBlankRate,
                        select = { it.yAxisBlankRate },
                        update = { copy(yAxisBlankRate = it) },
                    )
                    color(
                        title = Strings.PreferencesChartsWaveformColor,
                        defaultValue = AppConf.Amplitude.DefaultColor,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = true,
                    )
                    color(
                        title = Strings.PreferencesChartsWaveformBackgroundColor,
                        defaultValue = AppConf.Amplitude.DefaultBackgroundColor,
                        select = { it.backgroundColor },
                        update = { copy(backgroundColor = it) },
                        useAlpha = true,
                    )
                }
            }
    }

    object ChartsSpectrogram : PreferencesPage(
        Strings.PreferencesChartsSpectrogram,
        Strings.PreferencesChartsSpectrogramDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.painter.spectrogram },
                    updater = { copy(painter = painter.copy(spectrogram = it)) },
                ) {
                    switch(
                        title = Strings.PreferencesChartsSpectrogramEnabled,
                        defaultValue = AppConf.Spectrogram.DefaultEnabled,
                        select = { it.enabled },
                        update = { copy(enabled = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsSpectrogramHeight,
                        defaultValue = AppConf.Spectrogram.DefaultHeightWeight,
                        min = AppConf.Spectrogram.MinHeightWeight,
                        max = AppConf.Spectrogram.MaxHeightWeight,
                        select = { it.heightWeight },
                        update = { copy(heightWeight = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramPointDensity,
                        description = Strings.PreferencesChartsSpectrogramPointDensityDescription,
                        defaultValue = AppConf.Spectrogram.DefaultPointDensity,
                        min = AppConf.Spectrogram.MinPointDensity,
                        max = AppConf.Spectrogram.MaxPointDensity,
                        select = { it.pointDensity },
                        update = { copy(pointDensity = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramHopSize,
                        defaultValue = AppConf.Spectrogram.DefaultStandardHopSize,
                        description = Strings.PreferencesChartsSpectrogramHopSizeDescription,
                        min = AppConf.Spectrogram.MinStandardHopSize,
                        max = AppConf.Spectrogram.MaxStandardHopSize,
                        select = { it.standardHopSize },
                        update = { copy(standardHopSize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramWindowSize,
                        description = Strings.PreferencesChartsSpectrogramWindowSizeDescription,
                        defaultValue = AppConf.Spectrogram.DefaultStandardWindowSize,
                        min = AppConf.Spectrogram.MinStandardWindowSize,
                        max = AppConf.Spectrogram.MaxStandardWindowSize,
                        select = { it.standardWindowSize },
                        update = { copy(standardWindowSize = it) },
                    )
                    selection(
                        title = Strings.PreferencesChartsSpectrogramWindowType,
                        defaultValue = AppConf.Spectrogram.DefaultWindowType,
                        select = { it.windowType },
                        update = { copy(windowType = it) },
                        options = AppConf.WindowType.values(),
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMelScaleStep,
                        defaultValue = AppConf.Spectrogram.DefaultMelScaleStep,
                        min = AppConf.Spectrogram.MinMelScaleStep,
                        max = AppConf.Spectrogram.MaxMelScaleStep,
                        select = { it.melScaleStep },
                        update = { copy(melScaleStep = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMaxFrequency,
                        defaultValue = AppConf.Spectrogram.DefaultMaxFrequency,
                        min = AppConf.Spectrogram.MinMaxFrequency,
                        max = AppConf.Spectrogram.MaxMaxFrequency,
                        select = { it.maxFrequency },
                        update = { copy(maxFrequency = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMinIntensity,
                        defaultValue = AppConf.Spectrogram.DefaultMinIntensity,
                        select = { it.minIntensity },
                        update = { copy(minIntensity = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.spectrogram.let { it.minIntensity < it.maxIntensity }
                                },
                                prompt = Strings.PreferencesChartsSpectrogramMinIntensityInvalid,
                            ),
                        ),
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMaxIntensity,
                        defaultValue = AppConf.Spectrogram.DefaultMaxIntensity,
                        select = { it.maxIntensity },
                        update = { copy(maxIntensity = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.spectrogram.let { it.minIntensity < it.maxIntensity }
                                },
                                prompt = Strings.PreferencesChartsSpectrogramMaxIntensityInvalid,
                            ),
                        ),
                    )
                    selection(
                        title = Strings.PreferencesChartsSpectrogramColorPalette,
                        defaultValue = AppConf.Spectrogram.DefaultColorPalette,
                        description = Strings.PreferencesChartsSpectrogramColorPaletteDescription,
                        clickableTags = listOf(
                            ClickableTag(
                                tag = "edit",
                                onClick = { Desktop.getDesktop().open(ColorPaletteRepository.directory) },
                            ),
                        ),
                        select = { it.colorPalette },
                        update = { copy(colorPalette = it) },
                        options = ColorPaletteRepository.getAll().map { it.name }.toTypedArray(),
                    )
                    switch(
                        title = Strings.PreferencesChartsSpectrogramUseHighAlphaContrast,
                        defaultValue = AppConf.Spectrogram.DefaultUseHighAlphaContrast,
                        description = Strings.PreferencesChartsSpectrogramUseHighAlphaContrastDescription,
                        select = { it.useHighAlphaContrast },
                        update = { copy(useHighAlphaContrast = it) },
                    )
                }
            }
    }

    object ChartsPower : PreferencesPage(
        Strings.PreferencesChartsPower,
        Strings.PreferencesChartsPowerDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.painter.power },
                    updater = { copy(painter = painter.copy(power = it)) },
                ) {
                    switch(
                        title = Strings.PreferencesChartsPowerEnabled,
                        defaultValue = AppConf.Power.DefaultEnabled,
                        select = { it.enabled },
                        update = { copy(enabled = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsPowerMergeChannels,
                        defaultValue = AppConf.Power.DefaultMergeChannels,
                        select = { it.mergeChannels },
                        update = { copy(mergeChannels = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsPowerHeight,
                        defaultValue = AppConf.Power.DefaultHeightWeight,
                        min = AppConf.Power.MinHeightWeight,
                        max = AppConf.Power.MaxHeightWeight,
                        select = { it.heightWeight },
                        update = { copy(heightWeight = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsPowerUnitSize,
                        description = Strings.PreferencesChartsPowerUnitSizeDescription,
                        defaultValue = AppConf.Power.DefaultUnitSize,
                        min = AppConf.Power.MinUnitSize,
                        max = AppConf.Power.MaxUnitSize,
                        select = { it.unitSize },
                        update = { copy(unitSize = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.power.let { it.unitSize <= it.windowSize }
                                },
                                prompt = Strings.PreferencesChartsPowerUnitSizeInvalid,
                            ),
                        ),
                    )
                    integer(
                        title = Strings.PreferencesChartsPowerWindowSize,
                        defaultValue = AppConf.Power.DefaultWindowSize,
                        min = AppConf.Power.MinWindowSize,
                        max = AppConf.Power.MaxWindowSize,
                        select = { it.windowSize },
                        update = { copy(windowSize = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.power.let { it.unitSize <= it.windowSize }
                                },
                                prompt = Strings.PreferencesChartsPowerWindowSizeInvalid,
                            ),
                        ),
                    )
                    float(
                        title = Strings.PreferencesChartsPowerMinPower,
                        defaultValue = AppConf.Power.DefaultMinPower,
                        min = AppConf.Power.MinMinPower,
                        max = AppConf.Power.MaxMaxPower,
                        select = { it.minPower },
                        update = { copy(minPower = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.power.let { it.minPower < it.maxPower }
                                },
                                prompt = Strings.PreferencesChartsPowerMinPowerInvalid,
                            ),
                        ),
                    )
                    float(
                        title = Strings.PreferencesChartsPowerMaxPower,
                        defaultValue = AppConf.Power.DefaultMaxPower,
                        min = AppConf.Power.MinMinPower,
                        max = AppConf.Power.MaxMaxPower,
                        select = { it.maxPower },
                        update = { copy(maxPower = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.power.let { it.minPower < it.maxPower }
                                },
                                prompt = Strings.PreferencesChartsPowerMaxPowerInvalid,
                            ),
                        ),
                    )
                    integer(
                        title = Strings.PreferencesChartsPowerIntensityAccuracy,
                        defaultValue = AppConf.Power.DefaultIntensityAccuracy,
                        min = AppConf.Power.MinIntensityAccuracy,
                        max = AppConf.Power.MaxIntensityAccuracy,
                        select = { it.intensityAccuracy },
                        update = { copy(intensityAccuracy = it) },
                    )
                    color(
                        title = Strings.PreferencesChartsPowerColor,
                        defaultValue = AppConf.Power.DefaultColor,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = true,
                    )
                    color(
                        title = Strings.PreferencesChartsPowerBackgroundColor,
                        defaultValue = AppConf.Power.DefaultBackgroundColor,
                        select = { it.backgroundColor },
                        update = { copy(backgroundColor = it) },
                        useAlpha = true,
                    )
                }
            }
    }

    object ChartsConversion : PreferencesPage(
        Strings.PreferencesChartsConversion,
        Strings.PreferencesChartsConversionDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.painter.conversion },
                    updater = { copy(painter = painter.copy(conversion = it)) },
                ) {
                    text(
                        title = Strings.PreferencesChartsConversionFFmpegPath,
                        description = Strings.PreferencesChartsConversionFFmpegPathDescription,
                        defaultValue = AppConf.Conversion.DefaultFFmpegPath,
                        clickableTags = listOf(
                            ClickableTag(
                                tag = "open",
                                onClick = { Url.open("https://www.ffmpeg.org/") },
                            ),
                        ),
                        select = { it.ffmpegPath },
                        update = { copy(ffmpegPath = it) },
                    )
                    text(
                        title = Strings.PreferencesChartsConversionFFmpegArgs,
                        defaultValue = AppConf.Conversion.DefaultFFmpegArgs,
                        select = { it.ffmpegArgs },
                        update = { copy(ffmpegArgs = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsConversionFFmpegUseForWav,
                        defaultValue = AppConf.Conversion.DefaultUseConversionForWav,
                        select = { it.useConversionForWav },
                        update = { copy(useConversionForWav = it) },
                    )
                }
            }
    }

    object Keymap : PreferencesPage(Strings.PreferencesKeymap, Strings.PreferencesKeymapDescription) {

        override val children: List<PreferencesPage>
            get() = listOf(KeymapKeyAction, KeymapMouseClickAction, KeymapMouseScrollAction)
    }

    object KeymapKeyAction : PreferencesPage(
        Strings.PreferencesKeymapKeyAction,
        Strings.PreferencesKeymapKeyActionDescription,
        scrollable = false,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.keymaps },
                    updater = { copy(keymaps = it) },
                ) {
                    keymap(
                        actionType = ActionType.Key,
                        defaultValue = listOf(),
                        select = { parent -> parent.keyActionMap.map { KeyActionKeyBind(it.key, it.value) } },
                        update = { list -> copy(keyActionMap = list.associate { it.action to it.keySet }) },
                    )
                }
            }
    }

    object KeymapMouseClickAction : PreferencesPage(
        Strings.PreferencesKeymapMouseClickAction,
        Strings.PreferencesKeymapMouseClickActionDescription,
        scrollable = false,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.keymaps },
                    updater = { copy(keymaps = it) },
                ) {
                    keymap(
                        actionType = ActionType.MouseClick,
                        defaultValue = listOf(),
                        select = { parent ->
                            parent.mouseClickActionMap.map {
                                MouseClickActionKeyBind(
                                    it.key,
                                    it.value,
                                )
                            }
                        },
                        update = { list -> copy(mouseClickActionMap = list.associate { it.action to it.keySet }) },
                    )
                }
            }
    }

    object KeymapMouseScrollAction : PreferencesPage(
        Strings.PreferencesKeymapMouseScrollAction,
        Strings.PreferencesKeymapMouseScrollActionDescription,
        scrollable = false,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.keymaps },
                    updater = { copy(keymaps = it) },
                ) {
                    keymap(
                        actionType = ActionType.MouseScroll,
                        defaultValue = listOf(),
                        select = { parent ->
                            parent.mouseScrollActionMap.map { MouseScrollActionKeyBind(it.key, it.value) }
                        },
                        update = { list -> copy(mouseScrollActionMap = list.associate { it.action to it.keySet }) },
                    )
                }
            }
    }

    object View : PreferencesPage(Strings.PreferencesView, Strings.PreferencesViewDescription) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.view },
                    updater = { copy(view = it) },
                ) {
                    selection(
                        title = Strings.PreferencesViewLanguage,
                        defaultValue = AppConf.View.DefaultLanguage,
                        select = { it.language },
                        update = { copy(language = it) },
                        options = Language.values(),
                    )
                    switch(
                        title = Strings.PreferencesViewHideSampleExtension,
                        defaultValue = AppConf.View.DefaultHideSampleExtension,
                        select = { it.hideSampleExtension },
                        update = { copy(hideSampleExtension = it) },
                    )
                    color(
                        title = Strings.PreferencesViewAppAccentColor,
                        defaultValue = AppConf.View.DefaultAccentColor,
                        select = { it.accentColor },
                        update = { copy(accentColor = it) },
                        useAlpha = false,
                    )
                    color(
                        title = Strings.PreferencesViewAppAccentColorVariant,
                        defaultValue = AppConf.View.DefaultAccentColorVariant,
                        select = { it.accentColorVariant },
                        update = { copy(accentColorVariant = it) },
                        useAlpha = false,
                    )
                    selection(
                        title = Strings.PreferencesViewPinnedEntryListPosition,
                        defaultValue = AppConf.View.DefaultPinnedEntryListPosition,
                        select = { it.pinnedEntryListPosition },
                        update = { copy(pinnedEntryListPosition = it) },
                        options = AppConf.ViewPosition.values(),
                    )
                }
            }
    }

    object Editor : PreferencesPage(Strings.PreferencesEditor, Strings.PreferencesEditorDescription) {

        override val children: List<PreferencesPage>
            get() = listOf(
                EditorScissors,
                EditorAutoScroll,
                EditorNotes,
                EditorContinuousLabelNames,
                EditorPostEditAction,
            )

        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.editor },
                    updater = { copy(editor = it) },
                ) {
                    selection(
                        title = Strings.PreferencesEditorLockedDrag,
                        description = Strings.PreferencesEditorLockedDragDescription,
                        columnStyle = true,
                        defaultValue = AppConf.Editor.DefaultLockedDrag,
                        select = { it.lockedDrag },
                        update = { copy(lockedDrag = it) },
                        options = AppConf.Editor.LockedDrag.values(),
                    )
                    switch(
                        title = Strings.PreferencesEditorLockedSettingParameterWithCursor,
                        description = Strings.PreferencesEditorLockedSettingParameterWithCursorDescription,
                        defaultValue = AppConf.Editor.DefaultLockedSettingParameterWithCursor,
                        select = { it.lockedSettingParameterWithCursor },
                        update = { copy(lockedSettingParameterWithCursor = it) },
                    )
                    color(
                        title = Strings.PreferencesEditorPlayerCursorColor,
                        defaultValue = AppConf.Editor.DefaultPlayerCursorColor,
                        useAlpha = false,
                        select = { it.playerCursorColor },
                        update = { copy(playerCursorColor = it) },
                    )
                }
            }
    }

    object EditorScissors : PreferencesPage(
        Strings.PreferencesEditorScissors,
        Strings.PreferencesEditorScissorsDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.editor },
                    updater = { copy(editor = it) },
                ) {
                    switch(
                        title = Strings.PreferencesEditorScissorsUseOnScreenScissors,
                        description = Strings.PreferencesEditorScissorsUseOnScreenScissorsDescription,
                        defaultValue = AppConf.Editor.DefaultUseOnScreenScissors,
                        select = { it.useOnScreenScissors },
                        update = { copy(useOnScreenScissors = it) },
                    )
                    color(
                        title = Strings.PreferencesEditorScissorsColor,
                        defaultValue = AppConf.Editor.DefaultScissorsColor,
                        useAlpha = true,
                        select = { it.scissorsColor },
                        update = { copy(scissorsColor = it) },
                    )
                }
                withContext(
                    selector = { it.editor.scissorsActions },
                    updater = { copy(editor = editor.copy(scissorsActions = it)) },
                ) {
                    selection(
                        title = Strings.PreferencesEditorScissorsActionGoTo,
                        defaultValue = AppConf.ScissorsActions.DefaultGoTo,
                        select = { it.goTo },
                        update = { copy(goTo = it) },
                        options = AppConf.ScissorsActions.Target.values(),
                    )
                    selection(
                        title = Strings.PreferencesEditorScissorsActionAskForName,
                        defaultValue = AppConf.ScissorsActions.DefaultAskForName,
                        select = { it.askForName },
                        update = { copy(askForName = it) },
                        options = AppConf.ScissorsActions.Target.values(),
                    )
                    selection(
                        title = Strings.PreferencesEditorScissorsActionPlay,
                        defaultValue = AppConf.ScissorsActions.DefaultPlay,
                        select = { it.play },
                        update = { copy(play = it) },
                        options = AppConf.ScissorsActions.Target.values(),
                    )
                }
            }
    }

    object EditorNotes : PreferencesPage(
        Strings.PreferencesEditorNotes,
        Strings.PreferencesEditorNotesDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.editor },
                    updater = { copy(editor = it) },
                ) {
                    switch(
                        title = Strings.PreferencesEditorShowDone,
                        defaultValue = AppConf.Editor.DefaultShowDone,
                        select = { it.showDone },
                        update = { copy(showDone = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowStarred,
                        defaultValue = AppConf.Editor.DefaultShowStar,
                        select = { it.showStar },
                        update = { copy(showStar = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowTag,
                        defaultValue = AppConf.Editor.DefaultShowTag,
                        select = { it.showTag },
                        update = { copy(showTag = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowExtra,
                        description = Strings.PreferencesEditorShowExtraDescription,
                        defaultValue = AppConf.Editor.DefaultShowExtra,
                        select = { it.showExtra },
                        update = { copy(showExtra = it) },
                    )
                }
            }
    }

    object EditorPostEditAction : PreferencesPage(
        Strings.PreferencesEditorPostEditAction,
        Strings.PreferencesEditorPostEditActionDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                group(Strings.PreferencesEditorPostEditActionDone) {
                    withContext(
                        selector = { it.editor.postEditDone },
                        updater = { copy(editor = editor.copy(postEditDone = it)) },
                    ) {
                        switch(
                            title = Strings.PreferencesEditorPostEditActionEnabled,
                            defaultValue = AppConf.PostEditAction.DefaultDone.enabled,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        selection(
                            title = Strings.PreferencesEditorPostEditActionTrigger,
                            defaultValue = AppConf.PostEditAction.DefaultDone.field,
                            options = AppConf.PostEditAction.TriggerField.values(),
                            select = { it.field },
                            update = { copy(field = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseDragging,
                            description = Strings.PreferencesEditorPostEditActionUseDraggingDescription,
                            defaultValue = AppConf.PostEditAction.DefaultDone.useDragging,
                            select = { it.useDragging },
                            update = { copy(useDragging = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseCursorSet,
                            description = Strings.PreferencesEditorPostEditActionUseCursorSetDescription,
                            defaultValue = AppConf.PostEditAction.DefaultDone.useCursorSet,
                            select = { it.useCursorSet },
                            update = { copy(useCursorSet = it) },
                            enabled = { it.enabled },
                        )
                    }
                }
                group(Strings.PreferencesEditorPostEditActionNext) {
                    withContext(
                        selector = { it.editor.postEditNext },
                        updater = { copy(editor = editor.copy(postEditNext = it)) },
                    ) {
                        switch(
                            title = Strings.PreferencesEditorPostEditActionEnabled,
                            defaultValue = AppConf.PostEditAction.DefaultNext.enabled,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        selection(
                            title = Strings.PreferencesEditorPostEditActionTrigger,
                            defaultValue = AppConf.PostEditAction.DefaultNext.field,
                            options = AppConf.PostEditAction.TriggerField.values(),
                            select = { it.field },
                            update = { copy(field = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseDragging,
                            description = Strings.PreferencesEditorPostEditActionUseDraggingDescription,
                            defaultValue = AppConf.PostEditAction.DefaultNext.useDragging,
                            select = { it.useDragging },
                            update = { copy(useDragging = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseCursorSet,
                            description = Strings.PreferencesEditorPostEditActionUseCursorSetDescription,
                            defaultValue = AppConf.PostEditAction.DefaultNext.useCursorSet,
                            select = { it.useCursorSet },
                            update = { copy(useCursorSet = it) },
                            enabled = { it.enabled },
                        )
                    }
                }
            }
    }

    object EditorAutoScroll : PreferencesPage(
        Strings.PreferencesEditorAutoScroll,
        Strings.PreferencesEditorAutoScrollDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.editor.autoScroll },
                    updater = { copy(editor = editor.copy(autoScroll = it)) },
                ) {
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnLoadedNewSample,
                        defaultValue = AppConf.AutoScroll.DefaultOnLoadedNewSample,
                        select = { it.onLoadedNewSample },
                        update = { copy(onLoadedNewSample = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnJumpedToEntry,
                        defaultValue = AppConf.AutoScroll.DefaultOnJumpedToEntry,
                        select = { it.onJumpedToEntry },
                        update = { copy(onJumpedToEntry = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnSwitchedInMultipleEditMode,
                        defaultValue = AppConf.AutoScroll.DefaultOnSwitchedInMultipleEditMode,
                        select = { it.onSwitchedInMultipleEditMode },
                        update = { copy(onSwitchedInMultipleEditMode = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnSwitched,
                        defaultValue = AppConf.AutoScroll.DefaultOnSwitched,
                        select = { it.onSwitched },
                        update = { copy(onSwitched = it) },
                    )
                }
            }
    }

    object EditorContinuousLabelNames : PreferencesPage(
        Strings.PreferencesEditorContinuousLabelNames,
        Strings.PreferencesEditorContinuousLabelNamesDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.editor.continuousLabelNames },
                    updater = { copy(editor = editor.copy(continuousLabelNames = it)) },
                ) {
                    color(
                        title = Strings.PreferencesEditorContinuousLabelNamesColor,
                        defaultValue = AppConf.ContinuousLabelNames.DefaultColor,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = false,
                    )
                    selection(
                        title = Strings.PreferencesEditorContinuousLabelNamesSize,
                        defaultValue = AppConf.ContinuousLabelNames.DefaultSize,
                        select = { it.size },
                        update = { copy(size = it) },
                        options = AppConf.FontSize.values(),
                    )
                    selection(
                        title = Strings.PreferencesEditorContinuousLabelNamesPosition,
                        defaultValue = AppConf.ContinuousLabelNames.DefaultPosition,
                        select = { it.position },
                        update = { copy(position = it) },
                        options = AppConf.ViewCornerPosition.values(),
                    )
                }
            }
    }

    object Playback : PreferencesPage(Strings.PreferencesPlayback, Strings.PreferencesPlaybackDescription) {

        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                group(
                    Strings.PreferencesPlaybackPlayOnDragging,
                    Strings.PreferencesPlaybackPlayOnDraggingDescription,
                ) {
                    withContext(
                        selector = { it.playback.playOnDragging },
                        updater = { copy(playback = playback.copy(playOnDragging = it)) },
                    ) {
                        switch(
                            title = Strings.PreferencesPlaybackPlayOnDraggingEnabled,
                            defaultValue = AppConf.PlayOnDragging.DefaultPlayOnDraggingEnabled,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        integer(
                            title = Strings.PreferencesPlaybackPlayOnDraggingRangeRadiusMillis,
                            defaultValue = AppConf.PlayOnDragging.DefaultPlayOnDraggingRangeRadiusMillis,
                            min = AppConf.PlayOnDragging.MinPlayOnDraggingRangeRadiusMillis,
                            max = AppConf.PlayOnDragging.MaxPlayOnDraggingRangeRadiusMillis,
                            select = { it.rangeRadiusMillis },
                            update = { copy(rangeRadiusMillis = it) },
                        )
                        integer(
                            title = Strings.PreferencesPlaybackPlayOnDraggingEventQueueSize,
                            defaultValue = AppConf.PlayOnDragging.DefaultPlayOnDraggingEventQueueSize,
                            min = AppConf.PlayOnDragging.MinPlayOnDraggingEventQueueSize,
                            max = AppConf.PlayOnDragging.MaxPlayOnDraggingEventQueueSize,
                            select = { it.eventQueueSize },
                            update = { copy(eventQueueSize = it) },
                        )
                    }
                }
            }
    }

    object AutoSave : PreferencesPage(Strings.PreferencesAutoSave, Strings.PreferencesAutoSaveDescription) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.autoSave },
                    updater = { copy(autoSave = it) },
                ) {
                    selection(
                        title = Strings.PreferencesAutoSaveTarget,
                        defaultValue = AppConf.AutoSave.DefaultTarget,
                        select = { it.target },
                        update = { copy(target = it) },
                        options = AppConf.AutoSave.Target.values(),
                    )
                    integer(
                        title = Strings.PreferencesAutoSaveIntervalSec,
                        defaultValue = AppConf.AutoSave.DefaultIntervalSec,
                        min = AppConf.AutoSave.MinIntervalSec,
                        select = { it.intervalSec },
                        update = { copy(intervalSec = it) },
                    )
                }
            }
    }

    object History : PreferencesPage(Strings.PreferencesHistory, Strings.PreferencesHistoryDescription) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.history },
                    updater = { copy(history = it) },
                ) {
                    integer(
                        title = Strings.PreferencesHistoryMaxSize,
                        defaultValue = AppConf.History.DefaultMaxSize,
                        min = AppConf.History.MinMaxSize,
                        select = { it.maxSize },
                        update = { copy(maxSize = it) },
                    )
                    switch(
                        title = Strings.PreferencesHistorySquashIndex,
                        description = Strings.PreferencesHistorySquashIndexDescription,
                        defaultValue = AppConf.History.DefaultSquashIndex,
                        select = { it.squashIndex },
                        update = { copy(squashIndex = it) },
                    )
                }
            }
    }

    object Misc : PreferencesPage(Strings.PreferencesMisc, Strings.PreferencesMiscDescription) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.misc },
                    updater = { copy(misc = it) },
                ) {
                    switch(
                        title = Strings.PreferencesMiscUseCustomFileDialog,
                        description = Strings.PreferencesMiscUseCustomFileDialogDescription,
                        defaultValue = AppConf.Misc.DefaultUseCustomFileDialog,
                        select = { it.useCustomFileDialog },
                        update = { copy(useCustomFileDialog = it) },
                    )
                }
            }
    }

    val rootPages: Array<PreferencesPage>
        get() = arrayOf(
            Charts,
            Keymap,
            View,
            Editor,
            Playback,
            AutoSave,
            History,
            Misc,
        )
}

private class PreferencesItemContext<P>(
    val builder: PageContentBuilder,
    val selector: (AppConf) -> P,
    val updater: AppConf.(P) -> AppConf,
) {
    private fun <T> selectWithContext(select: (P) -> T): (AppConf) -> T = {
        select(selector(it))
    }

    private fun <T> updateWithContext(update: P.(T) -> P): AppConf.(T) -> AppConf = {
        val parent = selector(this)
        updater(parent.update(it))
    }

    fun switch(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: Boolean,
        select: (P) -> Boolean,
        update: P.(Boolean) -> P,
        enabled: (P) -> Boolean = { true },
    ) = builder.item(
        PreferencesItem.Switch(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
        ),
    )

    fun integer(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: Int,
        min: Int? = null,
        max: Int? = null,
        select: (P) -> Int,
        update: P.(Int) -> P,
        enabled: (P) -> Boolean = { true },
        validationRules: List<PreferencesItemValidationRule> = listOf(),
    ) = builder.item(
        PreferencesItem.IntegerInput(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
            validationRules = validationRules,
        ),
    )

    fun floatPercentage(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: Float,
        min: Float? = null,
        max: Float? = null,
        select: (P) -> Float,
        update: P.(Float) -> P,
        enabled: (P) -> Boolean = { true },
        validationRules: List<PreferencesItemValidationRule> = listOf(),
    ) = float(
        title = title,
        description = description,
        clickableTags = clickableTags,
        columnStyle = columnStyle,
        defaultValue = defaultValue.multiplyWithBigDecimal(100f),
        min = min?.multiplyWithBigDecimal(100f),
        max = max?.multiplyWithBigDecimal(100f),
        select = { select(it).multiplyWithBigDecimal(100f) },
        update = { update(it.divideWithBigDecimal(100f)) },
        enabled = enabled,
        validationRules = validationRules,
    )

    fun float(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: Float,
        min: Float? = null,
        max: Float? = null,
        select: (P) -> Float,
        update: P.(Float) -> P,
        enabled: (P) -> Boolean = { true },
        validationRules: List<PreferencesItemValidationRule> = listOf(),
    ) = builder.item(
        PreferencesItem.FloatInput(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
            validationRules = validationRules,
        ),
    )

    fun text(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: String,
        select: (P) -> String,
        update: P.(String) -> P,
        enabled: (P) -> Boolean = { true },
        validationRules: List<PreferencesItemValidationRule> = listOf(),
    ) = builder.item(
        PreferencesItem.StringInput(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
            validationRules = validationRules,
        ),
    )

    fun color(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: String,
        select: (P) -> String,
        update: P.(String) -> P,
        enabled: (P) -> Boolean = { true },
        useAlpha: Boolean,
    ) = builder.item(
        PreferencesItem.ColorStringInput(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
            useAlpha = useAlpha,
        ),
    )

    fun <T> selection(
        title: Strings?,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        columnStyle: Boolean = false,
        defaultValue: T,
        select: (P) -> T,
        update: P.(T) -> P,
        enabled: (P) -> Boolean = { true },
        options: Array<T>,
    ) = builder.item(
        PreferencesItem.Selection(
            title = title,
            description = description,
            clickableTags = clickableTags,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
            options = options,
        ),
    )

    fun <K : Action> keymap(
        actionType: ActionType,
        defaultValue: List<ActionKeyBind<K>>,
        select: (P) -> List<ActionKeyBind<K>>,
        update: P.(List<ActionKeyBind<K>>) -> P,
    ) = builder.item(
        PreferencesItem.Keymap(
            actionType = actionType,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
        ),
    )
}

private class PageContentBuilder {
    private val content = mutableListOf<PreferencesGroup>()
    private var currentGroup = mutableListOf<PreferencesItem<*>>()

    fun <P> withContext(
        selector: (AppConf) -> P,
        updater: AppConf.(P) -> AppConf,
        block: PreferencesItemContext<P>.() -> Unit,
    ) {
        PreferencesItemContext(this, selector, updater).block()
    }

    fun item(item: PreferencesItem<*>) {
        currentGroup.add(item)
    }

    fun group(name: Strings, description: Strings? = null, block: PageContentBuilder.() -> Unit) {
        pushCurrentGroup()
        block()
        content.add(PreferencesGroup(name, description, currentGroup))
        currentGroup = mutableListOf()
    }

    fun build(): List<PreferencesGroup> {
        pushCurrentGroup()
        return content
    }

    private fun pushCurrentGroup() {
        if (currentGroup.isNotEmpty()) {
            content.add(PreferencesGroup(null, null, currentGroup))
            currentGroup = mutableListOf()
        }
    }
}

private fun buildPageContent(block: PageContentBuilder.() -> Unit): List<PreferencesGroup> {
    val builder = PageContentBuilder()
    builder.block()
    return builder.build()
}
