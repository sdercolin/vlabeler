package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseScrollActionKeyBind
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.repository.FontRepository
import com.sdercolin.vlabeler.repository.update.model.UpdateChannel
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*
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
                ChartsFundamental,
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
                            defaultValue = AppConf.CanvasResolution.DEFAULT_DEFAULT,
                            min = AppConf.CanvasResolution.MIN,
                            max = AppConf.CanvasResolution.MAX,
                            select = { it.default },
                            update = { copy(default = it) },
                        )
                        integer(
                            title = Strings.PreferencesChartsCanvasResolutionStep,
                            defaultValue = AppConf.CanvasResolution.DEFAULT_STEP,
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
                        defaultValue = AppConf.CanvasResolution.DEFAULT_STEP,
                        min = AppConf.Painter.MIN_MAX_DATA_CHUNK_SIZE,
                        max = AppConf.Painter.MAX_MAX_DATA_CHUNK_SIZE,
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
                        defaultValue = AppConf.Amplitude.DEFAULT_RESAMPLE_DOWN_TO_HZ,
                        min = AppConf.Amplitude.MIN_RESAMPLE_DOWN_TO_HZ,
                        select = { it.resampleDownToHz },
                        update = { copy(resampleDownToHz = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsWaveformNormalize,
                        description = Strings.PreferencesChartsWaveformNormalizeDescription,
                        defaultValue = AppConf.Amplitude.DEFAULT_NORMALIZE,
                        select = { it.normalize },
                        update = { copy(normalize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsWaveformUnitSize,
                        description = Strings.PreferencesChartsWaveformUnitSizeDescription,
                        defaultValue = AppConf.Amplitude.DEFAULT_UNIT_SIZE,
                        min = AppConf.Amplitude.MIN_UNIT_SIZE,
                        max = AppConf.Amplitude.MAX_UNIT_SIZE,
                        select = { it.unitSize },
                        update = { copy(unitSize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsWaveformIntensityAccuracy,
                        defaultValue = AppConf.Amplitude.DEFAULT_INTENSITY_ACCURACY,
                        min = AppConf.Amplitude.MIN_INTENSITY_ACCURACY,
                        max = AppConf.Amplitude.MAX_INTENSITY_ACCURACY,
                        select = { it.intensityAccuracy },
                        update = { copy(intensityAccuracy = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsWaveformYAxisBlankRate,
                        defaultValue = AppConf.Amplitude.DEFAULT_YAXIS_BLANK_RATE,
                        min = AppConf.Amplitude.MIN_YAXIS_BLANK_RATE,
                        max = AppConf.Amplitude.MAX_YAXIS_BLANK_RATE,
                        select = { it.yAxisBlankRate },
                        update = { copy(yAxisBlankRate = it) },
                    )
                    color(
                        title = Strings.PreferencesChartsWaveformColor,
                        defaultValue = AppConf.Amplitude.DEFAULT_COLOR,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = true,
                    )
                    color(
                        title = Strings.PreferencesChartsWaveformBackgroundColor,
                        defaultValue = AppConf.Amplitude.DEFAULT_BACKGROUND_COLOR,
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
                        defaultValue = AppConf.Spectrogram.DEFAULT_ENABLED,
                        select = { it.enabled },
                        update = { copy(enabled = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsSpectrogramHeight,
                        defaultValue = AppConf.Spectrogram.DEFAULT_HEIGHT_WEIGHT,
                        min = AppConf.Spectrogram.MIN_HEIGHT_WEIGHT,
                        max = AppConf.Spectrogram.MAX_HEIGHT_WEIGHT,
                        select = { it.heightWeight },
                        update = { copy(heightWeight = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramPointDensity,
                        description = Strings.PreferencesChartsSpectrogramPointDensityDescription,
                        defaultValue = AppConf.Spectrogram.DEFAULT_POINT_DENSITY,
                        min = AppConf.Spectrogram.MIN_POINT_DENSITY,
                        max = AppConf.Spectrogram.MAX_POINT_DENSITY,
                        select = { it.pointDensity },
                        update = { copy(pointDensity = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramHopSize,
                        defaultValue = AppConf.Spectrogram.DEFAULT_STANDARD_HOP_SIZE,
                        description = Strings.PreferencesChartsSpectrogramHopSizeDescription,
                        min = AppConf.Spectrogram.MIN_STANDARD_HOP_SIZE,
                        max = AppConf.Spectrogram.MAX_STANDARD_HOP_SIZE,
                        select = { it.standardHopSize },
                        update = { copy(standardHopSize = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramWindowSize,
                        description = Strings.PreferencesChartsSpectrogramWindowSizeDescription,
                        defaultValue = AppConf.Spectrogram.DEFAULT_STANDARD_WINDOW_SIZE,
                        min = AppConf.Spectrogram.MIN_STANDARD_WINDOW_SIZE,
                        max = AppConf.Spectrogram.MAX_STANDARD_WINDOW_SIZE,
                        select = { it.standardWindowSize },
                        update = { copy(standardWindowSize = it) },
                    )
                    selection(
                        title = Strings.PreferencesChartsSpectrogramWindowType,
                        defaultValue = AppConf.Spectrogram.DEFAULT_WINDOW_TYPE,
                        select = { it.windowType },
                        update = { copy(windowType = it) },
                        options = AppConf.WindowType.entries.toTypedArray(),
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMelScaleStep,
                        defaultValue = AppConf.Spectrogram.DEFAULT_MEL_SCALE_STEP,
                        min = AppConf.Spectrogram.MIN_MEL_SCALE_STEP,
                        max = AppConf.Spectrogram.MAX_MEL_SCALE_STEP,
                        select = { it.melScaleStep },
                        update = { copy(melScaleStep = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMaxFrequency,
                        defaultValue = AppConf.Spectrogram.DEFAULT_MAX_FREQUENCY,
                        min = AppConf.Spectrogram.MIN_MAX_FREQUENCY,
                        max = AppConf.Spectrogram.MAX_MAX_FREQUENCY,
                        select = { it.maxFrequency },
                        update = { copy(maxFrequency = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsSpectrogramMinIntensity,
                        defaultValue = AppConf.Spectrogram.DEFAULT_MIN_INTENSITY,
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
                        defaultValue = AppConf.Spectrogram.DEFAULT_MAX_INTENSITY,
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
                        defaultValue = AppConf.Spectrogram.DEFAULT_COLOR_PALETTE,
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
                        defaultValue = AppConf.Spectrogram.DEFAULT_USE_HIGH_ALPHA_CONTRAST,
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
                        defaultValue = AppConf.Power.DEFAULT_ENABLED,
                        select = { it.enabled },
                        update = { copy(enabled = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsPowerMergeChannels,
                        defaultValue = AppConf.Power.DEFAULT_MERGE_CHANNELS,
                        select = { it.mergeChannels },
                        update = { copy(mergeChannels = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsPowerHeight,
                        defaultValue = AppConf.Power.DEFAULT_HEIGHT_WEIGHT,
                        min = AppConf.Power.MIN_HEIGHT_WEIGHT,
                        max = AppConf.Power.MAX_HEIGHT_WEIGHT,
                        select = { it.heightWeight },
                        update = { copy(heightWeight = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsPowerUnitSize,
                        description = Strings.PreferencesChartsPowerUnitSizeDescription,
                        defaultValue = AppConf.Power.DEFAULT_UNIT_SIZE,
                        min = AppConf.Power.MIN_UNIT_SIZE,
                        max = AppConf.Power.MAX_UNIT_SIZE,
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
                        defaultValue = AppConf.Power.DEFAULT_WINDOW_SIZE,
                        min = AppConf.Power.MIN_WINDOW_SIZE,
                        max = AppConf.Power.MAX_WINDOW_SIZE,
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
                        defaultValue = AppConf.Power.DEFAULT_MIN_POWER,
                        min = AppConf.Power.MIN_MIN_POWER,
                        max = AppConf.Power.MAX_MAX_POWER,
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
                        defaultValue = AppConf.Power.DEFAULT_MAX_POWER,
                        min = AppConf.Power.MIN_MIN_POWER,
                        max = AppConf.Power.MAX_MAX_POWER,
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
                        defaultValue = AppConf.Power.DEFAULT_INTENSITY_ACCURACY,
                        min = AppConf.Power.MIN_INTENSITY_ACCURACY,
                        max = AppConf.Power.MAX_INTENSITY_ACCURACY,
                        select = { it.intensityAccuracy },
                        update = { copy(intensityAccuracy = it) },
                    )
                    color(
                        title = Strings.PreferencesChartsPowerColor,
                        defaultValue = AppConf.Power.DEFAULT_COLOR,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = true,
                    )
                    color(
                        title = Strings.PreferencesChartsPowerBackgroundColor,
                        defaultValue = AppConf.Power.DEFAULT_BACKGROUND_COLOR,
                        select = { it.backgroundColor },
                        update = { copy(backgroundColor = it) },
                        useAlpha = true,
                    )
                }
            }
    }

    object ChartsFundamental : PreferencesPage(
        Strings.PreferencesChartsFundamental,
        Strings.PreferencesChartsFundamentalDescription,
    ) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.painter.fundamental },
                    updater = { copy(painter = painter.copy(fundamental = it)) },
                ) {
                    switch(
                        title = Strings.PreferencesChartsFundamentalEnabled,
                        defaultValue = AppConf.Fundamental.DEFAULT_ENABLED,
                        select = { it.enabled },
                        update = { copy(enabled = it) },
                    )
                    floatPercentage(
                        title = Strings.PreferencesChartsFundamentalHeight,
                        defaultValue = AppConf.Fundamental.DEFAULT_HEIGHT_WEIGHT,
                        min = AppConf.Fundamental.MIN_HEIGHT_WEIGHT,
                        max = AppConf.Fundamental.MAX_HEIGHT_WEIGHT,
                        select = { it.heightWeight },
                        update = { copy(heightWeight = it) },
                    )
                    integer(
                        title = Strings.PreferencesChartsFundamentalSemitoneResolution,
                        defaultValue = AppConf.Fundamental.DEFAULT_SEMITONE_RESOLUTION,
                        min = AppConf.Fundamental.MIN_SEMITONE_RESOLUTION,
                        max = AppConf.Fundamental.MAX_SEMITONE_RESOLUTION,
                        select = { it.semitoneResolution },
                        update = { copy(semitoneResolution = it) },
                    )
                    float(
                        title = Strings.PreferencesChartsFundamentalMinFundamental,
                        defaultValue = AppConf.Fundamental.DEFAULT_MIN_FUNDAMENTAL,
                        min = AppConf.Fundamental.MIN_FUNDAMENTAL,
                        max = AppConf.Fundamental.MAX_FUNDAMENTAL,
                        select = { it.minFundamental },
                        update = { copy(minFundamental = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.fundamental.let { it.minFundamental < it.maxFundamental }
                                },
                                prompt = Strings.PreferencesChartsFundamentalMinFundamentalInvalid,
                            ),
                        ),
                    )
                    float(
                        title = Strings.PreferencesChartsFundamentalMaxFundamental,
                        defaultValue = AppConf.Fundamental.DEFAULT_MAX_FUNDAMENTAL,
                        min = AppConf.Fundamental.MIN_FUNDAMENTAL,
                        max = AppConf.Fundamental.MAX_FUNDAMENTAL,
                        select = { it.maxFundamental },
                        update = { copy(maxFundamental = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.fundamental.let { it.minFundamental < it.maxFundamental }
                                },
                                prompt = Strings.PreferencesChartsFundamentalMaxFundamentalInvalid,
                            ),
                        ),
                    )
                    integer(
                        title = Strings.PreferencesChartsFundamentalSemitoneSampleNum,
                        defaultValue = AppConf.Fundamental.DEFAULT_SEMITONE_SAMPLE_NUM,
                        min = 1,
                        max = AppConf.Fundamental.MAX_SEMITONE_SAMPLE_NUM,
                        select = { it.semitoneSampleNum },
                        update = { copy(semitoneSampleNum = it) },
                    )
                    float(
                        title = Strings.PreferencesChartsFundamentalMaxHarmonicFrequency,
                        defaultValue = AppConf.Fundamental.DEFAULT_MAX_HARMONIC_FREQUENCY,
                        max = AppConf.Fundamental.MAX_MAX_HARMONIC_FREQUENCY,
                        select = { it.maxHarmonicFrequency },
                        update = { copy(maxHarmonicFrequency = it) },
                        validationRules = listOf(
                            PreferencesItemValidationRule(
                                validate = { appConf ->
                                    appConf.painter.fundamental.let { it.maxHarmonicFrequency >= it.maxFundamental }
                                },
                                prompt = Strings.PreferencesChartsFundamentalMaxHarmonicFrequencyInvalid,
                            ),
                        ),
                    )
                    color(
                        title = Strings.PreferencesChartsFundamentalColor,
                        defaultValue = AppConf.Fundamental.DEFAULT_COLOR,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = true,
                    )
                    switch(
                        title = Strings.PreferencesChartsFundamentalDrawReferenceLine,
                        defaultValue = AppConf.Fundamental.DEFAULT_DRAW_REFERENCE_LINE,
                        select = { it.drawReferenceLine },
                        update = { copy(drawReferenceLine = it) },
                    )
                    color(
                        title = Strings.PreferencesChartsFundamentalReferenceLineColor,
                        defaultValue = AppConf.Fundamental.DEFAULT_REFERENCE_LINE_COLOR,
                        select = { it.referenceLineColor },
                        update = { copy(referenceLineColor = it) },
                        useAlpha = true,
                        enabled = { it.drawReferenceLine },
                    )
                    color(
                        title = Strings.PreferencesChartsFundamentalBackgroundColor,
                        defaultValue = AppConf.Fundamental.DEFAULT_BACKGROUND_COLOR,
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
                        defaultValue = AppConf.Conversion.DEFAULT_FFMPEG_PATH,
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
                        defaultValue = AppConf.Conversion.DEFAULT_FFMPEG_ARGS,
                        select = { it.ffmpegArgs },
                        update = { copy(ffmpegArgs = it) },
                    )
                    switch(
                        title = Strings.PreferencesChartsConversionFFmpegUseForWav,
                        defaultValue = AppConf.Conversion.DEFAULT_USE_CONVERSION_FOR_WAV,
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
                        defaultValue = AppConf.View.DEFAULT_LANGUAGE,
                        select = { it.language },
                        update = { copy(language = it) },
                        options = Language.entries.toTypedArray(),
                    )
                    selection(
                        title = Strings.PreferencesViewFontFamily,
                        defaultValue = AppConf.View.DEFAULT_FONT_FAMILY_NAME,
                        description = Strings.PreferencesViewFontFamilyDescription,
                        clickableTags = listOf(
                            ClickableTag(
                                tag = "edit",
                                onClick = { Desktop.getDesktop().open(FontRepository.fontDirectory) },
                            ),
                        ),
                        select = { it.fontFamilyName },
                        update = { copy(fontFamilyName = it) },
                        options = FontRepository.listAllNames().toTypedArray(),
                    )
                    switch(
                        title = Strings.PreferencesViewHideSampleExtension,
                        defaultValue = AppConf.View.DEFAULT_HIDE_SAMPLE_EXTENSION,
                        select = { it.hideSampleExtension },
                        update = { copy(hideSampleExtension = it) },
                    )
                    color(
                        title = Strings.PreferencesViewAppAccentColor,
                        defaultValue = AppConf.View.DEFAULT_ACCENT_COLOR,
                        select = { it.accentColor },
                        update = { copy(accentColor = it) },
                        useAlpha = false,
                    )
                    color(
                        title = Strings.PreferencesViewAppAccentColorVariant,
                        defaultValue = AppConf.View.DEFAULT_ACCENT_COLOR_VARIANT,
                        select = { it.accentColorVariant },
                        update = { copy(accentColorVariant = it) },
                        useAlpha = false,
                    )
                    selection(
                        title = Strings.PreferencesViewPinnedEntryListPosition,
                        defaultValue = AppConf.View.DEFAULT_PINNED_ENTRY_LIST_POSITION,
                        select = { it.pinnedEntryListPosition },
                        update = { copy(pinnedEntryListPosition = it) },
                        options = AppConf.ViewSidePosition.entries.toTypedArray(),
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
                        defaultValue = AppConf.Editor.DEFAULT_LOCKED_DRAG,
                        select = { it.lockedDrag },
                        update = { copy(lockedDrag = it) },
                        options = AppConf.Editor.LockedDrag.entries.toTypedArray(),
                    )
                    switch(
                        title = Strings.PreferencesEditorLockedSettingParameterWithCursor,
                        description = Strings.PreferencesEditorLockedSettingParameterWithCursorDescription,
                        defaultValue = AppConf.Editor.DEFAULT_LOCKED_SETTING_PARAMETER_WITH_CURSOR,
                        select = { it.lockedSettingParameterWithCursor },
                        update = { copy(lockedSettingParameterWithCursor = it) },
                    )
                    color(
                        title = Strings.PreferencesEditorPlayerCursorColor,
                        defaultValue = AppConf.Editor.DEFAULT_PLAYER_CURSOR_COLOR,
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
                        defaultValue = AppConf.Editor.DEFAULT_USE_ON_SCREEN_SCISSORS,
                        select = { it.useOnScreenScissors },
                        update = { copy(useOnScreenScissors = it) },
                    )
                    integer(
                        title = Strings.PreferencesEditorScissorsScissorsSubmitThreshold,
                        description = Strings.PreferencesEditorScissorsScissorsSubmitThresholdDescription,
                        defaultValue = AppConf.Editor.DEFAULT_SCISSORS_SUBMIT_THRESHOLD,
                        min = AppConf.Editor.MIN_SCISSORS_SUBMIT_THRESHOLD,
                        max = AppConf.Editor.MAX_SCISSORS_SUBMIT_THRESHOLD,
                        enabled = { it.useOnScreenScissors },
                        select = { it.scissorsSubmitThreshold },
                        update = { copy(scissorsSubmitThreshold = it) },
                    )
                    color(
                        title = Strings.PreferencesEditorScissorsColor,
                        defaultValue = AppConf.Editor.DEFAULT_SCISSORS_COLOR,
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
                        defaultValue = AppConf.ScissorsActions.DEFAULT_GO_TO,
                        select = { it.goTo },
                        update = { copy(goTo = it) },
                        options = AppConf.ScissorsActions.Target.entries.toTypedArray(),
                    )
                    selection(
                        title = Strings.PreferencesEditorScissorsActionAskForName,
                        defaultValue = AppConf.ScissorsActions.DEFAULT_ASK_FOR_NAME,
                        select = { it.askForName },
                        update = { copy(askForName = it) },
                        options = AppConf.ScissorsActions.Target.entries.toTypedArray(),
                    )
                    selection(
                        title = Strings.PreferencesEditorScissorsActionPlay,
                        defaultValue = AppConf.ScissorsActions.DEFAULT_PLAY,
                        select = { it.play },
                        update = { copy(play = it) },
                        options = AppConf.ScissorsActions.Target.entries.toTypedArray(),
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
                        defaultValue = AppConf.Editor.DEFAULT_SHOW_DONE,
                        select = { it.showDone },
                        update = { copy(showDone = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowStarred,
                        defaultValue = AppConf.Editor.DEFAULT_SHOW_STAR,
                        select = { it.showStar },
                        update = { copy(showStar = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowTag,
                        defaultValue = AppConf.Editor.DEFAULT_SHOW_TAG,
                        select = { it.showTag },
                        update = { copy(showTag = it) },
                    )
                    switch(
                        title = Strings.PreferencesEditorShowExtra,
                        description = Strings.PreferencesEditorShowExtraDescription,
                        defaultValue = AppConf.Editor.DEFAULT_SHOW_EXTRA,
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
                            defaultValue = AppConf.PostEditAction.DEFAULT_DONE.enabled,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        selection(
                            title = Strings.PreferencesEditorPostEditActionTrigger,
                            defaultValue = AppConf.PostEditAction.DEFAULT_DONE.field,
                            options = AppConf.PostEditAction.TriggerField.entries.toTypedArray(),
                            select = { it.field },
                            update = { copy(field = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseDragging,
                            description = Strings.PreferencesEditorPostEditActionUseDraggingDescription,
                            defaultValue = AppConf.PostEditAction.DEFAULT_DONE.useDragging,
                            select = { it.useDragging },
                            update = { copy(useDragging = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseCursorSet,
                            description = Strings.PreferencesEditorPostEditActionUseCursorSetDescription,
                            defaultValue = AppConf.PostEditAction.DEFAULT_DONE.useCursorSet,
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
                            defaultValue = AppConf.PostEditAction.DEFAULT_NEXT.enabled,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        selection(
                            title = Strings.PreferencesEditorPostEditActionTrigger,
                            defaultValue = AppConf.PostEditAction.DEFAULT_NEXT.field,
                            options = AppConf.PostEditAction.TriggerField.entries.toTypedArray(),
                            select = { it.field },
                            update = { copy(field = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseDragging,
                            description = Strings.PreferencesEditorPostEditActionUseDraggingDescription,
                            defaultValue = AppConf.PostEditAction.DEFAULT_NEXT.useDragging,
                            select = { it.useDragging },
                            update = { copy(useDragging = it) },
                            enabled = { it.enabled },
                        )
                        switch(
                            title = Strings.PreferencesEditorPostEditActionUseCursorSet,
                            description = Strings.PreferencesEditorPostEditActionUseCursorSetDescription,
                            defaultValue = AppConf.PostEditAction.DEFAULT_NEXT.useCursorSet,
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
                        defaultValue = AppConf.AutoScroll.DEFAULT_ON_LOADED_NEW_SAMPLE,
                        select = { it.onLoadedNewSample },
                        update = { copy(onLoadedNewSample = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnJumpedToEntry,
                        defaultValue = AppConf.AutoScroll.DEFAULT_ON_JUMPED_TO_ENTRY,
                        select = { it.onJumpedToEntry },
                        update = { copy(onJumpedToEntry = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnSwitchedInMultipleEditMode,
                        defaultValue = AppConf.AutoScroll.DEFAULT_ON_SWITCHED_IN_MULTIPLE_EDIT_MODE,
                        select = { it.onSwitchedInMultipleEditMode },
                        update = { copy(onSwitchedInMultipleEditMode = it) },
                        enabled = { it.onSwitched.not() },
                    )
                    switch(
                        title = Strings.PreferencesEditorAutoScrollOnSwitched,
                        defaultValue = AppConf.AutoScroll.DEFAULT_ON_SWITCHED,
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
                        defaultValue = AppConf.ContinuousLabelNames.DEFAULT_COLOR,
                        select = { it.color },
                        update = { copy(color = it) },
                        useAlpha = false,
                    )
                    color(
                        title = Strings.PreferencesEditorContinuousLabelNamesBackgroundColor,
                        defaultValue = AppConf.ContinuousLabelNames.DEFAULT_BACKGROUND_COLOR,
                        select = { it.backgroundColor },
                        update = { copy(backgroundColor = it) },
                        useAlpha = true,
                    )
                    color(
                        title = Strings.PreferencesEditorContinuousLabelNamesEditableBackgroundColor,
                        defaultValue = AppConf.ContinuousLabelNames.DEFAULT_EDITABLE_BACKGROUND_COLOR,
                        select = { it.editableBackgroundColor },
                        update = { copy(editableBackgroundColor = it) },
                        useAlpha = true,
                    )
                    selection(
                        title = Strings.PreferencesEditorContinuousLabelNamesSize,
                        defaultValue = AppConf.ContinuousLabelNames.DEFAULT_SIZE,
                        select = { it.size },
                        update = { copy(size = it) },
                        options = AppConf.FontSize.entries.toTypedArray(),
                    )
                    selection(
                        title = Strings.PreferencesEditorContinuousLabelNamesPosition,
                        defaultValue = AppConf.ContinuousLabelNames.DEFAULT_POSITION,
                        select = { it.position },
                        update = { copy(position = it) },
                        options = AppConf.ViewPosition.entries.toTypedArray(),
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
                            defaultValue = AppConf.PlayOnDragging.DEFAULT_PLAY_ON_DRAGGING_ENABLED,
                            select = { it.enabled },
                            update = { copy(enabled = it) },
                        )
                        integer(
                            title = Strings.PreferencesPlaybackPlayOnDraggingRangeRadiusMillis,
                            defaultValue = AppConf.PlayOnDragging.DEFAULT_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS,
                            min = AppConf.PlayOnDragging.MIN_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS,
                            max = AppConf.PlayOnDragging.MAX_PLAY_ON_DRAGGING_RANGE_RADIUS_MILLIS,
                            select = { it.rangeRadiusMillis },
                            update = { copy(rangeRadiusMillis = it) },
                        )
                        integer(
                            title = Strings.PreferencesPlaybackPlayOnDraggingEventQueueSize,
                            defaultValue = AppConf.PlayOnDragging.DEFAULT_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE,
                            min = AppConf.PlayOnDragging.MIN_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE,
                            max = AppConf.PlayOnDragging.MAX_PLAY_ON_DRAGGING_EVENT_QUEUE_SIZE,
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
                        defaultValue = AppConf.AutoSave.DEFAULT_TARGET,
                        select = { it.target },
                        update = { copy(target = it) },
                        options = AppConf.AutoSave.Target.entries.toTypedArray(),
                    )
                    integer(
                        title = Strings.PreferencesAutoSaveIntervalSec,
                        defaultValue = AppConf.AutoSave.DEFAULT_INTERVAL_SEC,
                        min = AppConf.AutoSave.MIN_INTERVAL_SEC,
                        select = { it.intervalSec },
                        update = { copy(intervalSec = it) },
                    )
                }
            }
    }

    object AutoReload : PreferencesPage(Strings.PreferencesAutoReload, Strings.PreferencesAutoReloadDescription) {
        override val content: List<PreferencesGroup>
            get() = buildPageContent {
                withContext(
                    selector = { it.autoReload },
                    updater = { copy(autoReload = it) },
                ) {
                    selection(
                        title = Strings.PreferencesAutoReloadBehavior,
                        defaultValue = AppConf.AutoReload.DEFAULT_BEHAVIOR,
                        select = { it.behavior },
                        update = { copy(behavior = it) },
                        options = AppConf.AutoReload.Behavior.entries.toTypedArray(),
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
                        defaultValue = AppConf.History.DEFAULT_MAX_SIZE,
                        min = AppConf.History.MIN_MAX_SIZE,
                        select = { it.maxSize },
                        update = { copy(maxSize = it) },
                    )
                    switch(
                        title = Strings.PreferencesHistorySquashIndex,
                        description = Strings.PreferencesHistorySquashIndexDescription,
                        defaultValue = AppConf.History.DEFAULT_SQUASH_INDEX,
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
                    selection(
                        title = Strings.PreferencesMiscUpdateChannel,
                        description = Strings.PreferencesMiscUpdateChannelDescription,
                        defaultValue = AppConf.Misc.DEFAULT_UPDATE_CHANNEL,
                        options = UpdateChannel.entries.toTypedArray(),
                        select = { it.updateChannel },
                        update = { copy(updateChannel = it) },
                    )
                    switch(
                        title = Strings.PreferencesMiscUseCustomFileDialog,
                        description = Strings.PreferencesMiscUseCustomFileDialogDescription,
                        defaultValue = AppConf.Misc.DEFAULT_USE_CUSTOM_FILE_DIALOG,
                        select = { it.useCustomFileDialog },
                        update = { copy(useCustomFileDialog = it) },
                    )
                    group(name = Strings.PreferencesMiscDangerZone) {
                        button(
                            title = Strings.PreferencesMiscClearRecord,
                            description = Strings.PreferencesMiscClearRecordDescription,
                            buttonText = Strings.PreferencesMiscClearRecordButton,
                            onClick = { requestClearAppRecordAndExit() },
                            isDangerous = true,
                        )
                        button(
                            title = Strings.PreferencesMiscClearAppData,
                            description = Strings.PreferencesMiscClearAppDataDescription,
                            buttonText = Strings.PreferencesMiscClearAppDataButton,
                            onClick = { requestClearAppDataAndExit() },
                            isDangerous = true,
                        )
                    }
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
            AutoReload,
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

    fun button(
        title: Strings,
        description: Strings? = null,
        clickableTags: List<ClickableTag> = listOf(),
        buttonText: Strings,
        onClick: AppState.() -> Unit,
        isDangerous: Boolean = false,
        enabled: (P) -> Boolean = { true },
    ) = builder.item(
        PreferencesItem.Button(
            title = title,
            description = description,
            clickableTags = clickableTags,
            buttonText = buttonText,
            onClick = onClick,
            isDangerous = isDangerous,
            enabled = selectWithContext(enabled),
        ),
    )
}

private class PageContentBuilder {
    private val content = mutableListOf<PreferencesGroup>()
    private var currentGroup = mutableListOf<PreferencesItem>()

    fun <P> withContext(
        selector: (AppConf) -> P,
        updater: AppConf.(P) -> AppConf,
        block: PreferencesItemContext<P>.() -> Unit,
    ) {
        PreferencesItemContext(this, selector, updater).block()
    }

    fun item(item: PreferencesItem) {
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
