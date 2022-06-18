package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.FileDialog
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.HomePath
import com.sdercolin.vlabeler.util.isValidFileName
import com.sdercolin.vlabeler.util.update
import java.io.File

@Composable
fun BoxScope.Starter(
    appState: MutableState<AppState>,
    requestNewProject: (Project) -> Unit,
    availableLabelerConfs: List<LabelerConf>
) {
    Surface(Modifier.fillMaxSize()) {
        if (!appState.value.isConfiguringNewProject) {
            Column(
                modifier = Modifier.wrapContentSize()
                    .padding(30.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = string(Strings.AppName), style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(50.dp))

                Row {
                    OutlinedButton(
                        modifier = Modifier.size(180.dp, 120.dp),
                        onClick = { appState.update { copy(isConfiguringNewProject = true) } }
                    ) {
                        Text(string(Strings.StarterNewProject))
                    }
                    Spacer(Modifier.width(40.dp))
                    OutlinedButton(
                        modifier = Modifier.size(180.dp, 120.dp),
                        onClick = { appState.update { copy(isShowingOpenProjectDialog = true) } }
                    ) {
                        Text(string(Strings.StarterOpen))
                    }
                }
            }
        } else {
            NewProject(
                create = requestNewProject,
                cancel = { appState.update { copy(isConfiguringNewProject = false) } },
                availableLabelerConfs = availableLabelerConfs
            )
        }
    }
}

@Composable
private fun NewProject(create: (Project) -> Unit, cancel: () -> Unit, availableLabelerConfs: List<LabelerConf>) {
    var sampleDirectory by remember { mutableStateOf(HomePath.absolutePath) }
    var workingDirectory by remember { mutableStateOf(HomePath.absolutePath) }
    var workingDirectoryEdited by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var projectNameEdited by remember { mutableStateOf(false) }
    var currentPathPicker by remember { mutableStateOf<PathPicker?>(null) }
    var labeler by remember(availableLabelerConfs) { mutableStateOf(availableLabelerConfs.first()) }
    var inputLabelFile by remember { mutableStateOf("") }
    var inputLabelFileEdited by remember { mutableStateOf(false) }
    val encodings = listOf("UTF-8", "Shift-JIS")
    var encoding by remember(labeler) {
        val parser = labeler.parser
        val encodingName = if (parser != null) {
            encodings.find { it.equals(parser.defaultEncoding, ignoreCase = true) } ?: encodings.first()
        } else encodings.first()
        mutableStateOf(encodingName)
    }

    fun setSampleDirectory(path: String) {
        sampleDirectory = path
        if (!workingDirectoryEdited) {
            workingDirectory = sampleDirectory
        }
        if (!projectNameEdited && !workingDirectoryEdited) {
            projectName = if (File(path) != HomePath) path.trim('/').split("/").last() else ""
        }
        if (!inputLabelFileEdited) {
            inputLabelFile = if (File(path) != HomePath) {
                val parser = labeler.parser
                if (parser == null) "" else {
                    val file = File(path).resolve(parser.defaultInputFilePath)
                    if (file.exists()) file.absolutePath else ""
                }
            } else ""
        }
    }

    fun setWorkingDirectory(path: String) {
        workingDirectory = path
        if (!projectNameEdited) {
            projectName = if (File(path) != HomePath) path.trim('/').split("/").last() else ""
        }
    }

    fun isSampleDirectoryValid(): Boolean {
        val file = File(sampleDirectory)
        if (!file.isDirectory) return false
        return file.exists()
    }

    fun isWorkingDirectoryValid(): Boolean {
        val file = File(workingDirectory)
        if (file.parentFile?.exists() == false) return false
        return file.name.isValidFileName()
    }

    fun isProjectNameValid(): Boolean {
        return projectName.isValidFileName()
    }

    fun isInputLabelFileValid(): Boolean {
        if (inputLabelFile == "") return true
        val file = File(inputLabelFile)
        return file.extension == labeler.parser?.extension && file.exists()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 60.dp, vertical = 40.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = string(Strings.StarterNewProject), style = MaterialTheme.typography.h4, maxLines = 1)
        Spacer(Modifier.height(25.dp))
        listOf<@Composable () -> Unit>({
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = sampleDirectory,
                onValueChange = { setSampleDirectory(it) },
                label = { Text(string(Strings.StarterNewSampleDirectory)) },
                maxLines = 2,
                trailingIcon = {
                    IconButton(onClick = { currentPathPicker = PathPicker.SampleDirectory }) {
                        Icon(Icons.Default.FolderOpen, null)
                    }
                },
                isError = isSampleDirectoryValid().not()
            )
        }, {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = workingDirectory,
                onValueChange = {
                    workingDirectoryEdited = true
                    setWorkingDirectory(it)
                },
                label = { Text(string(Strings.StarterNewWorkingDirectory)) },
                maxLines = 2,
                trailingIcon = {
                    IconButton(onClick = { currentPathPicker = PathPicker.WorkingDirectory }) {
                        Icon(Icons.Default.FolderOpen, null)
                    }
                },
                isError = isWorkingDirectoryValid().not()
            )
        }, {
            OutlinedTextField(
                modifier = Modifier.widthIn(min = 300.dp),
                value = projectName,
                onValueChange = {
                    projectName = it
                    projectNameEdited = true
                },
                label = { Text(string(Strings.StarterNewProjectName)) },
                maxLines = 2,
                isError = isProjectNameValid().not()
            )
        }, {
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextField(
                    modifier = Modifier.widthIn(min = 400.dp),
                    value = labeler.displayedName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(string(Strings.StarterNewLabeler)) },
                    maxLines = 1,
                    leadingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ExpandMore, null)
                        }
                    }
                )
                DropdownMenu(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableLabelerConfs.forEach { conf ->
                        DropdownMenuItem(
                            onClick = {
                                labeler = conf
                                expanded = false
                            }
                        ) {
                            Text(text = conf.displayedName)
                        }
                    }
                }
            }
        }, {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = if (labeler.parser != null) inputLabelFile else "",
                onValueChange = {
                    inputLabelFile = it
                    inputLabelFileEdited = true
                },
                enabled = labeler.parser != null,
                label = { Text(string(Strings.StarterNewInputLabelFile)) },
                placeholder = { Text(string(Strings.StarterNewInputLabelFilePlaceholder)) },
                maxLines = 2,
                trailingIcon = {
                    IconButton(onClick = { currentPathPicker = PathPicker.InputFile }) {
                        Icon(Icons.Default.FolderOpen, null)
                    }
                },
                isError = isInputLabelFileValid().not()
            )
        }, {
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextField(
                    modifier = Modifier.widthIn(min = 200.dp),
                    value = encoding,
                    onValueChange = { },
                    enabled = labeler.parser != null && inputLabelFile != "",
                    readOnly = true,
                    label = { Text(string(Strings.StarterNewEncoding)) },
                    maxLines = 1,
                    leadingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ExpandMore, null)
                        }
                    }
                )
                DropdownMenu(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    encodings.forEach { encodingName ->
                        DropdownMenuItem(
                            onClick = {
                                encoding = encodingName
                                expanded = false
                            }
                        ) {
                            Text(text = encodingName)
                        }
                    }
                }
            }
        }).forEach {
            it.invoke()
            Spacer(Modifier.height(20.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = cancel) {
                Text(string(Strings.CommonCancel))
            }
            Button(
                onClick = {
                    create(
                        Project.from(
                            sampleDirectory = sampleDirectory,
                            workingDirectory = workingDirectory,
                            projectName = projectName,
                            labelerConf = labeler,
                            inputLabelFile = inputLabelFile,
                            encoding = encoding
                        )
                    )
                },
                enabled = isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid()
            ) {
                Text(string(Strings.CommonOkay))
            }
        }
        currentPathPicker?.let { picker ->
            val title = when (picker) {
                PathPicker.SampleDirectory -> string(Strings.ChooseSampleDirectoryDialogTitle)
                PathPicker.WorkingDirectory -> string(Strings.ChooseWorkingDirectoryDialogTitle)
                PathPicker.InputFile -> string(Strings.ChooseInputLabelFileDialogTitle)
            }
            val initial = when (picker) {
                PathPicker.SampleDirectory -> sampleDirectory
                PathPicker.WorkingDirectory -> workingDirectory
                PathPicker.InputFile -> if (inputLabelFile != "" && isInputLabelFileValid()) {
                    File(inputLabelFile).parent
                } else {
                    sampleDirectory
                }
            }
            val extensions = when (picker) {
                PathPicker.SampleDirectory -> listOf(Project.SampleFileExtension)
                PathPicker.WorkingDirectory -> null
                PathPicker.InputFile -> labeler.parser?.extension?.let { listOf(it) }
            }
            FileDialog(
                title = title,
                initialDirectory = initial,
                extensions = extensions
            ) { directory, file ->
                currentPathPicker = null
                directory ?: return@FileDialog
                when (picker) {
                    PathPicker.SampleDirectory -> setSampleDirectory(directory)
                    PathPicker.WorkingDirectory -> {
                        workingDirectoryEdited = true
                        setWorkingDirectory(directory)
                    }
                    PathPicker.InputFile -> {
                        inputLabelFileEdited = true
                        inputLabelFile = File(directory, file ?: "").absolutePath
                    }
                }
            }
        }
    }
}

private enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    InputFile
}
