package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseScrollActionKeyBind
import com.sdercolin.vlabeler.ui.editor.SpectrogramColorPalette
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings

abstract class PreferencesPage(
    val displayedName: Strings,
    val description: Strings,
    val scrollable: Boolean = true,
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

        override val content: List<PreferencesGroup> = buildPageContent {
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
                float(
                    title = Strings.PreferencesChartsWaveformYAxisBlankRate,
                    defaultValue = AppConf.Amplitude.DefaultYAxisBlankRate * 100,
                    min = AppConf.Amplitude.MinYAxisBlankRate * 100,
                    max = AppConf.Amplitude.MaxYAxisBlankRate * 100,
                    select = { it.yAxisBlankRate * 100 },
                    update = { copy(yAxisBlankRate = it / 100) },
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
                float(
                    title = Strings.PreferencesChartsSpectrogramHeight,
                    defaultValue = AppConf.Spectrogram.DefaultHeightWeight * 100,
                    min = AppConf.Spectrogram.MinHeightWeight * 100,
                    max = AppConf.Spectrogram.MaxHeightWeight * 100,
                    select = { it.heightWeight * 100 },
                    update = { copy(heightWeight = it / 100) },
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
                )
                integer(
                    title = Strings.PreferencesChartsSpectrogramMaxIntensity,
                    defaultValue = AppConf.Spectrogram.DefaultMaxIntensity,
                    select = { it.maxIntensity },
                    update = { copy(maxIntensity = it) },
                )
                selection(
                    title = Strings.PreferencesChartsSpectrogramColorPalette,
                    defaultValue = AppConf.Spectrogram.DefaultColorPalette,
                    select = { it.colorPalette },
                    update = { copy(colorPalette = it) },
                    options = SpectrogramColorPalette.Presets.values(),
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.keymaps },
                updater = { copy(keymaps = it) },
            ) {
                keymap(
                    actionType = ActionType.MouseClick,
                    defaultValue = listOf(),
                    select = { parent -> parent.mouseClickActionMap.map { MouseClickActionKeyBind(it.key, it.value) } },
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
            get() = listOf(EditorScissors, EditorAutoScroll, EditorNotes)

        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.editor },
                updater = { copy(editor = it) },
            ) {
                selection(
                    title = Strings.PreferencesEditorPlayerLockedDrag,
                    description = Strings.PreferencesEditorPlayerLockedDragDescription,
                    columnStyle = true,
                    defaultValue = AppConf.Editor.DefaultLockedDrag,
                    select = { it.lockedDrag },
                    update = { copy(lockedDrag = it) },
                    options = AppConf.Editor.LockedDrag.values(),
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
        override val content: List<PreferencesGroup> = buildPageContent {
            withContext(
                selector = { it.editor },
                updater = { copy(editor = it) },
            ) {
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
                    title = Strings.PreferencesEditorAutoDone,
                    defaultValue = AppConf.Editor.DefaultAutoDone,
                    select = { it.autoDone },
                    update = { copy(autoDone = it) },
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
            }
        }
    }

    object EditorAutoScroll : PreferencesPage(
        Strings.PreferencesEditorAutoScroll,
        Strings.PreferencesEditorAutoScrollDescription,
    ) {
        override val content: List<PreferencesGroup> = buildPageContent {
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

    object Playback : PreferencesPage(Strings.PreferencesPlayback, Strings.PreferencesPlaybackDescription) {

        override val content: List<PreferencesGroup> = buildPageContent {
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
        override val content: List<PreferencesGroup> = buildPageContent {
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
        override val content: List<PreferencesGroup> = buildPageContent {
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

    companion object {

        fun getRootPages() = listOf(
            Charts,
            Keymap,
            View,
            Editor,
            Playback,
            AutoSave,
            History,
        )
    }
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
        columnStyle: Boolean = false,
        defaultValue: Boolean,
        select: (P) -> Boolean,
        update: P.(Boolean) -> P,
        enabled: (P) -> Boolean = { true },
    ) = builder.item(
        PreferencesItem.Switch(
            title = title,
            description = description,
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
        columnStyle: Boolean = false,
        defaultValue: Int,
        min: Int? = null,
        max: Int? = null,
        select: (P) -> Int,
        update: P.(Int) -> P,
        enabled: (P) -> Boolean = { true },
    ) = builder.item(
        PreferencesItem.IntegerInput(
            title = title,
            description = description,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
        ),
    )

    fun float(
        title: Strings,
        description: Strings? = null,
        columnStyle: Boolean = false,
        defaultValue: Float,
        min: Float? = null,
        max: Float? = null,
        select: (P) -> Float,
        update: P.(Float) -> P,
        enabled: (P) -> Boolean = { true },
    ) = builder.item(
        PreferencesItem.FloatInput(
            title = title,
            description = description,
            columnStyle = columnStyle,
            defaultValue = defaultValue,
            min = min,
            max = max,
            select = selectWithContext(select),
            update = updateWithContext(update),
            enabled = selectWithContext(enabled),
        ),
    )

    fun color(
        title: Strings,
        description: Strings? = null,
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
