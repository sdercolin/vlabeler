package com.sdercolin.vlabeler.ui.starter

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadSavedParams
import com.sdercolin.vlabeler.io.saveParams
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.ParamMap
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
    private val coroutineScope: CoroutineScope,
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
    var templatePluginParams: ParamMap? by mutableStateOf(null)
    var templatePluginError: Boolean by mutableStateOf(false)
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
        updateInputFileIfNeeded()
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

    private fun getSupportedInputFileExtension(): String? {
        val plugin = templatePlugin
        if (plugin != null) {
            return plugin.inputFileExtension
        }
        return labeler.extension
    }

    fun updateLabeler(labeler: LabelerConf) {
        this.labeler = labeler
        if (templatePlugin?.isLabelFileExtensionSupported(labeler.extension) == false) {
            templatePlugin = null
            templatePluginError = false
        }
        updateInputFileIfNeeded()
    }

    fun updatePlugin(plugin: Plugin?) {
        if (plugin != null) {
            coroutineScope.launch {
                templatePlugin = plugin
                updatePluginParams(plugin.loadSavedParams())
                updateInputFileIfNeeded()
            }
        } else {
            templatePluginParams = null
            templatePlugin = null
            templatePluginError = false
            updateInputFileIfNeeded()
        }
    }

    fun updatePluginParams(params: ParamMap?) {
        templatePluginParams = params
        if (params != null) {
            templatePluginError = requireNotNull(templatePlugin).checkParams(params) == false
        }
    }

    fun savePluginParams(plugin: Plugin, params: ParamMap) {
        coroutineScope.launch {
            plugin.saveParams(params)
            updatePluginParams(params)
        }
    }

    private fun updateInputFileIfNeeded() {
        val supportedExtension = getSupportedInputFileExtension()
        if (supportedExtension == null) {
            updateInputFile("", editedByUser = false)
            return
        }
        if (inputFileEdited) return
        if (supportedExtension == labeler.extension) {
            val file = labeler.defaultInputFilePath?.let { File(sampleDirectory).resolve(it) }
            val inputFilePath = if (file?.exists() == true) file.absolutePath else ""
            updateInputFile(inputFilePath, editedByUser = false)
        }
    }

    fun updateInputFile(path: String, editedByUser: Boolean) {
        if (editedByUser) inputFileEdited = true
        if (path == inputFile) return
        coroutineScope.launch(Dispatchers.IO) {
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
            string(Strings.StarterNewInputFileDisabled)
        } else {
            string(Strings.StarterNewInputFile, extension)
        }
    }

    fun getInputFilePlaceholderText(): String? {
        return if (templatePlugin == null) string(Strings.StarterNewInputFilePlaceholder)
        else null
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
        isInputFileValid() && !templatePluginError

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
        name: String?
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
                updateInputFile(file.absolutePath, editedByUser = true)
            }
        }
    }

    fun getSupportedPlugins(plugins: List<Plugin>) = plugins
        .filter { it.type == Plugin.Type.Template }
        .filter { it.isLabelFileExtensionSupported(labeler.extension) }
        .sortedBy { it.displayedName }

    fun create(
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
                pluginParams = templatePluginParams,
                inputFilePath = inputFile,
                encoding = encoding
            ).getOrElse {
                val message = it.message.orEmpty()
                Log.error(it)
                isLoading = false
                snackbarHostState.showSnackbar(message)
                return@launch
            }
            create(project)
            isLoading = false
        }
    }
}

@Composable
fun rememberProjectCreatorState(
    coroutineScope: CoroutineScope,
    availableLabelerConfs: List<LabelerConf>,
    appRecordStore: AppRecordStore
) = remember(appRecordStore) {
    ProjectCreatorState(coroutineScope, availableLabelerConfs, appRecordStore)
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    InputFile
}
