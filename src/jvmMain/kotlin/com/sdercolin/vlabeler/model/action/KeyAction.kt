package com.sdercolin.vlabeler.model.action

import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Strings

enum class KeyAction(
    val displayedNameSections: List<Strings>,
    val defaultKeySets: List<KeySet>,
    val isInMenu: Boolean
) {
    NewProject(
        listOf(Strings.MenuFile, Strings.MenuFileNewProject),
        listOf(KeySet(Key.N, setOf(Key.Ctrl, Key.Shift))),
        true
    ),
    OpenProject(
        listOf(Strings.MenuFile, Strings.MenuFileOpen),
        listOf(KeySet(Key.O, setOf(Key.Ctrl, Key.Shift))),
        true
    ),
    ClearRecentProjects(
        listOf(Strings.MenuFile, Strings.MenuFileOpenRecent, Strings.MenuFileOpenRecentClear),
        listOf(),
        true
    ),
    SaveProject(
        listOf(Strings.MenuFile, Strings.MenuFileSave),
        listOf(KeySet(Key.S, setOf(Key.Ctrl))),
        true
    ),
    SaveProjectAs(
        listOf(Strings.MenuFile, Strings.MenuFileSaveAs),
        listOf(KeySet(Key.S, setOf(Key.Ctrl, Key.Shift))),
        true
    ),
    ExportProject(
        listOf(Strings.MenuFile, Strings.MenuFileExport),
        listOf(KeySet(Key.E, setOf(Key.Ctrl))),
        true
    ),
    InvalidateCaches(
        listOf(Strings.MenuFile, Strings.MenuFileInvalidateCaches),
        listOf(KeySet(Key.I, setOf(Key.Ctrl, Key.Shift))),
        true
    ),
    CloseProject(
        listOf(Strings.MenuFile, Strings.MenuFileClose),
        listOf(KeySet(Key.W, setOf(Key.Ctrl))),
        true
    ),
    Undo(
        listOf(Strings.MenuEdit, Strings.MenuEditUndo),
        listOf(KeySet(Key.Z, setOf(Key.Ctrl))),
        true
    ),
    Redo(
        listOf(Strings.MenuEdit, Strings.MenuEditRedo),
        listOf(KeySet(Key.Z, setOf(Key.Ctrl, Key.Shift))),
        true
    ),
    UseToolCursor(
        listOf(Strings.MenuEdit, Strings.MenuEditTools, Strings.MenuEditToolsCursor),
        listOf(KeySet(Key.One)),
        true
    ),
    UseToolScissors(
        listOf(Strings.MenuEdit, Strings.MenuEditTools, Strings.MenuEditToolsScissors),
        listOf(KeySet(Key.Two)),
        true
    ),
    RenameCurrentEntry(
        listOf(Strings.MenuEdit, Strings.MenuEditRenameEntry),
        listOf(KeySet(Key.R, setOf(Key.Ctrl))),
        true
    ),
    DuplicateCurrentEntry(
        listOf(Strings.MenuEdit, Strings.MenuEditDuplicateEntry),
        listOf(KeySet(Key.D, setOf(Key.Ctrl))),
        true
    ),
    RemoveCurrentEntry(
        listOf(Strings.MenuEdit, Strings.MenuEditRemoveEntry),
        listOf(KeySet(Key.Delete)),
        true
    ),
    ToggleMultipleEditMode(
        listOf(Strings.MenuEdit, Strings.MenuEditMultipleEditMode),
        listOf(KeySet(Key.M, setOf(Key.Ctrl))),
        true
    ),
    ToggleMarker(
        listOf(Strings.MenuView, Strings.MenuViewToggleMarker),
        listOf(KeySet(Key.Zero, setOf(Key.Ctrl))),
        true
    ),
    ToggleProperties(
        listOf(Strings.MenuView, Strings.MenuViewToggleProperties),
        listOf(KeySet(Key.One, setOf(Key.Ctrl))),
        true
    ),
    TogglePinnedEntryList(
        listOf(Strings.MenuView, Strings.MenuViewPinEntryList),
        listOf(KeySet(Key.Two, setOf(Key.Ctrl))),
        true
    ),
    ToggleToolbox(
        listOf(Strings.MenuView, Strings.MenuViewToggleToolbox),
        listOf(KeySet(Key.Three, setOf(Key.Ctrl))),
        true
    ),
    OpenSampleList(
        listOf(Strings.MenuView, Strings.MenuViewOpenSampleList),
        listOf(KeySet(Key.Nine, setOf(Key.Ctrl))),
        true
    ),
    NavigateNextEntry(
        listOf(Strings.MenuNavigate, Strings.MenuNavigateNextEntry),
        listOf(KeySet(Key.Down)),
        true
    ),
    NavigatePreviousEntry(
        listOf(Strings.MenuNavigate, Strings.MenuNavigatePreviousEntry),
        listOf(KeySet(Key.Up)),
        true
    ),
    NavigateNextSample(
        listOf(Strings.MenuNavigate, Strings.MenuNavigateNextSample),
        listOf(KeySet(Key.Down, setOf(Key.Ctrl))),
        true
    ),
    NavigatePreviousSample(
        listOf(Strings.MenuNavigate, Strings.MenuNavigatePreviousSample),
        listOf(KeySet(Key.Up, setOf(Key.Ctrl))),
        true
    ),
    NavigateJumpToEntry(
        listOf(Strings.MenuNavigate, Strings.MenuNavigateJumpToEntry),
        listOf(KeySet(Key.G, setOf(Key.Ctrl))),
        true
    ),
    NavigateScrollFit(
        listOf(Strings.MenuNavigate, Strings.MenuNavigateScrollFit),
        listOf(KeySet(Key.F)),
        true
    ),
    ManageMacroPlugins(
        listOf(Strings.MenuTools, Strings.MenuToolsBatchEdit, Strings.MenuToolsBatchEditManagePlugins),
        listOf(KeySet(Key.Semicolon, setOf(Key.Ctrl))),
        true
    ),
    ManageTemplatePlugins(
        listOf(Strings.MenuSettings, Strings.MenuSettingsTemplatePlugins),
        listOf(KeySet(Key.Apostrophe, setOf(Key.Ctrl))),
        true
    ),
    Preferences(
        listOf(Strings.MenuSettings, Strings.MenuSettingsPreferences),
        listOf(KeySet(Key.Comma, setOf(Key.Ctrl))),
        true
    ),
    ManageLabelers(
        listOf(Strings.MenuSettings, Strings.MenuSettingsLabelers),
        listOf(KeySet(Key.Period, setOf(Key.Ctrl))),
        true
    ),
    OpenLogDirectory(listOf(Strings.MenuHelp, Strings.MenuHelpOpenLogDirectory), listOf(), true),
    OpenLatestRelease(listOf(Strings.MenuHelp, Strings.MenuHelpOpenLatestRelease), listOf(), true),
    OpenGitHub(listOf(Strings.MenuHelp, Strings.MenuHelpOpenGitHub), listOf(), true),
    JoinDiscord(listOf(Strings.MenuHelp, Strings.MenuHelpJoinDiscord), listOf(), true)
}
