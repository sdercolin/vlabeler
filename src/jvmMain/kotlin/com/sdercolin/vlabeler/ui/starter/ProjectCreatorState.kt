package com.sdercolin.vlabeler.ui.starter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Project.Companion.getDefaultCacheDirectory
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.detectEncoding
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.getDirectory
import com.sdercolin.vlabeler.util.isValidFileName
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files

class ProjectCreatorState(
    private val appState: AppState,
    private val coroutineScope: CoroutineScope,
    private val labelerConfs: List<LabelerConf>,
    private val templatePlugins: List<Plugin>,
    val appRecordStore: AppRecordStore,
    initialFile: File?,
) {

    val appConf get() = appState.appConf
    private val appRecord get() = appRecordStore.value
    var isLoading: Boolean by mutableStateOf(false)

    /* region Page */
    enum class Page(val text: Strings) {
        Directory(Strings.StarterNewDirectoryPage),
        Labeler(Strings.StarterNewLabelerPage),
        DataSource(Strings.StarterNewDataSourcePage),
        ;

        fun next() = values().getOrNull(ordinal + 1)
        fun previous() = values().getOrNull(ordinal - 1)
    }

    var page: Page by mutableStateOf(Page.Directory)

    val hasPrevious get() = page.previous() != null
    val hasNext get() = page.next() != null
    val hasError get() = isValid(page).not()

    fun goPrevious() {
        page.previous()?.let { page = it }
    }

    fun goNext() {
        page.next()?.let { page = it } ?: create()
    }

    private val detailExpandedOnPages = mutableStateListOf(
        *Page.values().map {
            appRecord.projectCreatorDetailsExpanded.getOrNull(it.ordinal) ?: false
        }.toTypedArray(),
    )

    val isDetailExpanded get() = detailExpandedOnPages[page.ordinal]

    fun toggleDetailExpanded() {
        detailExpandedOnPages[page.ordinal] = detailExpandedOnPages[page.ordinal].not()
        appRecordStore.update {
            toggleProjectCreatorDetailsExpanded(page.ordinal)
        }
    }
    /* endregion */

    /* region Directory Page */
    var sampleDirectory: String by mutableStateOf(appRecord.sampleDirectory ?: HomeDir.absolutePath)
        private set
    var projectName: String by mutableStateOf("")
    private var projectNameEdited: Boolean by mutableStateOf(false)

    var workingDirectory: String by mutableStateOf(appRecord.workingDirectory ?: HomeDir.absolutePath)
        private set
    private var workingDirectoryEdited: Boolean by mutableStateOf(false)
    var cacheDirectory: String by mutableStateOf("")
        private set
    private var cacheDirectoryEdited: Boolean by mutableStateOf(false)
    var currentPathPicker: PathPicker? by mutableStateOf(null)
        private set

    init {
        if (initialFile != null) {
            val isProjectFile = initialFile.extension == Project.PROJECT_FILE_EXTENSION
            val directory = if (isProjectFile) initialFile.parentFile else initialFile
            updateSampleDirectory(directory?.absolutePath ?: HomeDir.absolutePath)
            if (isProjectFile) {
                updateProjectName(initialFile.nameWithoutExtension, byUser = false)
            }
        }
    }

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

    fun updateProjectName(name: String, byUser: Boolean = true) {
        if (byUser) {
            projectNameEdited = true
        }
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
        return file.exists() && Files.isReadable(file.toPath())
    }

    fun isProjectNameValid(): Boolean {
        return projectName.isValidFileName()
    }

    fun isWorkingDirectoryValid(): Boolean {
        val file = File(workingDirectory)
        if (file.parentFile?.exists() == false) return false
        return file.name.isValidFileName() && file.isDirectory && Files.isWritable(file.toPath())
    }

    fun isProjectFileExisting(): Boolean {
        return if (isWorkingDirectoryValid() && isProjectNameValid()) {
            File(workingDirectory, "$projectName.${Project.PROJECT_FILE_EXTENSION}").exists()
        } else false
    }

    fun isCacheDirectoryValid(): Boolean {
        val file = File(cacheDirectory)
        val parent = file.parent.orEmpty()
        if (parent != workingDirectory && parent.toFile().exists().not()) return false
        if (Files.isWritable(parent.toFile().toPath()).not()) return false
        if (file.isFile) return false
        return file.name.isValidFileName()
    }
    /* endregion */

    /* region Labeler Page */
    private val builtInCategoryTags = listOf("UTAU", "NNSVS", "LAB")
    val labelerCategories = labelerConfs.asSequence()
        .map { it.categoryTag }
        .distinct()
        .sortedWith { o1, o2 ->
            if (o1 == "") {
                return@sortedWith 1
            }
            if (o2 == "") {
                return@sortedWith -1
            }
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
    var labelerCategory: String by mutableStateOf(
        labelerCategories.firstOrNull { it == appRecord.labelerCategory } ?: labelerCategories.first(),
    )
        private set
    val selectableLabelers: List<LabelerConf>
        get() = labelerConfs
            .filter { it.categoryTag == labelerCategory }
            .sortedWith { o1, o2 ->
                if (o1.displayOrder != o2.displayOrder) {
                    o1.displayOrder.compareTo(o2.displayOrder)
                } else {
                    o1.name.compareTo(o2.name)
                }
            }
    var labeler: LabelerConf by mutableStateOf(
        labelerConfs.firstOrNull { it.name == appRecord.labelerName && it.categoryTag == labelerCategory }
            ?: selectableLabelers.first(),
    )
        private set
    var labelerParams: ParamMap? by mutableStateOf(null)
    var labelerSavedParams: ParamMap? by mutableStateOf(null)
    var labelerError: Boolean by mutableStateOf(false)

    private val selectedLabelerByCategory = mutableMapOf<String, LabelerConf>()

    fun updateLabelerCategory(category: String) {
        labelerCategory = category
        if (labeler.categoryTag != category) {
            val selectedLabeler = selectedLabelerByCategory[category] ?: selectableLabelers.first()
            updateLabeler(selectedLabeler)
        }
    }

    fun updateLabeler(labeler: LabelerConf) {
        this.labeler = labeler
        if (templatePlugin?.isLabelFileExtensionSupported(labeler.extension) == false) {
            updatePlugin(null)
        }
        selectedLabelerByCategory[labelerCategory] = labeler
        coroutineScope.launch {
            val savedParams = labeler.loadSavedParams(labeler.getSavedParamsFile())
            updateLabelerParams(savedParams)
            labelerSavedParams = savedParams
            if (labeler.isSelfConstructed) {
                encoding = getEncodingByLabeler()
                if (contentType !in selectableContentTypes) {
                    contentType = ContentType.Default
                }
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

    fun openWebsite(plugin: BasePlugin) {
        val url = plugin.website.takeIf { it.isNotBlank() } ?: return
        Url.open(url)
    }
    /* endregion */

    /* region Input Page */
    enum class ContentType(val text: Strings) {
        Default(Strings.StarterNewContentTypeDefault),
        File(Strings.StarterNewContentTypeFile),
        Plugin(Strings.StarterNewContentTypePlugin)
    }

    val selectableContentTypes: List<ContentType>
        get() = if (labeler.isSelfConstructed) {
            listOf(ContentType.Default, ContentType.Plugin)
        } else {
            ContentType.values().toList()
        }
    var contentType: ContentType by mutableStateOf(
        appRecord.projectContentType?.takeIf { it in selectableContentTypes }
            ?: ContentType.Default,
    )
        private set

    var templatePlugin: Plugin? by mutableStateOf(null)
    var templatePluginParams: ParamMap? by mutableStateOf(null)
    var templatePluginSavedParams: ParamMap? by mutableStateOf(null)
    var templatePluginError: Boolean by mutableStateOf(false)

    fun selectContentType(contentType: ContentType, language: Language) {
        this.contentType = contentType
        if (contentType == ContentType.Plugin && templatePlugin == null) {
            val plugin = getSupportedPlugins(language).firstOrNull()
            updatePlugin(plugin)
        }
    }

    @Composable
    fun getTemplatePluginName(): String = templatePlugin?.displayedName?.get()
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
            contentType == ContentType.File -> true
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
                    warningText = Strings.StarterNewWarningSelfConstructedLabelerWithTemplatePlugin
                }
            }
        } else {
            templatePlugin = null
            templatePluginParams = null
            templatePluginSavedParams = null
            templatePluginError = false
            warningText = null
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

    fun getSupportedPlugins(language: Language) = templatePlugins
        .asSequence()
        .filter { it.type == Plugin.Type.Template }
        .filter { it.isLabelFileExtensionSupported(labeler.extension) }
        .map { it to it.displayedName.getCertain(language) }
        .sortedBy { it.second }
        .map { it.first }
        .toList()

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

    /* region File Pickers */
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
    /* endregion */

    var warningText: Strings? by mutableStateOf(null)

    private fun isValid(page: Page) = when (page) {
        Page.Directory -> isProjectNameValid() && isSampleDirectoryValid() && isWorkingDirectoryValid() &&
            isCacheDirectoryValid()
        Page.Labeler -> !labelerError
        Page.DataSource -> when (contentType) {
            ContentType.Default -> true
            ContentType.File -> isInputFileValid()
            ContentType.Plugin -> templatePlugin != null && !templatePluginError
        }
    }

    private fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            Log.debug(
                "Create project. sampleDir=$sampleDirectory, " +
                    "workingDir=$workingDirectory, " +
                    "projectName=$projectName, " +
                    "labeler=${labeler.name}, " +
                    "contentType=$contentType, " +
                    "plugin=${templatePlugin?.name?.takeIf { contentType == ContentType.Plugin }}, " +
                    "input=${inputFile.takeIf { contentType == ContentType.File }}, " +
                    "encoding=$encoding",
            )
            appRecordStore.update {
                copy(
                    sampleDirectory = this@ProjectCreatorState.sampleDirectory,
                    workingDirectory = this@ProjectCreatorState.workingDirectory,
                    labelerCategory = this@ProjectCreatorState.labelerCategory,
                    labelerName = this@ProjectCreatorState.labeler.name,
                    projectContentType = this@ProjectCreatorState.contentType,
                )
            }
            val labelerParams = requireNotNull(labelerParams)
            saveLabelerParams(labelerParams)
            templatePluginParams?.let { savePluginParams(it) }

            val useFile = contentType == ContentType.File
            val usePlugin = contentType == ContentType.Plugin

            val project = projectOf(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                cacheDirectory = cacheDirectory,
                rawLabelerConf = labeler,
                labelerParams = labelerParams,
                plugin = templatePlugin?.takeIf { usePlugin },
                pluginParams = templatePluginParams?.takeIf { usePlugin },
                inputFilePath = inputFile.takeIf { useFile },
                encoding = encoding,
                autoExport = autoExport,
            ).getOrElse {
                isLoading = false
                appState.showError(it)
                return@launch
            }
            appState.onCreateProject(project, templatePlugin, templatePluginParams)
            isLoading = false
        }
    }
}

@Composable
fun rememberProjectCreatorState(
    appState: AppState,
    coroutineScope: CoroutineScope,
    activeLabelerConfs: List<LabelerConf>,
    activeTemplatePlugins: List<Plugin>,
    appRecordStore: AppRecordStore,
    initialFile: File? = null,
) = remember(appRecordStore) {
    ProjectCreatorState(
        appState,
        coroutineScope,
        activeLabelerConfs,
        activeTemplatePlugins,
        appRecordStore,
        initialFile,
    )
}

enum class PathPicker {
    SampleDirectory,
    WorkingDirectory,
    CacheDirectory,
    InputFile
}
