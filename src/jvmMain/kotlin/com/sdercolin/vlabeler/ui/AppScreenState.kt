package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.ui.editor.EditorState
import java.io.File

interface AppScreenState {
    var screen: Screen
    val editor: EditorState?
}

sealed class Screen {
    object Starter : Screen()
    class ProjectCreator(val initialFile: File? = null) : Screen()
    class Editor(val state: EditorState) : Screen()
}

class AppScreenStateImpl : AppScreenState {
    override var screen: Screen by mutableStateOf(Screen.Starter)

    override val editor: EditorState?
        get() = (screen as? Screen.Editor)?.state
}
