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
        en = "New project"
    ),
    MenuFileOpen(
        en = "Open"
    ),
    MenuFileSave(
        en = "Save"
    ),
    MenuFileSaveAs(
        en = "Save as"
    ),
    MenuFileExport(
        en = "Export"
    ),
    MenuFileClose(
        en = "Close"
    ),
    MenuEdit(
        en = "Edit"
    ),
    MenuJumpToEntry(
        en = "Go to entry..."
    ),
    CommonOkay(
        en = "OK"
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
    StarterNewProject(
        en = "New Project"
    ),
    StarterOpen(
        en = "Open"
    ),
    StarterNewSampleDirectory(
        en = "Sample directory"
    ),
    StarterNewWorkingDirectory(
        en = "Working directory"
    ),
    StarterNewProjectName(
        en = "Project name"
    ),
    StarterNewProjectNameWarning(
        en = "The project file already exists. Creating project will overwrite the file."
    ),
    StarterNewLabeler(
        en = "Labeler"
    ),
    StarterNewInputLabelFile(
        en = "Input label file"
    ),
    StarterNewInputLabelFilePlaceholder(
        en = "(Create template if this is not set)"
    ),
    StarterNewEncoding(
        en = "Encoding"
    ),
    ChooseSampleDirectoryDialogTitle(
        en = "Choose sample directory"
    ),
    ChooseWorkingDirectoryDialogTitle(
        en = "Choose sample directory"
    ),
    ChooseInputLabelFileDialogTitle(
        en = "Choose input label file"
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
        en = "Input horizontal resolution (points per pixel) for the editor (%d ~ %d)"
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
    JumpToEntryDialogSearchPlaceholder(
        en = "Search for entry..."
    ),
    EmptySampleDirectoryException(
        en = "Could not create project because no sample files are found in the given sample directory."
    );

    fun get(language: Language): String = when (language) {
        English -> en
    }
}

fun string(key: Strings, vararg params: Any?): String {
    val template = key.get(currentLanguage)
    return template.format(*params)
}
