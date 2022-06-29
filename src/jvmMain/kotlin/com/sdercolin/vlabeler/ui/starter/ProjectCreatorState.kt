package com.sdercolin.vlabeler.ui.starter

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.EmptySampleDirectoryException
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.detectEncoding
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.getDirectory
import com.sdercolin.vlabeler.util.isValidFileName
import com.sdercolin.vlabeler.util.lastPathSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ProjectCreatorState(availableLabelerConfs: List<LabelerConf>) {
    var isLoading: Boolean by mutableStateOf(false)
    var sampleDirectory: String by mutableStateOf(HomeDir.absolutePath)
        private set
    var workingDirectory: String by mutableStateOf(HomeDir.absolutePath)
        private set
    private var workingDirectoryEdited: Boolean by mutableStateOf(false)
    var projectName: String by mutableStateOf("")
        private set
    private var projectNameEdited: Boolean by mutableStateOf(false)
    var currentPathPicker: PathPicker? by mutableStateOf(null)
        private set
    var labeler: LabelerConf by mutableStateOf(availableLabelerConfs.first())
    var inputLabelFile: String by mutableStateOf("")
        private set
    private var inputLabelFileEdited: Boolean by mutableStateOf(false)
    val encodings = AvailableEncodings

    private val encodingState = mutableStateOf(
        run {
            val parser = labeler.parser
            val encodingName = encodings.find { encodingNameEquals(parser.defaultEncoding, it) }
                ?: encodings.first().takeIf { it.isNotBlank() }
                ?: encodings.first()
            encodingName
        }
    )
    var encoding: String by encodingState

    fun updateSampleDirectory(path: String) {
        sampleDirectory = path
        if (!workingDirectoryEdited) {
            workingDirectory = sampleDirectory
        }
        if (!projectNameEdited && !workingDirectoryEdited) {
            projectName = if (File(path).absolutePath != HomeDir.absolutePath) path.lastPathSection else ""
        }
        if (!inputLabelFileEdited) {
            inputLabelFile = if (File(path).absolutePath != HomeDir.absolutePath) {
                val file = labeler.defaultInputFilePath?.let { File(path).resolve(it) }
                if (file?.exists() == true) file.absolutePath else ""
            } else ""
        }
    }

    fun updateWorkingDirectory(path: String) {
        workingDirectoryEdited = true
        workingDirectory = path
        if (!projectNameEdited) {
            projectName = if (File(path).absolutePath != HomeDir.absolutePath) path.lastPathSection else ""
        }
    }

    fun updateProjectName(name: String) {
        projectNameEdited = true
        projectName = name
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

    fun isProjectFileExisting(): Boolean {
        return if (isWorkingDirectoryValid() && isProjectNameValid()) {
            File(workingDirectory, "$projectName.${Project.ProjectFileExtension}").exists()
        } else false
    }

    fun updateInputLabelFile(scope: CoroutineScope, path: String) {
        scope.launch(Dispatchers.IO) {
            inputLabelFileEdited = true
            inputLabelFile = path
            val file = File(path)
            if (file.isFile && file.exists()) {
                val detectedEncoding = file.readBytes().detectEncoding() ?: return@launch
                encoding = encodings.find { encodingNameEquals(detectedEncoding, it) } ?: detectedEncoding
            }
        }
    }

    fun isInputLabelFileValid(): Boolean {
        if (inputLabelFile == "") return true
        val file = File(inputLabelFile)
        return file.extension == labeler.extension && file.exists()
    }

    fun isValid(): Boolean = isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid()

    fun pickSampleDirectory() {
        currentPathPicker = PathPicker.SampleDirectory
    }

    fun pickWorkingDirectory() {
        currentPathPicker = PathPicker.WorkingDirectory
    }

    fun pickInputFile() {
        currentPathPicker = PathPicker.InputFile
    }

    val isEncodingSelectionEnabled get() = inputLabelFile != ""

    fun getFilePickerDirectoryMode(picker: PathPicker) =
        picker != PathPicker.InputFile

    fun getFilePickerExtensions(
        picker: PathPicker
    ) = when (picker) {
        PathPicker.SampleDirectory -> listOf(Project.SampleFileExtension)
        PathPicker.WorkingDirectory -> null
        PathPicker.InputFile -> listOf(labeler.extension)
    }

    fun getFilePickerInitialDirectory(
        picker: PathPicker
    ) = when (picker) {
        PathPicker.SampleDirectory -> sampleDirectory
        PathPicker.WorkingDirectory -> workingDirectory
        PathPicker.InputFile -> if (inputLabelFile != "" && isInputLabelFileValid()) {
            File(inputLabelFile).parent.orEmpty()
        } else {
            sampleDirectory
        }
    }

    fun getFilePickerTitle(picker: PathPicker) = when (picker) {
        PathPicker.SampleDirectory -> string(Strings.ChooseSampleDirectoryDialogTitle)
        PathPicker.WorkingDirectory -> string(Strings.ChooseWorkingDirectoryDialogTitle)
        PathPicker.InputFile -> string(Strings.ChooseInputLabelFileDialogTitle)
    }

    fun handleFilePickerResult(
        picker: PathPicker,
        parent: String?,
        name: String?,
        coroutineScope: CoroutineScope
    ) {
        currentPathPicker = null
        if (parent == null || name == null) return
        val file = File(parent, name)
        when (picker) {
            PathPicker.SampleDirectory -> {
                updateSampleDirectory(file.getDirectory().absolutePath)
            }
            PathPicker.WorkingDirectory -> {
                updateWorkingDirectory(file.getDirectory().absolutePath)
            }
            PathPicker.InputFile -> {
                updateInputLabelFile(coroutineScope, file.absolutePath)
            }
        }
    }

    fun create(
        coroutineScope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        create: (Project) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            Log.debug(
                "Create project. sampleDir=$sampleDirectory, " +
                    "workingDir=$workingDirectory, " +
                    "projectName=$projectName, " +
                    "labeler=${labeler.name}, " +
                    "input=$inputLabelFile, " +
                    "encoding=$encoding"
            )
            val project = Project.from(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                labelerConf = labeler,
                inputLabelFile = inputLabelFile,
                encoding = encoding
            ).getOrElse {
                val message = when (it) {
                    is EmptySampleDirectoryException -> string(
                        Strings.EmptySampleDirectoryException
                    )
                    else -> it.message.orEmpty()
                }
                Log.error(it)
                snackbarHostState.showSnackbar(message)
                null
            }
            project?.let(create)
            isLoading = false
        }
    }
}

@Composable
fun rememberProjectCreatorState(availableLabelerConfs: List<LabelerConf>) = remember(availableLabelerConfs) {
    ProjectCreatorState(availableLabelerConfs)
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    InputFile
}
