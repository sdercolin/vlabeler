package com.sdercolin.vlabeler.ui.starter

import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Project.Companion.getDefaultCacheDirectory
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.currentLanguage
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.detectEncoding
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.getDirectory
import com.sdercolin.vlabeler.util.getLocalizedMessage
import com.sdercolin.vlabeler.util.isValidFileName
import com.sdercolin.vlabeler.util.lastPathSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ProjectCreatorState(
    private val appState: AppState,
    private val coroutineScope: CoroutineScope,
    labelerConfs: List<LabelerConf>,
    val appRecordStore: AppRecordStore,
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
    var labelerParams: ParamMap? by mutableStateOf(null)
    var labelerSavedParams: ParamMap? by mutableStateOf(null)
    var labelerError: Boolean by mutableStateOf(false)

    var templatePlugin: Plugin? by mutableStateOf(null)
    var templatePluginParams: ParamMap? by mutableStateOf(null)
    var templatePluginSavedParams: ParamMap? by mutableStateOf(null)
    var templatePluginError: Boolean by mutableStateOf(false)

    var warningText: Strings? by mutableStateOf(null)

    @Composable
    fun getTemplateName(): String = templatePlugin?.displayedName?.get()
        ?: string(Strings.StarterNewTemplatePluginNone)

    var inputFile: String by mutableStateOf("")
        private set
    private var inputFileEdited: Boolean by mutableStateOf(false)
    val encodings = AvailableEncodings
    var autoExport: Boolean by mutableStateOf(appRecord.autoExport)
        private set

    val canAutoExport: Boolean
        get() = when {
            labeler.isSelfConstructed -> true
            inputFile.isNotEmpty() && templatePlugin == null -> true
            labeler.defaultInputFilePath != null -> true
            else -> false
        }

    var encoding: String by mutableStateOf(getEncodingByLabeler())

    private fun getEncodingByLabeler(): String {
        val parser = labeler.parser
        return encodings.find { encodingNameEquals(parser.defaultEncoding, it) }
            ?: encodings.first().takeIf { it.isNotBlank() }
            ?: encodings.first()
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
            if (labeler.isSelfConstructed && plugin.inputFinderScriptFile != null) {
                return null
            }
            return plugin.inputFileExtension
        }
        if (labeler.isSelfConstructed) return null
        return labeler.extension
    }

    fun updateLabeler(labeler: LabelerConf) {
        this.labeler = labeler
        if (templatePlugin?.isLabelFileExtensionSupported(labeler.extension) == false) {
            templatePlugin = null
            templatePluginError = false
        }
        coroutineScope.launch {
            val savedParams = labeler.loadSavedParams(labeler.getSavedParamsFile())
            updateLabelerParams(savedParams)
            labelerSavedParams = savedParams
            if (labeler.isSelfConstructed) {
                encoding = getEncodingByLabeler()
                updatePlugin(null)
            }
            updateInputFileIfNeeded()
        }
    }

    fun updateLabelerParams(params: ParamMap?) {
        labelerParams = params
        if (params != null) {
            labelerError = labeler.checkParams(params, null) == false
        }
    }

    fun saveLabelerParams(params: ParamMap) {
        coroutineScope.launch {
            labeler.saveParams(params, labeler.getSavedParamsFile())
            labelerSavedParams = params
            updateLabelerParams(params)
        }
    }

    fun updatePlugin(plugin: Plugin?) {
        if (plugin != null) {
            coroutineScope.launch {
                templatePlugin = plugin
                val savedParams = plugin.loadSavedParams(plugin.getSavedParamsFile())
                updatePluginParams(savedParams)
                templatePluginSavedParams = savedParams
                updateInputFileIfNeeded()
                if (labeler.isSelfConstructed) {
                    warningText = Strings.StarterNewWarningSelfConstructedLabelerWithTemplatePlugin
                }
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
            templatePlugin?.run {
                saveParams(params, getSavedParamsFile())
            }
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

    @Composable
    fun getInputFileLabelText(): String {
        val plugin = templatePlugin
        val extension = getSupportedInputFileExtension()
        if (plugin != null) {
            return when {
                labeler.isSelfConstructed && plugin.inputFinderScriptFile != null ->
                    string(Strings.StarterNewInputFileDisabledByLabeler)
                extension == null -> string(Strings.StarterNewInputFileDisabledByPlugin)
                else -> string(Strings.StarterNewInputFile, extension)
            }
        }
        return if (extension == null) {
            string(Strings.StarterNewInputFileDisabledByLabeler)
        } else {
            string(Strings.StarterNewInputFile, extension)
        }
    }

    @Composable
    fun getInputFilePlaceholderText(): String? {
        return if (templatePlugin == null) string(Strings.StarterNewInputFilePlaceholder)
        else null
    }

    fun isInputFileValid(): Boolean {
        if (isInputFileEnabled().not()) return true
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

    val isEncodingSelectionEnabled: Boolean
        get() = getSupportedInputFileExtension() != null || labeler.isSelfConstructed

    fun getFilePickerDirectoryMode(picker: PathPicker) =
        picker != PathPicker.InputFile

    fun getFilePickerExtensions(
        picker: PathPicker,
    ) = when (picker) {
        PathPicker.SampleDirectory -> Sample.acceptableSampleFileExtensions
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

    @Composable
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

    suspend fun showSnackBar(message: String) {
        appState.showSnackbar(message)
    }

    @Composable
    fun getSupportedPlugins(plugins: List<Plugin>) = plugins
        .filter { it.type == Plugin.Type.Template }
        .filter { it.isLabelFileExtensionSupported(labeler.extension) }
        .map { it to it.displayedName.get() }
        .sortedBy { it.second }
        .map { it.first }

    fun interface OnCreateListener {
        fun onCreate(project: Project, plugin: Plugin?, pluginParams: ParamMap?)
    }

    fun create(listener: OnCreateListener) {
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
            val labelerParams = requireNotNull(labelerParams)
            saveLabelerParams(labelerParams)
            templatePluginParams?.let { savePluginParams(it) }
            val project = projectOf(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                cacheDirectory = cacheDirectory,
                labelerConf = labeler,
                labelerParams = labelerParams,
                plugin = templatePlugin,
                pluginParams = templatePluginParams,
                inputFilePath = inputFile,
                encoding = encoding,
                autoExport = autoExport,
            ).getOrElse {
                val message = it.getLocalizedMessage(currentLanguage)
                Log.error(it)
                isLoading = false
                appState.showSnackbar(message, duration = SnackbarDuration.Indefinite)
                return@launch
            }
            listener.onCreate(project, templatePlugin, templatePluginParams)
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
) = remember(appRecordStore) {
    ProjectCreatorState(appState, coroutineScope, activeLabelerConfs, appRecordStore)
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    CacheDirectory,
    InputFile
}
