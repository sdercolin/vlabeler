package com.sdercolin.vlabeler.ui.starter

import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import com.sdercolin.vlabeler.ui.string.stringStatic
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

class PaginatedProjectCreatorState(
    private val appState: AppState,
    private val coroutineScope: CoroutineScope,
    private val labelerConfs: List<LabelerConf>,
    val appRecordStore: AppRecordStore,
) {
    enum class Page(val text: Strings) {
        Directory(Strings.StarterNewProjectDirectoryPage),
        Labeler(Strings.StarterNewProjectLabelerPage),
        Input(Strings.StarterNewProjectInputPage),
        ;

        fun next() = values().getOrNull(ordinal + 1)
        fun previous() = values().getOrNull(ordinal - 1)
        fun isLast() = next() == null
    }

    val appConf get() = appState.appConf
    private val appRecord get() = appRecordStore.value
    var isLoading: Boolean by mutableStateOf(false)

    var page: Page by mutableStateOf(Page.Directory)
    val detailExpandedOnPages = mutableStateListOf(
        *Page.values().map {
            appRecord.projectCreatorDetailsExpanded.getOrNull(it.ordinal) ?: false
        }.toTypedArray(),
    )

    fun toggleDetailExpanded() {
        detailExpandedOnPages[page.ordinal] = detailExpandedOnPages[page.ordinal].not()
        appRecordStore.update {
            toggleProjectCreatorDetailsExpanded(page.ordinal)
        }
    }

    /* region Directory Page */
    var sampleDirectory: String by mutableStateOf(appRecord.sampleDirectory ?: HomeDir.absolutePath)
        private set
    var projectName: String by mutableStateOf("")
    private var projectNameEdited: Boolean by mutableStateOf(false)
        private set

    var workingDirectory: String by mutableStateOf(appRecord.workingDirectory ?: HomeDir.absolutePath)
        private set
    private var workingDirectoryEdited: Boolean by mutableStateOf(false)
    var cacheDirectory: String by mutableStateOf("")
        private set
    private var cacheDirectoryEdited: Boolean by mutableStateOf(false)
    var currentPathPicker: PathPicker? by mutableStateOf(null)
        private set

    fun updateSampleDirectory(path: String) {
        sampleDirectory = path
        if (!projectNameEdited) {
            fillInProjectNameByDefault(path)
        }
        if (!workingDirectoryEdited) {
            workingDirectory = sampleDirectory
            if (!cacheDirectoryEdited) {
                fillInCacheDirectoryByDefault(path, projectName)
            }
        }
    }

    fun updateProjectName(name: String) {
        projectNameEdited = true
        projectName = name

        if (!cacheDirectoryEdited) {
            fillInCacheDirectoryByDefault(workingDirectory, name)
        }
    }

    private fun fillInProjectNameByDefault(path: String) {
        projectName = if (File(path).absolutePath != HomeDir.absolutePath) path.lastPathSection else ""
    }

    fun updateWorkingDirectory(path: String) {
        workingDirectoryEdited = true
        workingDirectory = path
        if (!cacheDirectoryEdited) {
            fillInCacheDirectoryByDefault(path, projectName)
        }
    }

    private fun fillInCacheDirectoryByDefault(path: String, projectName: String) {
        if (projectName.isEmpty()) return
        cacheDirectory = if (File(path).absolutePath != HomeDir.absolutePath) {
            getDefaultCacheDirectory(path, projectName)
        } else ""
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

    fun isProjectNameValid(): Boolean {
        return projectName.isValidFileName()
    }

    fun isWorkingDirectoryValid(): Boolean {
        val file = File(workingDirectory)
        if (file.parentFile?.exists() == false) return false
        return file.name.isValidFileName()
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
    /* endregion */

    /* region Labeler Page */
    private val builtInCategoryTags = listOf("UTAU", "NNSVS", "LAB")
    val labelerCategories = labelerConfs.asSequence()
        .map { it.categoryTag }
        .distinct()
        .sortedWith { o1, o2 ->
            val o1BuiltInIndex = builtInCategoryTags.indexOf(o1)
            val o2BuiltInIndex = builtInCategoryTags.indexOf(o2)
            if (o1BuiltInIndex != -1 && o2BuiltInIndex != -1) {
                o1BuiltInIndex.compareTo(o2BuiltInIndex)
            } else if (o1BuiltInIndex != -1) {
                -1
            } else if (o2BuiltInIndex != -1) {
                1
            } else {
                o1.compareTo(o2)
            }
        }
        .plus(stringStatic(Strings.CommonOthers)).toList()
    var labelerCategory: String by mutableStateOf(
        labelerCategories.firstOrNull { it == appRecord.labelerCategory } ?: labelerCategories.first(),
    )
        private set
    val selectableLabelers: List<LabelerConf>
        get() = labelerConfs.filter { it.categoryTag == labelerCategory }
    var labeler: LabelerConf by mutableStateOf(
        labelerConfs.firstOrNull { it.name == appRecord.labelerName && it.categoryTag == labelerCategory }
            ?: selectableLabelers.first(),
    )
        private set
    var labelerParams: ParamMap? by mutableStateOf(null)
    var labelerSavedParams: ParamMap? by mutableStateOf(null)
    var labelerError: Boolean by mutableStateOf(false)

    fun updateLabelerCategory(category: String) {
        labelerCategory = category
        if (labeler.categoryTag != category) {
            val labeler = labelerConfs.firstOrNull { it.categoryTag == category }
            if (labeler != null) {
                updateLabeler(labeler)
            }
        }
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
                contentType = ContentType.Default
                updatePlugin(null)
            } else {
                if (inputFileEdited) return@launch
                val file = labeler.defaultInputFilePath?.let { File(sampleDirectory).resolve(it) }
                    ?.takeIf { it.exists() }
                    ?: return@launch
                updateInputFile(file.absolutePath, editedByUser = false, detectEncoding = true)
            }
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
    /* endregion */

    /* region Input Page */
    enum class ContentType(val text: Strings) {
        Default(Strings.StarterNewProjectContentTypeDefault),
        File(Strings.StarterNewProjectContentTypeFile),
        Plugin(Strings.StarterNewProjectContentTypePlugin)
    }

    val selectableContentTypes: List<ContentType>
        get() = if (labeler.isSelfConstructed) {
            listOf(ContentType.Default, ContentType.Plugin)
        } else {
            ContentType.values().toList()
        }
    var contentType: ContentType by mutableStateOf(appRecord.projectContentType ?: ContentType.Default)

    var templatePlugin: Plugin? by mutableStateOf(null)
    var templatePluginParams: ParamMap? by mutableStateOf(null)
    var templatePluginSavedParams: ParamMap? by mutableStateOf(null)
    var templatePluginError: Boolean by mutableStateOf(false)

    var templatePluginWarningText: Strings? by mutableStateOf(null)

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

    fun updatePlugin(plugin: Plugin?) {
        if (plugin != null) {
            coroutineScope.launch {
                templatePlugin = plugin
                val savedParams = plugin.loadSavedParams(plugin.getSavedParamsFile())
                updatePluginParams(savedParams)
                templatePluginSavedParams = savedParams
                if (labeler.isSelfConstructed) {
                    templatePluginWarningText = Strings.StarterNewWarningSelfConstructedLabelerWithTemplatePlugin
                }
            }
        } else {
            templatePlugin = null
            templatePluginParams = null
            templatePluginSavedParams = null
            templatePluginError = false
            templatePluginWarningText = null
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

    fun updateInputFile(path: String?, editedByUser: Boolean, detectEncoding: Boolean = true) {
        if (editedByUser) inputFileEdited = true
        if (path == inputFile) return
        if (path == null) {
            contentType = ContentType.Default
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            inputFile = path
            val file = File(path)
            if (file.isFile && file.exists() && detectEncoding) {
                val detectedEncoding = file.readBytes().detectEncoding() ?: return@launch
                encoding = encodings.find { encodingNameEquals(detectedEncoding, it) } ?: detectedEncoding
            }
        }
    }

    fun isInputFileValid(): Boolean {
        if (inputFile == "") {
            return false
        }
        val file = File(inputFile)
        return file.extension == labeler.extension && file.exists()
    }

    fun toggleAutoExport(enabled: Boolean) {
        autoExport = enabled
        appRecordStore.update { copy(autoExport = enabled) }
    }
    /* endregion */

    fun isValid(page: PaginatedProjectCreatorState.Page) = when (page) {
        Page.Directory -> isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid() &&
            isCacheDirectoryValid()
        Page.Labeler -> !labelerError
        Page.Input -> when (contentType) {
            ContentType.Default -> true
            ContentType.File -> isInputFileValid()
            ContentType.Plugin -> !templatePluginError
        }
    }

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

    fun getFilePickerDirectoryMode(picker: PathPicker) =
        picker != PathPicker.InputFile

    fun getFilePickerExtensions(
        picker: PathPicker,
    ) = when (picker) {
        PathPicker.SampleDirectory -> Sample.acceptableSampleFileExtensions
        PathPicker.WorkingDirectory -> null
        PathPicker.CacheDirectory -> null
        PathPicker.InputFile -> listOf(labeler.extension)
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
                    sampleDirectory = this@PaginatedProjectCreatorState.sampleDirectory,
                    workingDirectory = this@PaginatedProjectCreatorState.workingDirectory,
                    labelerCategory = this@PaginatedProjectCreatorState.labelerCategory,
                    labelerName = this@PaginatedProjectCreatorState.labeler.name,
                    projectContentType = this@PaginatedProjectCreatorState.contentType,
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
