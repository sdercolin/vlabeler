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
import com.sdercolin.vlabeler.model.ArgumentMap
import com.sdercolin.vlabeler.model.Arguments
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Project.Companion.getDefaultCacheDirectory
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
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
import com.sdercolin.vlabeler.util.resolveHome
import com.sdercolin.vlabeler.util.toFileOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ProjectCreatorState(
    private val appState: AppState,
    private val coroutineScope: CoroutineScope,
    private val labelerConfs: List<LabelerConf>,
    val appRecordStore: AppRecordStore,
    private var launchArguments: ArgumentMap?,
) {
    val appConf get() = appState.appConf
    private val appRecord get() = appRecordStore.value
    var isLoading: Boolean by mutableStateOf(false)
    var sampleDirectory: String by mutableStateOf(appRecord.sampleDirectory ?: HomeDir.absolutePath)
        private set
    var workingDirectory: String by mutableStateOf(appRecord.workingDirectory ?: HomeDir.absolutePath)
        private set
    private var workingDirectoryEdited: Boolean by mutableStateOf(false)
    var projectName: String by mutableStateOf("")
        private set
    private var projectNameEdited: Boolean by mutableStateOf(false)
    var cacheDirectory: String by mutableStateOf("")
        private set
    private var cacheDirectoryEdited: Boolean by mutableStateOf(false)
    var currentPathPicker: PathPicker? by mutableStateOf(null)
        private set
    var labeler: LabelerConf by mutableStateOf(
        labelerConfs.firstOrNull { it.name == appRecord.labelerName } ?: labelerConfs.first(),
    )
        private set
    var templatePlugin: Plugin? by mutableStateOf(null)
    var templatePluginParams: ParamMap? by mutableStateOf(null)
    var templatePluginSavedParams: ParamMap? by mutableStateOf(null)
    var templatePluginError: Boolean by mutableStateOf(false)
    val templateName: String get() = templatePlugin?.displayedName ?: string(Strings.StarterNewTemplatePluginNone)
    var inputFile: String by mutableStateOf("")
        private set
    private var inputFileEdited: Boolean by mutableStateOf(false)
    val encodings = AvailableEncodings
    var autoExport: Boolean by mutableStateOf(appRecord.autoExport)
        private set

    private val encodingState = mutableStateOf(
        run {
            val parser = labeler.parser
            val encodingName = encodings.find { encodingNameEquals(parser.defaultEncoding, it) }
                ?: encodings.first().takeIf { it.isNotBlank() }
                ?: encodings.first()
            encodingName
        },
    )
    var encoding: String by encodingState

    fun consumeLaunchArguments() {
        val args = launchArguments ?: return
        launchArguments = null
        val projectFile = args[Arguments.OpenOrCreate]
            ?.toFileOrNull(allowHomePlaceholder = true, ensureExists = false)
            ?: return
        workingDirectory = projectFile.parent
        workingDirectoryEdited = true
        projectName = projectFile.nameWithoutExtension
        projectNameEdited = true
        sampleDirectory = args[Arguments.SampleDirectory]?.resolveHome() ?: workingDirectory
        cacheDirectory = args[Arguments.CacheDirectory]?.resolveHome()
            ?.also { cacheDirectoryEdited = true }
            ?: getDefaultCacheDirectory(workingDirectory, projectName)
        val encodingText = args[Arguments.Encoding]
        val encoding =
            if (encodingText != null) AvailableEncodings.find { charset(encodingText) == charset(it) } else null
        if (encoding != null) {
            this.encoding = encoding
        }
        val labelerName = args[Arguments.LabelerName]
        val labeler = labelerConfs.find { it.name == labelerName }
        if (labeler != null) {
            this.labeler = labeler
            val inputFile = args[Arguments.InputFile]?.toFileOrNull(allowHomePlaceholder = true, ensureIsFile = true)
            if (inputFile != null && inputFile.extension == labeler.extension) {
                this.inputFile = inputFile.absolutePath
                inputFileEdited = true
            } else {
                updateInputFileIfNeeded(detectEncoding = encoding == null)
            }
        }
        args[Arguments.AutoExport]?.toBooleanStrictOrNull()?.let { autoExport = it }
    }

    fun updateSampleDirectory(path: String) {
        sampleDirectory = path
        if (!workingDirectoryEdited) {
            workingDirectory = sampleDirectory
            if (!projectNameEdited) {
                fillInProjectNameByDefault(path)
            }
            if (!cacheDirectoryEdited) {
                fillInCacheDirectoryByDefault(path, projectName)
            }
        }
        updateInputFileIfNeeded()
    }

    fun updateWorkingDirectory(path: String) {
        workingDirectoryEdited = true
        workingDirectory = path
        if (!projectNameEdited) {
            fillInProjectNameByDefault(path)
        }
        if (!cacheDirectoryEdited) {
            fillInCacheDirectoryByDefault(path, projectName)
        }
    }

    private fun fillInProjectNameByDefault(path: String) {
        projectName = if (File(path).absolutePath != HomeDir.absolutePath) path.lastPathSection else ""
    }

    private fun fillInCacheDirectoryByDefault(path: String, projectName: String) {
        if (projectName.isEmpty()) return
        cacheDirectory = if (File(path).absolutePath != HomeDir.absolutePath) {
            getDefaultCacheDirectory(path, projectName)
        } else ""
    }

    fun updateProjectName(name: String) {
        projectNameEdited = true
        projectName = name

        if (!cacheDirectoryEdited) {
            fillInCacheDirectoryByDefault(workingDirectory, name)
        }
    }

    fun updateCacheDirectory(path: String) {
        cacheDirectoryEdited = true
        cacheDirectory = path
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

    fun isCacheDirectoryValid(): Boolean {
        val file = File(cacheDirectory)
        val parent = file.parent.orEmpty()
        if (parent != workingDirectory && File(parent).exists().not()) return false
        return file.name.isValidFileName()
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
                val savedParams = plugin.loadSavedParams()
                updatePluginParams(savedParams)
                templatePluginSavedParams = savedParams
                updateInputFileIfNeeded()
            }
        } else {
            templatePlugin = null
            templatePluginParams = null
            templatePluginSavedParams = null
            templatePluginError = false
            updateInputFileIfNeeded()
        }
    }

    fun updatePluginParams(params: ParamMap?) {
        templatePluginParams = params
        if (params != null) {
            templatePluginError = requireNotNull(templatePlugin).checkParams(params, null) == false
        }
    }

    fun savePluginParams(params: ParamMap) {
        coroutineScope.launch {
            templatePlugin?.saveParams(params)
            templatePluginSavedParams = params
            updatePluginParams(params)
        }
    }

    private fun updateInputFileIfNeeded(detectEncoding: Boolean = true) {
        val supportedExtension = getSupportedInputFileExtension()
        if (supportedExtension == null) {
            updateInputFile("", editedByUser = false)
            return
        }
        if (inputFileEdited) return
        if (supportedExtension == labeler.extension) {
            val file = labeler.defaultInputFilePath?.let { File(sampleDirectory).resolve(it) }
            val inputFilePath = if (file?.exists() == true) file.absolutePath else ""
            updateInputFile(inputFilePath, editedByUser = false, detectEncoding = detectEncoding)
        }
    }

    fun updateInputFile(path: String, editedByUser: Boolean, detectEncoding: Boolean = true) {
        if (editedByUser) inputFileEdited = true
        if (path == inputFile) return
        coroutineScope.launch(Dispatchers.IO) {
            inputFile = path
            val file = File(path)
            if (file.isFile && file.exists() && detectEncoding) {
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

    fun toggleAutoExport(enabled: Boolean) {
        autoExport = enabled
        appRecordStore.update { copy(autoExport = enabled) }
    }

    fun isValid(): Boolean = isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid() &&
        isCacheDirectoryValid() && isInputFileValid() && !templatePluginError

    fun pickSampleDirectory() {
        currentPathPicker = PathPicker.SampleDirectory
    }

    fun pickWorkingDirectory() {
        currentPathPicker = PathPicker.WorkingDirectory
    }

    fun pickCacheDirectory() {
        currentPathPicker = PathPicker.CacheDirectory
    }

    fun pickInputFile() {
        currentPathPicker = PathPicker.InputFile
    }

    val isEncodingSelectionEnabled get() = inputFile != ""

    fun getFilePickerDirectoryMode(picker: PathPicker) =
        picker != PathPicker.InputFile

    fun getFilePickerExtensions(
        picker: PathPicker,
    ) = when (picker) {
        PathPicker.SampleDirectory -> listOf(Project.SampleFileExtension)
        PathPicker.WorkingDirectory -> null
        PathPicker.CacheDirectory -> null
        PathPicker.InputFile -> getSupportedInputFileExtension()?.let { listOf(it) }
    }

    fun getFilePickerInitialDirectory(
        picker: PathPicker,
    ) = when (picker) {
        PathPicker.SampleDirectory -> sampleDirectory
        PathPicker.WorkingDirectory -> workingDirectory
        PathPicker.CacheDirectory -> cacheDirectory
        PathPicker.InputFile -> if (inputFile != "" && isInputFileValid()) {
            File(inputFile).parent.orEmpty()
        } else {
            sampleDirectory
        }
    }

    fun getFilePickerTitle(picker: PathPicker) = when (picker) {
        PathPicker.SampleDirectory -> string(Strings.ChooseSampleDirectoryDialogTitle)
        PathPicker.WorkingDirectory -> string(Strings.ChooseWorkingDirectoryDialogTitle)
        PathPicker.CacheDirectory -> string(Strings.ChooseCacheDirectoryDialogTitle)
        PathPicker.InputFile -> string(Strings.ChooseInputFileDialogTitle)
    }

    fun handleFilePickerResult(
        picker: PathPicker,
        parent: String?,
        name: String?,
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
            PathPicker.CacheDirectory -> {
                updateCacheDirectory(file.getDirectory().absolutePath)
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
        create: (Project) -> Unit,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            Log.debug(
                "Create project. sampleDir=$sampleDirectory, " +
                    "workingDir=$workingDirectory, " +
                    "projectName=$projectName, " +
                    "labeler=${labeler.name}, " +
                    "input=$inputFile, " +
                    "encoding=$encoding",
            )
            appRecordStore.update {
                copy(
                    sampleDirectory = this@ProjectCreatorState.sampleDirectory,
                    workingDirectory = this@ProjectCreatorState.workingDirectory,
                    labelerName = this@ProjectCreatorState.labeler.name,
                )
            }
            val project = projectOf(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                cacheDirectory = cacheDirectory,
                labelerConf = labeler,
                plugin = templatePlugin,
                pluginParams = templatePluginParams,
                inputFilePath = inputFile,
                encoding = encoding,
                autoExport = autoExport,
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
    appState: AppState,
    coroutineScope: CoroutineScope,
    activeLabelerConfs: List<LabelerConf>,
    appRecordStore: AppRecordStore,
    launchArguments: ArgumentMap?,
) = remember(appRecordStore) {
    ProjectCreatorState(appState, coroutineScope, activeLabelerConfs, appRecordStore, launchArguments)
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    CacheDirectory,
    InputFile
}
