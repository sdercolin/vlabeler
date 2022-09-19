@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.repository.ToolCursorRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.DoneIcon
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.StarIcon
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.PropertyView
import com.sdercolin.vlabeler.ui.editor.RenderStatusLabel
import com.sdercolin.vlabeler.ui.editor.ToolboxView
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun Labeler(
    editorState: EditorState,
    appState: AppState,
) {
    val project = editorState.project
    val openEditEntryNameDialog = remember(editorState, project) {
        { editorState.openEditEntryNameDialog(project.currentIndex, InputEntryNameDialogPurpose.Rename) }
    }
    val horizontalScrollState = rememberScrollState(0)

    LaunchedEffect(editorState) {
        editorState.scrollFitViewModel.eventFlow.distinctUntilChanged().collectLatest {
            if (appState.isScrollFitEnabled.not()) return@collectLatest
            horizontalScrollState.animateScrollTo(it)
        }
    }

    Column(Modifier.fillMaxSize()) {
        EntryTitleBar(
            appConf = appState.appConf,
            title = editorState.entryTitle,
            subTitle = editorState.getEntrySubTitle(),
            multiple = editorState.editedEntries.size > 1,
            done = editorState.entryDone,
            toggleDone = { editorState.toggleEntryDone(editorState.project.currentIndex) },
            star = editorState.entryStar,
            toggleStar = { editorState.toggleEntryStar(editorState.project.currentIndex) },
            tag = editorState.entryTag,
            editTag = { editorState.editEntryTag(editorState.project.currentIndex, it) },
            isEditingTag = editorState.isEditingTag,
            setEditingTag = { editorState.isEditingTag = it },
            tagOptions = editorState.tagOptions,
            openEditEntryNameDialog = openEditEntryNameDialog,
        )
        val cursor = remember(editorState.tool) {
            ToolCursorRepository.get(editorState.tool)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(width = 0.5.dp, color = Black50)
                .pointerHoverIcon(PointerIcon(cursor)),
        ) {
            Canvas(
                horizontalScrollState = horizontalScrollState,
                editorState = editorState,
                appState = appState,
            )
            if (appState.isPropertyViewDisplayed) {
                PropertyView(editorState.project)
            }
            if (appState.isToolboxDisplayed) {
                ToolboxView(
                    selectedTool = editorState.tool,
                    select = { editorState.tool = it },
                )
            }
            RenderStatusLabel(editorState.renderProgress)
        }
        HorizontalScrollbar(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            adapter = rememberScrollbarAdapter(horizontalScrollState),
        )
        val bottomBarState = rememberBottomBarState(project, appState, editorState)
        BottomBar(bottomBarState, appState)
    }
}

@Composable
private fun EntryTitleBar(
    appConf: AppConf,
    title: String,
    subTitle: String,
    multiple: Boolean,
    done: Boolean,
    toggleDone: () -> Unit,
    star: Boolean,
    toggleStar: () -> Unit,
    tag: String,
    editTag: (String) -> Unit,
    isEditingTag: Boolean,
    setEditingTag: (Boolean) -> Unit,
    tagOptions: List<String>,
    openEditEntryNameDialog: () -> Unit,
) {
    Surface {
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(80.dp)
                .background(color = MaterialTheme.colors.surface)
                .padding(horizontal = 20.dp),
        ) {
            Row(modifier = Modifier.fillMaxSize().align(Alignment.CenterStart)) {
                Row(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                    Text(
                        modifier = Modifier.alignByBaseline()
                            .clickable(enabled = !multiple) { openEditEntryNameDialog() },
                        text = title,
                        style = MaterialTheme.typography.h3,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = "（$subTitle）",
                        style = MaterialTheme.typography.h5,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (appConf.editor.let { it.showDone || it.showStar || it.showTag }) {
                    Spacer(Modifier.width(20.dp))
                    Row(
                        modifier = Modifier.align(Alignment.Bottom)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        if (appConf.editor.showTag) {
                            TagRegion(
                                tag = tag,
                                editTag = editTag,
                                tagOptions = tagOptions,
                                isEditing = isEditingTag,
                                setEditing = setEditingTag,
                            )
                        }
                        if (appConf.editor.showDone) {
                            FreeSizedIconButton(
                                onClick = toggleDone,
                                modifier = Modifier.padding(8.dp),
                            ) {
                                DoneIcon(done)
                            }
                        }
                        if (appConf.editor.showStar) {
                            FreeSizedIconButton(
                                onClick = toggleStar,
                                modifier = Modifier.padding(8.dp),
                            ) {
                                StarIcon(star)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRegion(
    tag: String,
    editTag: (String) -> Unit,
    tagOptions: List<String>,
    isEditing: Boolean,
    setEditing: (Boolean) -> Unit,
) {
    var isTextFieldFocused by remember { mutableStateOf(false) }
    var editingTag by remember(tag, isEditing) { mutableStateOf(TextFieldValue(tag, TextRange(0, tag.length))) }

    val focusRequester = remember { FocusRequester() }

    Box(contentAlignment = Alignment.BottomEnd) {
        val textBoxModifier = Modifier
            .padding(vertical = 5.dp, horizontal = 3.dp)
            .widthIn(min = 35.dp, max = 150.dp)
            .background(color = White20, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        val textStyle = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.8f))
        if (!isEditing) {
            LaunchedEffect(Unit) {
                isTextFieldFocused = false
            }
            if (tag.isEmpty()) {
                FreeSizedIconButton(
                    onClick = { setEditing(true) },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.NewLabel,
                        contentDescription = null,
                        tint = White20,
                    )
                }
            } else {
                Text(
                    modifier = textBoxModifier.clickable { setEditing(true) },
                    text = tag,
                    textAlign = TextAlign.Center,
                    style = textStyle,
                    maxLines = 1,
                )
            }
        } else {
            LaunchedEffect(Unit) {
                println("request focus")
                focusRequester.requestFocus()
            }

            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        println("focus changed: $it")
                        if (isTextFieldFocused && it.hasFocus.not()) {
                            println("focus lost")
                            setEditing(false)
                            if (editingTag.text != tag) {
                                editTag(editingTag.text)
                            }
                        }
                        isTextFieldFocused = it.hasFocus
                    },
                value = editingTag,
                onValueChange = { editingTag = it },
                textStyle = textStyle,
                cursorBrush = SolidColor(LightGray),
                maxLines = 1,
                decorationBox = {
                    Box(
                        modifier = textBoxModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        it()
                    }
                },
            )

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 80),
                focusable = false,
                onDismissRequest = {},
            ) {
                Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                    tagOptions.forEach { option ->
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colors.surface)
                                .clickable {
                                    setEditing(false)
                                    editTag(option)
                                }
                                .padding(10.dp),
                            text = option,
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        }
    }
}
