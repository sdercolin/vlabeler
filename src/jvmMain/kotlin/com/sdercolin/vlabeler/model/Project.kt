package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.InvalidCreatedProjectException
import com.sdercolin.vlabeler.exception.InvalidEditedProjectException
import com.sdercolin.vlabeler.exception.ProjectConstructorRuntimeException
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.io.moduleFromRawLabels
import com.sdercolin.vlabeler.io.moduleGroupFromRawLabels
import com.sdercolin.vlabeler.model.Project.Companion.PROJECT_VERSION
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.util.DefaultEncoding
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.containsFileRecursively
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.readTextByEncoding
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

/**
 * The project object.
 *
 * @property version The version of the project file. Should be [PROJECT_VERSION] when created.
 * @property rootSampleDirectoryPath The directory where all sample files are stored. Should always be an absolute path.
 * @property workingDirectoryPath The directory where the project file is stored. Can be relative to
 *     [rootSampleDirectory].
 * @property projectName The name of the project. Should be the same as the project file name without extension.
 * @property cacheDirectoryPath The directory where all cache files are stored. Can be relative to
 *     [rootSampleDirectory].
 * @property originalLabelerConf The original [LabelerConf] instance stored in the project file.
 * @property labelerConf The injected [LabelerConf] instance with [labelerParams].
 * @property labelerParams The parameters of the labeler.
 * @property encoding The encoding of the project file.
 * @property multipleEditMode Whether the multiple edit mode is enabled.
 * @property modules The modules in the project, which contains all actual content.
 * @property currentModuleIndex The index of the current module.
 * @property autoExport Whether to export the project automatically when the project is saved.
 */
@Serializable
@Immutable
data class Project(
    val version: Int = 0,
    @SerialName("rootSampleDirectory")
    val rootSampleDirectoryPath: String,
    @SerialName("workingDirectory")
    val workingDirectoryPath: String,
    val projectName: String,
    @SerialName("cacheDirectory")
    val cacheDirectoryPath: String,
    @SerialName("labelerConf")
    val originalLabelerConf: LabelerConf,
    @Transient
    val labelerConf: LabelerConf = originalLabelerConf,
    val labelerParams: ParamTypedMap? = null,
    val encoding: String = DefaultEncoding,
    val multipleEditMode: Boolean = labelerConf.continuous,
    val modules: List<Module>,
    val currentModuleIndex: Int,
    val autoExport: Boolean,
) {
    val rootSampleDirectory: File
        get() = rootSampleDirectoryPath.toFile()

    val workingDirectory: File
        get() = rootSampleDirectory.resolve(workingDirectoryPath)

    val projectFile: File
        get() = workingDirectory.resolve("$projectName.$PROJECT_FILE_EXTENSION")

    val cacheDirectory: File
        get() = rootSampleDirectory.resolve(cacheDirectoryPath)

    val isUsingDefaultCacheDirectory: Boolean
        get() = cacheDirectory.absolutePath == getDefaultCacheDirectory(
            workingDirectory.absolutePath,
            projectName,
        )

    val currentModule: Module
        get() = modules[currentModuleIndex]

    val currentEntry: Entry
        get() = currentModule.currentEntry

    val currentSampleName: String
        get() = currentEntry.sample

    val currentSampleFile: File
        get() = currentModule.getCurrentSampleFile(this)

    val isMultiModule: Boolean
        get() = modules.size > 1

    private fun getModule(name: String) = modules.first { it.name == name }

    fun updateModule(name: String, updater: Module.() -> Module): Project {
        val module = getModule(name)
        val index = modules.indexOf(module)
        return copy(modules = modules.toMutableList().apply { this[index] = module.updater() })
    }

    fun updateCurrentModule(updater: Module.() -> Module): Project {
        return updateModule(currentModule.name, updater)
    }

    fun updateEntryFilter(entryFilter: EntryFilter?): Project {
        return copy(modules = modules.map { it.copy(entryFilter = entryFilter) })
    }

    fun getEntriesForEditing(index: Int? = null) =
        currentModule.name to currentModule.getEntriesForEditing(index, multipleEditMode)

    fun hasSwitchedSample(previous: Project?): Boolean {
        if (previous?.currentModuleIndex != currentModuleIndex) return true
        return currentModule.hasSwitchedSample(previous.currentModule)
    }

    fun validate() = this.run {
        require(rootSampleDirectory.isAbsolute) {
            "rootSampleDirectory must not be relative."
        }
        require(modules.isNotEmpty()) { "No module found." }
        require(currentModuleIndex in modules.indices) { "Invalid current module index." }
        require(modules.distinctBy { it.name }.size == modules.size) { "Module names cannot be duplicated." }

        modules.forEach { module ->
            require(rootSampleDirectory.containsFileRecursively(module.getSampleDirectory(this))) {
                "Module[${module.name}]: sampleDirectory must be under the root sample directory."
            }
            val rawFile = module.getRawFile(this)
            if (rawFile != null) {
                require(rootSampleDirectory.containsFileRecursively(rawFile)) {
                    "Module[${module.name}]: rawFilePath must be under the root sample directory."
                }
            }
        }
        val modules = modules.map { it.validate(multipleEditMode, labelerConf) }
        copy(modules = modules)
    }

    fun makeRelativePathsIfPossible(): Project {
        fun File.makeRelativeIfPossible(base: File): File = if (base.containsFileRecursively(this)) {
            relativeTo(base)
        } else {
            this
        }

        val fixedWorkingDirectory = workingDirectory.makeRelativeIfPossible(rootSampleDirectory).path
        val fixedCacheDirectory = cacheDirectory.makeRelativeIfPossible(rootSampleDirectory).path

        val fixedModules = modules.map { module ->
            module.copy(
                sampleDirectoryPath = module.sampleDirectoryPath.toFile()
                    .makeRelativeIfPossible(rootSampleDirectory).path,
                rawFilePath = module.rawFilePath?.toFile()
                    ?.makeRelativeIfPossible(rootSampleDirectory)?.path,
            )
        }

        return copy(
            workingDirectoryPath = fixedWorkingDirectory,
            cacheDirectoryPath = fixedCacheDirectory,
            modules = fixedModules,
        )
    }

    companion object {
        const val PROJECT_VERSION = 3

        const val PROJECT_FILE_EXTENSION = "lbp"
        private const val DEFAULT_CACHE_DIRECTORY_SUFFIX = ".$PROJECT_FILE_EXTENSION.caches"

        fun getDefaultCacheDirectory(location: String, projectName: String): String {
            return File(location, "$projectName$DEFAULT_CACHE_DIRECTORY_SUFFIX").absolutePath
        }
    }
}

private fun generateEntriesByPlugin(
    rootSampleDirectory: String,
    moduleDefinition: ModuleDefinition,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    sampleFiles: List<File>,
    plugin: Plugin,
    params: ParamMap,
    encoding: String,
): Result<List<Entry>> = runCatching {
    when (
        val result = runTemplatePlugin(
            plugin = plugin,
            params = params,
            encoding = encoding,
            sampleFiles = sampleFiles,
            labelerConf = labelerConf,
            labelerParams = labelerParams,
            rootSampleDirectory = rootSampleDirectory,
            moduleDefinition = moduleDefinition,
        )
    ) {
        is TemplatePluginResult.Parsed -> {
            val entries = result.entries.map {
                it.copy(
                    points = it.points.take(labelerConf.fields.count()),
                    extras = it.extras.take(labelerConf.extraFields.count()),
                )
            }

            entries.postApplyLabelerConf(labelerConf)
        }
        is TemplatePluginResult.Raw -> moduleFromRawLabels(
            sources = result.lines,
            inputFile = null,
            labelerConf = labelerConf,
            labelerParams = labelerParams,
            sampleFiles = sampleFiles,
            encoding = encoding,
        )
    }
}

fun LabelerConf.injectLabelerParams(paramMap: ParamMap): LabelerConf {
    val paramDefsToInject = paramMap
        .mapNotNull { (key, value) ->
            val param = parameters.find { it.parameter.name == key }
            if (param == null) {
                null
            } else {
                param to value
            }
        }
        .filter { it.second != it.first.parameter.defaultValue }
        .map { it.first }
        .filter { it.injector != null }
    if (paramDefsToInject.isEmpty()) return this

    val js = JavaScript()
    js.setJson("labeler", this)
    try {
        for (def in paramDefsToInject) {
            js.setJson("value", paramMap.resolveItem(def.parameter.name, project = null, js = js))
            def.injector?.getScripts(directory)?.let { js.eval(it) }
        }
    } catch (t: Throwable) {
        throw InvalidEditedProjectException(t)
    }
    val labelerResult = js.getJson<LabelerConf>("labeler").migrate()
    js.close()

    listOf(
        "name",
        "version",
        "extension",
        "displayedName",
        "description",
        "author",
        "website",
        "email",
        "continuous",
        "parameters",
    ).forEach {
        val errorMessage = "Could not inject to change a basic field of LabelerConf: $it"
        when (it) {
            "name" -> require(labelerResult.name == name) { errorMessage }
            "version" -> require(labelerResult.version == version) { errorMessage }
            "extension" -> require(labelerResult.extension == this.extension) { errorMessage }
            "displayedName" -> require(labelerResult.displayedName == displayedName) { errorMessage }
            "description" -> require(labelerResult.description == description) { errorMessage }
            "author" -> require(labelerResult.author == author) { errorMessage }
            "website" -> require(labelerResult.website == website) { errorMessage }
            "email" -> require(labelerResult.email == email) { errorMessage }
            "continuous" -> require(labelerResult.continuous == continuous) { errorMessage }
            "parameters" ->
                require(labelerResult.parameters.stringifyJson() == parameters.stringifyJson()) { errorMessage }
        }
    }
    listOf(
        "defaultValues",
        "fields",
        "extraFields",
    ).forEach {
        val errorMessage = "Could not inject to change the size of a basic array of LabelerConf: $it"
        when (it) {
            "defaultValues" -> require(labelerResult.defaultValues.size == defaultValues.size) { errorMessage }
            "fields" -> require(labelerResult.fields.size == fields.size) { errorMessage }
            "extraFields" ->
                require(labelerResult.extraFields.size == extraFields.size) { errorMessage }
        }
    }
    require(labelerResult.fields.map { it.name } == fields.map { it.name }) {
        "Could not inject to change the name of a field of LabelerConf"
    }
    require(labelerResult.extraFields.map { it.name } == extraFields.map { it.name }) {
        "Could not inject to change the name of an extra field of LabelerConf"
    }
    require(labelerResult.properties.map { it.name } == properties.map { it.name }) {
        "Could not inject to change the name of a property of LabelerConf"
    }
    return labelerResult
}

/**
 * Should be called from IO threads, because this function runs scripting and may take time.
 */
@Suppress("RedundantSuspendModifier")
suspend fun projectOf(
    sampleDirectory: String,
    workingDirectory: String,
    projectName: String,
    cacheDirectory: String,
    rawLabelerConf: LabelerConf,
    labelerParams: ParamMap,
    plugin: Plugin?,
    pluginParams: ParamMap?,
    inputFilePath: String?,
    encoding: String,
    autoExport: Boolean,
): Result<Project> = runCatching {
    val labelerTypedParams = labelerParams.let { ParamTypedMap.from(it, rawLabelerConf.parameterDefs) }
    val labelerConf = labelerParams.let { rawLabelerConf.injectLabelerParams(it) }
    val moduleDefinitions = if (labelerConf.projectConstructor != null) {
        val js = JavaScript()
        js.set("debug", isDebug)
        js.set("root", sampleDirectory.toFile())
        js.set("encoding", encoding)
        js.setJson("acceptedSampleExtensions", Sample.acceptableSampleFileExtensions)
        js.setJson("resources", labelerConf.readResourceFiles())
        listOf(
            Resources.envJs,
            Resources.fileJs,
            Resources.expectedErrorJs,
            Resources.moduleDefinitionJs,
        ).forEach { js.execResource(it) }
        js.eval("root = new File(root)")
        labelerParams.resolve(project = null, js = js).let { js.setJson("params", it) }
        runCatching {
            labelerConf.projectConstructor.scripts.getScripts(labelerConf.directory).let { js.eval(it) }
        }.onFailure { t ->
            val expected = js.getOrNull("expectedError") ?: false
            js.close()
            return if (expected) {
                Result.failure(ProjectConstructorRuntimeException(t, t.message?.parseJson()))
            } else {
                Result.failure(InvalidCreatedProjectException(t))
            }
        }
        val modules = js.getJson<List<RawModuleDefinition>>("modules")
        js.close()
        modules.map { it.toModuleDefinition() }
    } else {
        val sampleDirectoryFile = File(sampleDirectory)
        val sampleFiles = Sample.listSampleFiles(sampleDirectoryFile)
        val inputFile = inputFilePath?.ifEmpty { null }?.toFile()
        listOf(
            ModuleDefinition(
                name = "",
                sampleDirectory = sampleDirectoryFile,
                sampleFiles = sampleFiles,
                inputFiles = listOfNotNull(inputFile),
                labelFile = inputFile.takeIf { plugin == null }
                    ?: labelerConf.defaultInputFilePath?.let { sampleDirectoryFile.resolve(it) },
            ),
        )
    }

    val modules =
        parseModule(moduleDefinitions, plugin, sampleDirectory, labelerConf, labelerParams, pluginParams, encoding)

    require(modules.isNotEmpty()) {
        "No entries were found for any module"
    }
    Project(
        version = PROJECT_VERSION,
        rootSampleDirectoryPath = sampleDirectory,
        workingDirectoryPath = workingDirectory,
        projectName = projectName,
        cacheDirectoryPath = cacheDirectory,
        labelerConf = labelerConf,
        originalLabelerConf = rawLabelerConf,
        labelerParams = labelerTypedParams,
        encoding = encoding,
        modules = modules,
        currentModuleIndex = 0,
        autoExport = autoExport,
    ).validate().makeRelativePathsIfPossible()
}.onFailure {
    return Result.failure(InvalidCreatedProjectException(it))
}

private fun parseModule(
    moduleDefinitions: List<ModuleDefinition>,
    plugin: Plugin?,
    sampleDirectory: String,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    pluginParams: ParamMap?,
    encoding: String,
): List<Module> {
    if (labelerConf.parser.scope != LabelerConf.Scope.Modules) {
        return parseSingleModule(
            moduleDefinitions,
            plugin,
            sampleDirectory,
            labelerConf,
            labelerParams,
            pluginParams,
            encoding,
        )
    }

    val moduleGroups = moduleDefinitions.groupBy { it.copy(name = "") }.map { it.value }

    return moduleGroups.flatMap { group ->
        parseModuleGroup(sampleDirectory, group, labelerConf, labelerParams, encoding)
    }
}

private fun parseSingleModule(
    moduleDefinitions: List<ModuleDefinition>,
    plugin: Plugin?,
    sampleDirectory: String,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    pluginParams: ParamMap?,
    encoding: String,
) = moduleDefinitions.mapNotNull { def ->
    val existingSingleInputFile = def.inputFiles?.firstOrNull { it.exists() }
    val entries = when {
        plugin != null -> {
            generateEntriesByPlugin(
                rootSampleDirectory = sampleDirectory,
                moduleDefinition = def,
                labelerConf = labelerConf,
                labelerParams = labelerParams,
                sampleFiles = def.sampleFiles,
                plugin = plugin,
                params = requireNotNull(pluginParams),
                encoding = encoding,
            ).getOrThrow()
        }
        existingSingleInputFile != null -> {
            moduleFromRawLabels(
                existingSingleInputFile.readTextByEncoding(encoding).lines(),
                existingSingleInputFile,
                labelerConf,
                labelerParams,
                def.sampleFiles,
                encoding = encoding,
            )
        }
        else -> {
            def.sampleFiles.map {
                Entry.fromDefaultValues(it.name, labelerConf)
            }
        }
    }
    if (entries.isEmpty()) {
        Log.error("No entries found for module ${def.name}")
        return@mapNotNull null
    }
    Module(
        rootDirectory = sampleDirectory.toFile(),
        name = def.name,
        sampleDirectory = def.sampleDirectory,
        entries = entries,
        currentIndex = 0,
        rawFilePath = def.labelFile,
    )
}

private fun parseModuleGroup(
    rootSampleDirectory: String,
    moduleDefinitionGroup: List<ModuleDefinition>,
    // TODO: plugin: Plugin?,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    // TODO: pluginParams: ParamMap?,
    encoding: String,
): List<Module> {
    val results = moduleGroupFromRawLabels(moduleDefinitionGroup, labelerConf, labelerParams, encoding)
    require(moduleDefinitionGroup.size == results.size) {
        "Module group size mismatch: ${moduleDefinitionGroup.size} != ${results.size}"
    }
    return moduleDefinitionGroup.zip(results).map { (def, result) ->
        Module(
            rootDirectory = rootSampleDirectory.toFile(),
            name = def.name,
            sampleDirectory = def.sampleDirectory,
            entries = result.entries,
            extras = result.extras,
            currentIndex = 0,
            rawFilePath = def.labelFile,
        )
    }
}
