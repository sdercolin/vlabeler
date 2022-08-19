package com.sdercolin.vlabeler.ui.string

import com.sdercolin.vlabeler.ui.string.Language.English

enum class Strings(val en: String) {
    AppName(
        en = "vLabeler"
    ),
    LanguageDisplayName(
        en = English.displayName
    ),
    MenuFile(
        en = "File"
    ),
    MenuFileNewProject(
        en = "New Project..."
    ),
    MenuFileOpen(
        en = "Open..."
    ),
    MenuFileOpenRecent(
        en = "Open Recent"
    ),
    MenuFileOpenRecentClear(
        en = "Clear Recently Opened"
    ),
    MenuFileSave(
        en = "Save"
    ),
    MenuFileSaveAs(
        en = "Save As..."
    ),
    MenuFileExport(
        en = "Export..."
    ),
    MenuFileInvalidateCaches(
        en = "Invalidate Caches"
    ),
    MenuFileClose(
        en = "Close"
    ),
    MenuEdit(
        en = "Edit"
    ),
    MenuEditUndo(
        en = "Undo"
    ),
    MenuEditRedo(
        en = "Redo"
    ),
    MenuEditTools(
        en = "Tools"
    ),
    MenuEditToolsCursor(
        en = "Cursor"
    ),
    MenuEditToolsScissors(
        en = "Scissors"
    ),
    MenuEditRenameEntry(
        en = "Rename Current Entry..."
    ),
    MenuEditDuplicateEntry(
        en = "Duplicate Current Entry..."
    ),
    MenuEditRemoveEntry(
        en = "Remove Current Entry"
    ),
    MenuEditMultipleEditMode(
        en = "Edit All Connected Entries"
    ),
    MenuView(
        en = "View"
    ),
    MenuViewToggleMarker(
        en = "Show Parameter Controllers"
    ),
    MenuViewPinEntryList(
        en = "Pin Entry List"
    ),
    MenuViewToggleProperties(
        en = "Show Properties"
    ),
    MenuViewToggleToolbox(
        en = "Show Toolbox"
    ),
    MenuViewOpenSampleList(
        en = "Sample List"
    ),
    MenuNavigate(
        en = "Navigate"
    ),
    MenuNavigateNextEntry(
        en = "Go to Next Entry"
    ),
    MenuNavigatePreviousEntry(
        en = "Go to Previous Entry"
    ),
    MenuNavigateNextSample(
        en = "Go to Next Sample"
    ),
    MenuNavigatePreviousSample(
        en = "Go to Previous Sample"
    ),
    MenuNavigateJumpToEntry(
        en = "Go to Entry..."
    ),
    MenuNavigateScrollFit(
        en = "Scroll to Show the Current Entry"
    ),
    MenuTools(
        en = "Tools"
    ),
    MenuToolsBatchEdit(
        en = "Batch Edit"
    ),
    MenuToolsBatchEditManagePlugins(
        en = "Manage Plugins..."
    ),
    MenuSettings(
        en = "Settings"
    ),
    MenuHelp(
        en = "Help"
    ),
    MenuHelpOpenLogDirectory(
        en = "Open Log Directory"
    ),
    MenuHelpOpenLatestRelease(
        en = "Open Latest Release"
    ),
    MenuHelpOpenGitHub(
        en = "Open GitHub Page"
    ),
    MenuHelpJoinDiscord(
        en = "Join Discord"
    ),
    MenuSettingsPreferences(
        en = "Preferences..."
    ),
    MenuSettingsLabelers(
        en = "Labelers..."
    ),
    MenuSettingsTemplatePlugins(
        en = "Template Generators..."
    ),
    CommonOkay(
        en = "OK"
    ),
    CommonApply(
        en = "Apply"
    ),
    CommonCancel(
        en = "Cancel"
    ),
    CommonYes(
        en = "Yes"
    ),
    CommonNo(
        en = "No"
    ),
    CommonWarning(
        en = "Warning"
    ),
    CommonError(
        en = "Error"
    ),
    CommonInputErrorPromptNumber(
        en = "Please enter a number."
    ),
    CommonInputErrorPromptInteger(
        en = "Please enter an integer number."
    ),
    CommonInputErrorPromptNumberRange(
        en = "Please enter a number between %s and %s."
    ),
    CommonInputErrorPromptNumberMin(
        en = "Please enter a number greater than or equal to %s."
    ),
    CommonInputErrorPromptNumberMax(
        en = "Please enter a number less than or equal to %s."
    ),
    StarterStart(
        en = "Start"
    ),
    StarterNewProject(
        en = "New Project..."
    ),
    StarterOpen(
        en = "Open..."
    ),
    StarterRecent(
        en = "Recent"
    ),
    StarterRecentEmpty(
        en = "You have no recent projects."
    ),
    StarterRecentDeleted(
        en = "This profile has been removed."
    ),
    StarterNewSampleDirectory(
        en = "Sample directory"
    ),
    StarterNewWorkingDirectory(
        en = "Project location"
    ),
    StarterNewProjectTitle(
        en = "New Project"
    ),
    StarterNewProjectName(
        en = "Project name"
    ),
    StarterNewProjectNameWarning(
        en = "The project file already exists. Creating project will overwrite the file."
    ),
    StarterNewCacheDirectory(
        en = "Cache directory"
    ),
    StarterNewLabeler(
        en = "Labeler"
    ),
    StarterNewTemplatePlugin(
        en = "Template generator"
    ),
    StarterNewTemplatePluginNone(
        en = "None"
    ),
    StarterNewInputFile(
        en = "Input file (.%s)"
    ),
    StarterNewInputFilePlaceholder(
        en = "(One would be created if left blank)"
    ),
    StarterNewInputFileDisabled(
        en = "No input file is required by the selected template generator"
    ),
    StarterNewEncoding(
        en = "Encoding"
    ),
    StarterNewAutoExport(
        en = "Auto-export"
    ),
    StarterNewAutoExportHelp(
        en = "Automatically export the project to overwrite the given input file (if not given," +
            " the default file position defined by labeler) when the project is saved."
    ),
    SampleListIncludedHeader(
        en = "Project sample files"
    ),
    SampleListIncludedItemEntryCountSingle(
        en = "%d entry"
    ),
    SampleListIncludedItemEntryCountPlural(
        en = "%d entries"
    ),
    SampleListExcludedHeader(
        en = "Other sample files"
    ),
    SampleListExcludedPlaceholder(
        en = "There are no unreferred sample files in the sample directory."
    ),
    SampleListEntryHeader(
        en = "Entries"
    ),
    SampleListEntriesPlaceholderUnselected(
        en = "Select a sample file in the left to show entries bound to it."
    ),
    SampleListEntriesPlaceholderNoEntry(
        en = "There are no entries bound to the selected sample file."
    ),
    SampleListEntriesPlaceholderNoEntryButton(
        en = "Create Default"
    ),
    SampleListJumpToSelectedEntryButton(
        en = "Go to selected entry"
    ),
    SampleListOpenSampleDirectoryButton(
        en = "Open sample directory"
    ),
    SampleListSampleDirectoryLabel(
        en = "Sample directory: "
    ),
    SampleListSampleDirectoryRedirectButton(
        en = "Change sample directory"
    ),
    EditorRenderStatusLabel(
        en = "%d/%d Rendering..."
    ),
    ChooseSampleDirectoryDialogTitle(
        en = "Choose sample directory"
    ),
    ChooseWorkingDirectoryDialogTitle(
        en = "Choose project location directory"
    ),
    ChooseCacheDirectoryDialogTitle(
        en = "Choose cache directory"
    ),
    ChooseInputFileDialogTitle(
        en = "Choose input file"
    ),
    OpenProjectDialogTitle(
        en = "Open project"
    ),
    SaveAsProjectDialogTitle(
        en = "Save as project"
    ),
    ExportDialogTitle(
        en = "Export"
    ),
    SetResolutionDialogDescription(
        en = "Input canvas resolution (points per pixel) for the editor (%d ~ %d)"
    ),
    AskIfSaveBeforeOpenDialogDescription(
        en = "You have unsaved changes. Do you want to save them before opening a new project?"
    ),
    AskIfSaveBeforeExportDialogDescription(
        en = "You have unsaved changes. Do you want to save them before exporting?"
    ),
    AskIfSaveBeforeCloseDialogDescription(
        en = "You have unsaved changes. Do you want to save them before closing the current project?"
    ),
    AskIfSaveBeforeExitDialogDescription(
        en = "You have unsaved changes. Do you want to save them before exiting?"
    ),
    InputEntryNameDialogDescription(
        en = "Rename entry"
    ),
    InputEntryNameDuplicateDialogDescription(
        en = "Input name for the new entry"
    ),
    InputEntryNameCutFormerDialogDescription(
        en = "Input name for the former entry after cutting"
    ),
    InputEntryNameCutLatterDialogDescription(
        en = "Input name for the latter entry after cutting"
    ),
    EditEntryNameDialogExistingError(
        en = "The name you input already exists."
    ),
    AskIfRemoveEntryDialogDescription(
        en = "Removing current entry..."
    ),
    AskIfRemoveEntryLastDialogDescription(
        en = "Removing current entry...\nThis entry is the only one that has reference of the current sample file." +
            "\nIf you need to add an entry on it later, please see menu `View` -> `Sample List`."
    ),
    AskIfLoadAutoSavedProjectDialogDescription(
        en = "Auto-saved project file found. Do you want to load it? If not, the file will be discarded."
    ),
    AskIfRedirectSampleDirectoryDialogDescription(
        en = "The sample directory of this project (%s) is not found. Do you want to redirect it to a new directory?"
    ),
    PluginDialogTitle(
        en = "vLabeler - Plugin"
    ),
    PluginDialogInfoAuthor(
        en = "author: %s"
    ),
    PluginDialogInfoVersion(
        en = "version: %d"
    ),
    PluginDialogInfoContact(
        en = "Contact author"
    ),
    PluginDialogDescriptionMin(
        en = "min: %s"
    ),
    PluginDialogDescriptionMax(
        en = "max: %s"
    ),
    PluginDialogDescriptionMinMax(
        en = "min: %s, max: %s"
    ),
    PluginDialogExecute(
        en = "Execute"
    ),
    PluginEntrySelectorTextMatchTypeEquals(
        en = "Equals"
    ),
    PluginEntrySelectorTextMatchTypeContains(
        en = "Contains"
    ),
    PluginEntrySelectorTextMatchTypeStartsWith(
        en = "Starts with"
    ),
    PluginEntrySelectorTextMatchTypeEndsWith(
        en = "Ends with"
    ),
    PluginEntrySelectorTextMatchTypeRegex(
        en = "Regex"
    ),
    PluginEntrySelectorNumberMatchTypeEquals(
        en = "="
    ),
    PluginEntrySelectorNumberMatchTypeGreaterThan(
        en = ">"
    ),
    PluginEntrySelectorNumberMatchTypeGreaterThanOrEquals(
        en = ">="
    ),
    PluginEntrySelectorNumberMatchTypeLessThan(
        en = "<"
    ),
    PluginEntrySelectorNumberMatchTypeLessThanOrEquals(
        en = "<="
    ),
    PluginEntrySelectorPreservedSubjectSample(
        en = "Sample name"
    ),
    PluginEntrySelectorPreservedSubjectName(
        en = "Entry name"
    ),
    PluginEntrySelectorComparerValue(
        en = "Input"
    ),
    PluginEntrySelectorPreviewSummaryError(
        en = "Invalid input"
    ),
    PluginEntrySelectorPreviewSummaryInitializing(
        en = "Initializing..."
    ),
    PluginEntrySelectorPreviewSummary(
        en = "Selects %d/%d"
    ),
    EditorSubTitleMultiple(
        en = "editing %d entries in sample %s"
    ),
    FailedToLoadSampleFileError(
        en = "Could not load the sample file.\nIt may not exist, or is not a supported format."
    ),
    EmptySampleDirectoryException(
        en = "Could not create project because no sample files are found in the given sample directory."
    ),
    PluginRuntimeUnexpectedException(
        en = "An unexpected error occurred during the plugin execution.\n" +
            "Please contact the author for more information."
    ),
    InvalidCreatedProjectException(
        en = "The created project is not valid.\nPlease contact author of the labeler/plugin for more information."
    ),
    InvalidOpenedProjectException(
        en = "Could not open the project because it contains invalid data.\n" +
            "Please check error log for more information."
    ),
    ProjectParseException(
        en = "Could not open the project.\nIt may be corrupted or created by an incompatible version of vLabeler."
    ),
    ProjectUpdateOnSampleException(
        en = "Could not update the project with the loaded sample file.\nPlease check error log for more information."
    ),
    InvalidEditedProjectException(
        en = "Invalid edited project.\nPlease check error log for more information."
    ),
    CustomizableItemLoadingException(
        en = "Could not load the selected customized item."
    ),
    LabelerManagerTitle(
        en = "Labelers"
    ),
    LabelerManagerImportDialogTitle(
        en = "Import labeler"
    ),
    TemplatePluginManagerTitle(
        en = "Template generators"
    ),
    TemplatePluginManagerImportDialogTitle(
        en = "Import template generator"
    ),
    MacroPluginManagerTitle(
        en = "Batch Edit Plugins"
    ),
    MacroPluginManagerImportDialogTitle(
        en = "Import batch edit plugin"
    ),
    CustomizableItemManagerRemoveItemConfirm(
        en = "Are you sure you want to remove \"%s\"? This will remove the file(s) from the disk."
    ),
    CustomizableItemManagerOpenDirectory(
        en = "Open directory"
    ),
    CustomizableItemManagerReload(
        en = "Reload list"
    ),
    CustomizableItemManagerLockedDescription(
        en = "This item is built-in and cannot be removed."
    ),
    PreferencesEditorResetPage(
        en = "Reset items in this page"
    ),
    PreferencesEditorResetAll(
        en = "Reset all items"
    ),
    PreferencesCharts(
        en = "Charts"
    ),
    PreferencesChartsDescription(
        en = "Customize the charts being rendered in the editor."
    ),
    PreferencesChartsCanvas(
        en = "Canvas"
    ),
    PreferencesChartsCanvasDescription(
        en = "Customize general settings about the canvas where the charts are drawn."
    ),
    PreferencesChartsCanvasResolution(
        en = "Canvas resolution"
    ),
    PreferencesChartsCanvasResolutionDescription(
        en = "Defined as number of sample points included in 1 pixel.\n" +
            "The bigger the number, the longer time duration the charts show in your screen."
    ),
    PreferencesChartsCanvasResolutionDefault(
        en = "Default resolution"
    ),
    PreferencesChartsCanvasResolutionStep(
        en = "Step"
    ),
    PreferencesChartsMaxDataChunkSize(
        en = "Max data chunk size"
    ),
    PreferencesChartsMaxDataChunkSizeDescription(
        en = "Max frames that will be included in a chart chunk.\n" +
            "The bigger the number, the less parts your charts will be divided to during rendering."
    ),
    PreferencesChartsWaveform(
        en = "Waveform"
    ),
    PreferencesChartsWaveformDescription(
        en = "Customize the waveform chart."
    ),
    PreferencesChartsWaveformUnitSize(
        en = "Frame size per pixel"
    ),
    PreferencesChartsWaveformIntensityAccuracy(
        en = "Bitmap height (px)"
    ),
    PreferencesChartsWaveformYAxisBlankRate(
        en = "Vertical padding (%%)"
    ),
    PreferencesChartsWaveformColor(
        en = "Color"
    ),
    PreferencesChartsWaveformBackgroundColor(
        en = "Background color"
    ),
    PreferencesChartsSpectrogram(
        en = "Spectrogram"
    ),
    PreferencesChartsSpectrogramDescription(
        en = "Customize the spectrogram chart."
    ),
    PreferencesChartsSpectrogramEnabled(
        en = "Show spectrogram"
    ),
    PreferencesChartsSpectrogramHeight(
        en = "Height relative to waveforms (%%)"
    ),
    PreferencesChartsSpectrogramPointPixelSize(
        en = "Point size (px)"
    ),
    PreferencesChartsSpectrogramHopSize(
        en = "FFT hop size"
    ),
    PreferencesChartsSpectrogramHopSizeDescription(
        en = "Adapted to the actual sample rate."
    ),
    PreferencesChartsSpectrogramWindowSize(
        en = "Window size"
    ),
    PreferencesChartsSpectrogramWindowSizeDescription(
        en = "Adapted to the actual sample rate."
    ),
    PreferencesChartsSpectrogramMelScaleStep(
        en = "Frequency resolution (mel)"
    ),
    PreferencesChartsSpectrogramMaxFrequency(
        en = "Max frequency displayed (Hz)"
    ),
    PreferencesChartsSpectrogramMinIntensity(
        en = "Min intensity displayed (dB)"
    ),
    PreferencesChartsSpectrogramMaxIntensity(
        en = "Max intensity displayed (dB)"
    ),
    PreferencesChartsSpectrogramWindowType(
        en = "Window function"
    ),
    PreferencesChartsSpectrogramColorPalette(
        en = "Colors"
    ),
    PreferencesKeymap(
        en = "Keymap"
    ),
    PreferencesKeymapDescription(
        en = "Customize key bindings for key/mouse actions."
    ),
    PreferencesKeymapKeyAction(
        en = "Key actions"
    ),
    PreferencesKeymapKeyActionDescription(
        en = "Customize key bindings for key actions."
    ),
    PreferencesKeymapMouseClickAction(
        en = "Mouse click actions"
    ),
    PreferencesKeymapMouseClickActionDescription(
        en = "Customize key bindings for mouse click actions.\n" +
            "An action is conducted only while all the keys in the key bindings are pressed."
    ),
    PreferencesKeymapMouseScrollAction(
        en = "Mouse scroll actions"
    ),
    PreferencesKeymapMouseScrollActionDescription(
        en = "Customize key bindings for mouse scroll actions.\n" +
            "An action is conducted only while all the keys in the key bindings are pressed."
    ),
    PreferencesKeymapEditDialogTitle(
        en = "Editing key bind for:"
    ),
    PreferencesKeymapEditDialogDescriptionMouseClick(
        en = "Left/Right click on the text field with other keys pressed to input a shortcut."
    ),
    PreferencesKeymapEditDialogDescriptionMouseScroll(
        en = "Scroll mouse wheel on the text field with other keys pressed to input a shortcut."
    ),
    PreferencesKeymapEditDialogConflictingLabel(
        en = "Already assigned to:"
    ),
    PreferencesKeymapEditDialogConflictingWarning(
        en = "This shortcut is already assigned to other actions.\nDo you want to remove the other assignments?"
    ),
    PreferencesKeymapEditDialogConflictingWarningKeep(
        en = "Keep"
    ),
    PreferencesKeymapEditDialogConflictingWarningRemove(
        en = "Remove"
    ),
    PreferencesEditor(
        en = "Editor"
    ),
    PreferencesEditorDescription(
        en = "Customize the editor's appearance and behavior."
    ),
    PreferencesEditorPlayerCursorColor(
        en = "Player cursor color"
    ),
    PreferencesEditorPlayerLockedDrag(
        en = "Fixed-drag"
    ),
    PreferencesEditorPlayerLockedDragDescription(
        en = "Select a condition to enable fixed-drag while you move the parameter lines.\n" +
            "When it is enabled, the other parameter lines will be moved accordingly to " +
            "keep relative positions to the parameter you are moving."
    ),
    PreferencesEditorPlayerLockedDragUseLabeler(
        en = "Use settings defined by the labeler"
    ),
    PreferencesEditorPlayerLockedDragUseStart(
        en = "Fixed-drag at the entry's start"
    ),
    PreferencesEditorPlayerLockedDragNever(
        en = "Never do fixed-drag"
    ),
    PreferencesEditorScissors(
        en = "Scissors"
    ),
    PreferencesEditorScissorsDescription(
        en = "Customize appearance and behavior of the scissors tool."
    ),
    PreferencesEditorScissorsColor(
        en = "Color"
    ),
    PreferencesEditorScissorsActionTargetNone(
        en = "None"
    ),
    PreferencesEditorScissorsActionTargetFormer(
        en = "The former entry"
    ),
    PreferencesEditorScissorsActionTargetLatter(
        en = "The latter entry"
    ),
    PreferencesEditorScissorsActionGoTo(
        en = "Go to entry after cutting"
    ),
    PreferencesEditorScissorsActionAskForName(
        en = "Rename entry after cutting"
    ),
    PreferencesEditorScissorsActionPlay(
        en = "Play audio when cutting"
    ),
    PreferencesEditorAutoScroll(
        en = "Auto scroll"
    ),
    PreferencesEditorAutoScrollDescription(
        en = "Define when the editor will automatically scroll to show the current entry."
    ),
    PreferencesEditorAutoScrollOnLoadedNewSample(
        en = "When switched to another sample"
    ),
    PreferencesEditorAutoScrollOnJumpedToEntry(
        en = "When switched to another entry by absolute index"
    ),
    PreferencesEditorAutoScrollOnSwitchedInMultipleEditMode(
        en = "When switched to another entry in multiple edit mode"
    ),
    PreferencesEditorAutoScrollOnSwitched(
        en = "When switched to another entry"
    ),
    PreferencesPlayback(
        en = "Playback"
    ),
    PreferencesPlaybackDescription(
        en = "Customize the behavior about audio playback."
    ),
    PreferencesPlaybackPlayOnDragging(
        en = "Preview on dragging"
    ),
    PreferencesPlaybackPlayOnDraggingDescription(
        en = "When dragging any parameter lines, play the audio range near the cursor."
    ),
    PreferencesPlaybackPlayOnDraggingEnabled(
        en = "Enabled"
    ),
    PreferencesPlaybackPlayOnDraggingRangeRadiusMillis(
        en = "Radius (ms)"
    ),
    PreferencesPlaybackPlayOnDraggingEventQueueSize(
        en = "Retain drag events"
    ),
    PreferencesAutoSave(
        en = "Auto save"
    ),
    PreferencesAutoSaveDescription(
        en = "Customize the behavior about project auto-save."
    ),
    PreferencesAutoSaveTarget(
        en = "Location of auto-saved file"
    ),
    PreferencesAutoSaveTargetNone(
        en = "Do not auto-save"
    ),
    PreferencesAutoSaveTargetProject(
        en = "Overwrite project file"
    ),
    PreferencesAutoSaveTargetRecord(
        en = "Save to temporary file"
    ),
    PreferencesAutoSaveIntervalSec(
        en = "Interval (sec)"
    ),
    ActionToggleSamplePlayback(
        en = "Toggle Playback of Current Sample"
    ),
    ActionToggleEntryPlayback(
        en = "Toggle Playback of Current Entry"
    ),
    ActionIncreaseResolution(
        en = "Zoom Out"
    ),
    ActionDecreaseResolution(
        en = "Zoom In"
    ),
    ActionInputResolution(
        en = "Input Canvas Resolution"
    ),
    ActionCancelDialog(
        en = "Close Dialog"
    ),
    ActionMoveParameter(
        en = "Drag Parameter Line"
    ),
    ActionMoveParameterWithPlaybackPreview(
        en = "Drag Parameter Line with Playback Preview"
    ),
    ActionMoveParameterInvertingLocked(
        en = "Drag Parameter Line with Fixed-drag Settings Inverted"
    ),
    ActionPlayAudioSection(
        en = "Play the Clicked Audio Part"
    ),
    ActionScrollCanvasLeft(
        en = "Scroll Canvas to Left"
    ),
    ActionScrollCanvasRight(
        en = "Scroll Canvas to Right"
    ),
    ActionZoomInCanvas(
        en = "Zoom In"
    ),
    ActionZoomOutCanvas(
        en = "Zoom Out"
    ),
    ActionGoToNextEntry(
        en = "Go to Next Entry"
    ),
    ActionGoToPreviousEntry(
        en = "Go to Previous Entry"
    ),
    ActionGoToNextSample(
        en = "Go to Next Sample"
    ),
    ActionGoToPreviousSample(
        en = "Go to Previous Sample"
    );

    fun get(language: Language): String = when (language) {
        English -> en
    }
}

fun string(key: Strings, vararg params: Any?): String {
    val template = key.get(currentLanguage)
    return template.format(*params)
}
