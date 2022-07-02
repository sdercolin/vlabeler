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
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppRecordStore
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

class ProjectCreatorState(
    availableLabelerConfs: List<LabelerConf>,
    private val appRecordStore: AppRecordStore
) {
    private val appRecord get() = appRecordStore.stateFlow.value
    var isLoading: Boolean by mutableStateOf(false)
    var sampleDirectory: String by mutableStateOf(appRecord.sampleDirectory ?: HomeDir.absolutePath)
        private set
    var workingDirectory: String by mutableStateOf(appRecord.workingDirectory ?: HomeDir.absolutePath)
        private set
    private var workingDirectoryEdited: Boolean by mutableStateOf(false)
    var projectName: String by mutableStateOf("")
        private set
    private var projectNameEdited: Boolean by mutableStateOf(false)
    var currentPathPicker: PathPicker? by mutableStateOf(null)
        private set
    var labeler: LabelerConf by mutableStateOf(
        availableLabelerConfs.firstOrNull { it.name == appRecord.labelerName } ?: availableLabelerConfs.first()
    )
        private set
    var templatePlugin: Plugin? by mutableStateOf(null)
    val templateName: String get() = templatePlugin?.displayedName ?: string(Strings.StarterNewTemplatePluginNone)
    var inputFile: String by mutableStateOf("")
        private set
    private var inputFileEdited: Boolean by mutableStateOf(false)
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
        if (!inputFileEdited) {
            inputFile = if (File(path).absolutePath != HomeDir.absolutePath) {
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

    fun getSupportedInputFileExtension(): String? {
        val plugin = templatePlugin
        if (plugin != null) {
            return plugin.inputFileExtension
        }
        return labeler.extension
    }

    fun updateLabeler(labeler: LabelerConf) {
        this.labeler = labeler
        if (templatePlugin?.supportedLabelFileExtension != labeler.extension) {
            templatePlugin = null
        }
        resetInputFileIfNeeded()
    }

    fun updatePlugin(plugin: Plugin?) {
        templatePlugin = plugin
        resetInputFileIfNeeded()
    }

    private fun resetInputFileIfNeeded() {
        val supportedExtension = getSupportedInputFileExtension()
        if (supportedExtension == null || inputFile.endsWith(supportedExtension).not()) {
            inputFile = ""
        }
    }

    fun updateInputFile(scope: CoroutineScope, path: String) {
        scope.launch(Dispatchers.IO) {
            inputFileEdited = true
            inputFile = path
            val file = File(path)
            if (file.isFile && file.exists()) {
                val detectedEncoding = file.readBytes().detectEncoding() ?: return@launch
                encoding = encodings.find { encodingNameEquals(detectedEncoding, it) } ?: detectedEncoding
            }
        }
    }

    fun isInputFileEnabled(): Boolean = getSupportedInputFileExtension() != null

    fun getInputFileLabelText(): String {
        val extension = getSupportedInputFileExtension()
        return if (extension == null) {
            string(Strings.StarterNewInputFile)
        } else {
            string(Strings.StarterNewInputFileWithExtension, extension)
        }
    }

    fun getInputFilePlaceholderText(): String? {
        val plugin = templatePlugin ?: return string(Strings.StarterNewInputFilePlaceholder)

        if (plugin.requireInputFile) {
            return null
        }
        if (plugin.inputFileExtension == null) {
            return string(Strings.StarterNewInputFilePlaceholderPluginNotRequired)
        }
        return null
    }

    fun isInputFileValid(): Boolean {
        val plugin = templatePlugin
        if (plugin == null) {
            if (inputFile == "") {
                // Use default template in labeler
                return true
            }
            val file = File(inputFile)
            return file.extension == labeler.extension && file.exists()
        } else {
            if (inputFile == "") return plugin.requireInputFile.not()

            val file = File(inputFile)
            return file.extension == plugin.inputFileExtension && file.exists()
        }
    }

    fun isValid(): Boolean = isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid() &&
        isInputFileValid()

    fun pickSampleDirectory() {
        currentPathPicker = PathPicker.SampleDirectory
    }

    fun pickWorkingDirectory() {
        currentPathPicker = PathPicker.WorkingDirectory
    }

    fun pickInputFile() {
        currentPathPicker = PathPicker.InputFile
    }

    val isEncodingSelectionEnabled get() = inputFile != ""

    fun getFilePickerDirectoryMode(picker: PathPicker) =
        picker != PathPicker.InputFile

    fun getFilePickerExtensions(
        picker: PathPicker
    ) = when (picker) {
        PathPicker.SampleDirectory -> listOf(Project.SampleFileExtension)
        PathPicker.WorkingDirectory -> null
        PathPicker.InputFile -> getSupportedInputFileExtension()?.let { listOf(it) }
    }

    fun getFilePickerInitialDirectory(
        picker: PathPicker
    ) = when (picker) {
        PathPicker.SampleDirectory -> sampleDirectory
        PathPicker.WorkingDirectory -> workingDirectory
        PathPicker.InputFile -> if (inputFile != "" && isInputFileValid()) {
            File(inputFile).parent.orEmpty()
        } else {
            sampleDirectory
        }
    }

    fun getFilePickerTitle(picker: PathPicker) = when (picker) {
        PathPicker.SampleDirectory -> string(Strings.ChooseSampleDirectoryDialogTitle)
        PathPicker.WorkingDirectory -> string(Strings.ChooseWorkingDirectoryDialogTitle)
        PathPicker.InputFile -> string(Strings.ChooseInputFileDialogTitle)
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
                updateInputFile(coroutineScope, file.absolutePath)
            }
        }
    }

    fun getSupportedPlugins(plugins: List<Plugin>) = plugins
        .filter { it.type == Plugin.Type.Template }
        .filter { it.supportedLabelFileExtension == labeler.extension }
        .sortedBy { it.displayedName }

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
                    "input=$inputFile, " +
                    "encoding=$encoding"
            )
            appRecordStore.update {
                copy(
                    sampleDirectory = this@ProjectCreatorState.sampleDirectory,
                    workingDirectory = this@ProjectCreatorState.workingDirectory,
                    labelerName = this@ProjectCreatorState.labeler.name
                )
            }
            val project = projectOf(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                labelerConf = labeler,
                plugin = templatePlugin,
                inputFilePath = inputFile,
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
fun rememberProjectCreatorState(
    availableLabelerConfs: List<LabelerConf>,
    appRecordStore: AppRecordStore
) = remember(appRecordStore) {
    ProjectCreatorState(availableLabelerConfs, appRecordStore)
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    InputFile
}
