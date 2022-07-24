package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.editor.SpectrogramColorPalette
import com.sdercolin.vlabeler.ui.string.Strings

abstract class PreferencesPage(
    val displayedName: Strings,
    val description: Strings
) {
    open val children: List<PreferencesPage> = listOf()
    open val content: List<PreferencesGroup> = listOf()

    val name: String get() = displayedName.name

    object Charts : PreferencesPage(Strings.PreferencesCharts, Strings.PreferencesChartsDescription) {

        override val children get() = listOf(ChartsCanvas, ChartsWaveform, ChartsSpectrogram)
    }

    object ChartsCanvas : PreferencesPage(Strings.PreferencesChartsCanvas, Strings.PreferencesChartsCanvasDescription) {

        override val content: List<PreferencesGroup> = buildPageContent {
            group(
                Strings.PreferencesChartsCanvasResolution,
                Strings.PreferencesChartsCanvasResolutionDescription
            ) {
                withContext(
                    selector = { it.painter.canvasResolution },
                    updater = { copy(painter = painter.copy(canvasResolution = it)) }
                ) {
                    integer(
                        title = Strings.PreferencesChartsCanvasResolutionDefault,
                        defaultValue = AppConf.CanvasResolution.DefaultDefault,
                        min = AppConf.CanvasResolution.Min,
                        max = AppConf.CanvasResolution.Max,
                        select = { it.default },
                        update = { copy(default = it) }
                    )
                    integer(
                        title = Strings.PreferencesChartsCanvasResolutionStep,
                        defaultValue = AppConf.CanvasResolution.DefaultStep,
                        min = 1,
                        select = { it.step },
                        update = { copy(step = it) }
                    )
                }
            }
            withContext(
                selector = { it.painter },
                updater = { copy(painter = it) }
            ) {
                integer(
                    title = Strings.PreferencesChartsMaxDataChunkSize,
                    description = Strings.PreferencesChartsMaxDataChunkSizeDescription,
                    defaultValue = AppConf.CanvasResolution.DefaultStep,
                    min = AppConf.Painter.MinMaxDataChunkSize,
                    max = AppConf.Painter.MaxMaxDataChunkSize,
                    select = { it.maxDataChunkSize },
                    update = { copy(maxDataChunkSize = it) }
                )
            }
        }
    }

    object ChartsWaveform : PreferencesPage(
        Strings.PreferencesChartsWaveform,
        Strings.PreferencesChartsWaveformDescription
    ) {

        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.painter.amplitude },
                updater = { copy(painter = painter.copy(amplitude = it)) }
            ) {
                integer(
                    title = Strings.PreferencesChartsWaveformUnitSize,
                    defaultValue = AppConf.Amplitude.DefaultUnitSize,
                    min = AppConf.Amplitude.MinUnitSize,
                    max = AppConf.Amplitude.MaxUnitSize,
                    select = { it.unitSize },
                    update = { copy(unitSize = it) }
                )
                integer(
                    title = Strings.PreferencesChartsWaveformIntensityAccuracy,
                    defaultValue = AppConf.Amplitude.DefaultIntensityAccuracy,
                    min = AppConf.Amplitude.MinIntensityAccuracy,
                    max = AppConf.Amplitude.MaxIntensityAccuracy,
                    select = { it.intensityAccuracy },
                    update = { copy(intensityAccuracy = it) }
                )
                float(
                    title = Strings.PreferencesChartsWaveformYAxisBlankRate,
                    defaultValue = AppConf.Amplitude.DefaultYAxisBlankRate * 100,
                    min = AppConf.Amplitude.MinYAxisBlankRate * 100,
                    max = AppConf.Amplitude.MaxYAxisBlankRate * 100,
                    select = { it.yAxisBlankRate * 100 },
                    update = { copy(yAxisBlankRate = it / 100) }
                )
                color(
                    title = Strings.PreferencesChartsWaveformColor,
                    defaultValue = AppConf.Amplitude.DefaultColor,
                    select = { it.color },
                    update = { copy(color = it) },
                    useAlpha = true
                )
                color(
                    title = Strings.PreferencesChartsWaveformBackgroundColor,
                    defaultValue = AppConf.Amplitude.DefaultBackgroundColor,
                    select = { it.backgroundColor },
                    update = { copy(backgroundColor = it) },
                    useAlpha = true
                )
            }
        }
    }

    object ChartsSpectrogram : PreferencesPage(
        Strings.PreferencesChartsSpectrogram,
        Strings.PreferencesChartsSpectrogramDescription
    ) {
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.painter.spectrogram },
                updater = { copy(painter = painter.copy(spectrogram = it)) }
            ) {
                switch(
                    title = Strings.PreferencesChartsSpectrogramEnabled,
                    defaultValue = AppConf.Spectrogram.DefaultEnabled,
                    select = { it.enabled },
                    update = { copy(enabled = it) }
                )
                float(
                    title = Strings.PreferencesChartsSpectrogramHeight,
                    defaultValue = AppConf.Spectrogram.DefaultHeightWeight * 100,
                    min = AppConf.Spectrogram.MinHeightWeight * 100,
                    max = AppConf.Spectrogram.MaxHeightWeight * 100,
                    select = { it.heightWeight * 100 },
                    update = { copy(heightWeight = it / 100) }
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramPointPixelSize,
                    defaultValue = AppConf.Spectrogram.DefaultPointPixelSize,
                    min = AppConf.Spectrogram.MinPointPixelSize,
                    max = AppConf.Spectrogram.MaxPointPixelSize,
                    select = { it.pointPixelSize },
                    update = { copy(pointPixelSize = it) }
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramFrameSize,
                    defaultValue = AppConf.Spectrogram.DefaultFrameSize,
                    min = AppConf.Spectrogram.MinFrameSize,
                    max = AppConf.Spectrogram.MaxFrameSize,
                    select = { it.frameSize },
                    update = { copy(frameSize = it) }
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramMaxFrequency,
                    defaultValue = AppConf.Spectrogram.DefaultMaxFrequency,
                    min = AppConf.Spectrogram.MinMaxFrequency,
                    max = AppConf.Spectrogram.MaxMaxFrequency,
                    select = { it.maxFrequency },
                    update = { copy(maxFrequency = it) }
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramMinIntensity,
                    defaultValue = AppConf.Spectrogram.DefaultMinIntensity,
                    select = { it.minIntensity },
                    update = { copy(minIntensity = it) }
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramMaxIntensity,
                    defaultValue = AppConf.Spectrogram.DefaultMaxIntensity,
                    select = { it.maxIntensity },
                    update = { copy(maxIntensity = it) }
                )
                selection(
                    title = Strings.PreferencesChartsSpectrogramWindowType,
                    defaultValue = AppConf.Spectrogram.DefaultWindowType,
                    select = { it.windowType },
                    update = { copy(windowType = it) },
                    options = AppConf.WindowType.values()
                )
                selection(
                    title = Strings.PreferencesChartsSpectrogramColorPalette,
                    defaultValue = AppConf.Spectrogram.DefaultColorPalette,
                    select = { it.colorPalette },
                    update = { copy(colorPalette = it) },
                    options = SpectrogramColorPalette.Presets.values()
                )
            }
        }
    }

    object Editor : PreferencesPage(Strings.PreferencesEditor, Strings.PreferencesEditorDescription) {

        override val children: List<PreferencesPage>
            get() = listOf(EditorScissors, EditorAutoScroll)

        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.editor },
                updater = { copy(editor = it) }
            ) {
                color(
                    title = Strings.PreferencesEditorPlayerCursorColor,
                    defaultValue = AppConf.Editor.DefaultPlayerCursorColor,
                    useAlpha = false,
                    select = { it.playerCursorColor },
                    update = { copy(playerCursorColor = it) }
                )
            }
        }
    }

    object EditorScissors : PreferencesPage(
        Strings.PreferencesEditorScissors,
        Strings.PreferencesEditorScissorsDescription
    ) {
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.editor },
                updater = { copy(editor = it) }
            ) {
                color(
                    title = Strings.PreferencesEditorScissorsColor,
                    defaultValue = AppConf.Editor.DefaultScissorsColor,
                    useAlpha = true,
                    select = { it.scissorsColor },
                    update = { copy(scissorsColor = it) }
                )
            }
            withContext(
                selector = { it.editor.scissorsActions },
                updater = { copy(editor = editor.copy(scissorsActions = it)) }
            ) {
                selection(
                    title = Strings.PreferencesEditorScissorsActionGoTo,
                    defaultValue = AppConf.ScissorsActions.DefaultGoTo,
                    select = { it.goTo },
                    update = { copy(goTo = it) },
                    options = AppConf.ScissorsActions.Target.values()
                )
                selection(
                    title = Strings.PreferencesEditorScissorsActionAskForName,
                    defaultValue = AppConf.ScissorsActions.DefaultAskForName,
                    select = { it.askForName },
                    update = { copy(askForName = it) },
                    options = AppConf.ScissorsActions.Target.values()
                )
                selection(
                    title = Strings.PreferencesEditorScissorsActionPlay,
                    defaultValue = AppConf.ScissorsActions.DefaultPlay,
                    select = { it.play },
                    update = { copy(play = it) },
                    options = AppConf.ScissorsActions.Target.values()
                )
            }
        }
    }

    object EditorAutoScroll : PreferencesPage(
        Strings.PreferencesEditorAutoScroll,
        Strings.PreferencesEditorAutoScrollDescription
    ) {
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.editor.autoScroll },
                updater = { copy(editor = editor.copy(autoScroll = it)) }
            ) {
                switch(
                    title = Strings.PreferencesEditorAutoScrollOnLoadedNewSample,
                    defaultValue = AppConf.AutoScroll.DefaultOnLoadedNewSample,
                    select = { it.onLoadedNewSample },
                    update = { copy(onLoadedNewSample = it) }
                )
                switch(
                    title = Strings.PreferencesEditorAutoScrollOnJumpedToEntry,
                    defaultValue = AppConf.AutoScroll.DefaultOnJumpedToEntry,
                    select = { it.onJumpedToEntry },
                    update = { copy(onJumpedToEntry = it) }
                )
                switch(
                    title = Strings.PreferencesEditorAutoScrollOnSwitchedInMultipleEditMode,
                    defaultValue = AppConf.AutoScroll.DefaultOnSwitchedInMultipleEditMode,
                    select = { it.onSwitchedInMultipleEditMode },
                    update = { copy(onSwitchedInMultipleEditMode = it) }
                )
            }
        }
    }

    object AutoSave : PreferencesPage(Strings.PreferencesAutoSave, Strings.PreferencesAutoSaveDescription) {
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.autoSave },
                updater = { copy(autoSave = it) }
            ) {
                selection(
                    title = Strings.PreferencesAutoSaveTarget,
                    defaultValue = AppConf.AutoSave.DefaultTarget,
                    select = { it.target },
                    update = { copy(target = it) },
                    options = AppConf.AutoSave.Target.values()
                )
                integer(
                    title = Strings.PreferencesAutoSaveIntervalSec,
                    defaultValue = AppConf.AutoSave.DefaultIntervalSec,
                    min = AppConf.AutoSave.MinIntervalSec,
                    select = { it.intervalSec },
                    update = { copy(intervalSec = it) }
                )
            }
        }
    }

    companion object {

        fun getRootPages() = listOf(
            Charts,
            Editor,
            AutoSave
        )

        private fun getChildrenRecursively(page: PreferencesPage): List<PreferencesPage> {
            return listOf(page) + page.children.flatMap { getChildrenRecursively(it) }
        }

        private fun getAllPages() = getRootPages().flatMap { getChildrenRecursively(it) }

        fun getPage(name: String) = getAllPages().find { it.name == name }
    }
}

private class PreferencesItemContext<P>(
    val builder: PageContentBuilder,
    val selector: (AppConf) -> P,
    val updater: AppConf.(P) -> AppConf
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
        defaultValue: Boolean,
        select: (P) -> Boolean,
        update: P.(Boolean) -> P
    ) = builder.item(
        PreferencesItem.Switch(
            title = title,
            description = description,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update)
        )
    )

    fun integer(
        title: Strings,
        description: Strings? = null,
        defaultValue: Int,
        min: Int? = null,
        max: Int? = null,
        select: (P) -> Int,
        update: P.(Int) -> P
    ) = builder.item(
        PreferencesItem.IntegerInput(
            title = title,
            description = description,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update)
        )
    )

    fun float(
        title: Strings,
        description: Strings? = null,
        defaultValue: Float,
        min: Float? = null,
        max: Float? = null,
        select: (P) -> Float,
        update: P.(Float) -> P
    ) = builder.item(
        PreferencesItem.FloatInput(
            title = title,
            description = description,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update)
        )
    )

    fun color(
        title: Strings,
        description: Strings? = null,
        defaultValue: String,
        select: (P) -> String,
        update: P.(String) -> P,
        useAlpha: Boolean
    ) = builder.item(
        PreferencesItem.ColorStringInput(
            title = title,
            description = description,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            useAlpha = useAlpha
        )
    )

    fun <T> selection(
        title: Strings,
        description: Strings? = null,
        defaultValue: T,
        select: (P) -> T,
        update: P.(T) -> P,
        options: Array<T>
    ) = builder.item(
        PreferencesItem.Selection(
            title = title,
            description = description,
            defaultValue = defaultValue,
            select = selectWithContext(select),
            update = updateWithContext(update),
            options = options
        )
    )
}

private class PageContentBuilder {
    private val content = mutableListOf<PreferencesGroup>()
    private var currentGroup = mutableListOf<PreferencesItem<*>>()

    fun <P> withContext(
        selector: (AppConf) -> P,
        updater: AppConf.(P) -> AppConf,
        block: PreferencesItemContext<P>.() -> Unit
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
