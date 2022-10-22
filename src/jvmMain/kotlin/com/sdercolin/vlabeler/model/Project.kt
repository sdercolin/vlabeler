package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.InvalidCreatedProjectException
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.io.moduleFromRawLabels
import com.sdercolin.vlabeler.io.moduleGroupFromRawLabels
import com.sdercolin.vlabeler.model.Project.Companion.ProjectVersion
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.orEmpty
import com.sdercolin.vlabeler.util.readTextByEncoding
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
@Immutable
data class Project(
    val version: Int = 0,
    val rootSampleDirectory: String? = null,
    val workingDirectory: String,
    val projectName: String,
    val cacheDirectory: String,
    @SerialName("labelerConf")
    val originalLabelerConf: LabelerConf,
    @Transient
    val labelerConf: LabelerConf = originalLabelerConf,
    val labelerParams: ParamTypedMap? = null,
    val encoding: String? = null,
    val multipleEditMode: Boolean = labelerConf.continuous,
    val modules: List<Module>,
    val currentModuleIndex: Int,
    val autoExport: Boolean,
) {

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    val isUsingDefaultCacheDirectory get() = cacheDirectory == getDefaultCacheDirectory(workingDirectory, projectName)

    val currentModule: Module
        get() = modules[currentModuleIndex]

    val currentEntry: Entry
        get() = currentModule.currentEntry

    val currentSampleName: String
        get() = currentEntry.sample

    val currentSampleFile: File
        get() = currentModule.currentSampleFile

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

    fun validate() = this.apply {
        require(modules.isNotEmpty()) { "No module found." }
        require(currentModuleIndex in modules.indices) { "Invalid current module index." }
        require(modules.distinctBy { it.name }.size == modules.size) { "Module names cannot be duplicated." }
        modules.forEach { it.validate(multipleEditMode, labelerConf) }
    }

    companion object {
        const val ProjectVersion = 2

        const val ProjectFileExtension = "lbp"
        private const val DefaultCacheDirectorySuffix = ".$ProjectFileExtension.caches"

        fun getDefaultCacheDirectory(location: String, projectName: String): String {
            return File(location, "$projectName$DefaultCacheDirectorySuffix").absolutePath
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
    inputFiles: List<File>,
    encoding: String,
): Result<List<Entry>> = runCatching {
    when (
        val result = runTemplatePlugin(
            plugin = plugin,
            params = params,
            inputFiles = inputFiles,
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
                    extras = it.extras.take(labelerConf.extraFieldNames.count()),
                )
            }.map { it.toEntry(fallbackSample = sampleFiles.first().name) }

            entries.postApplyLabelerConf(labelerConf)
        }
        is TemplatePluginResult.Raw -> moduleFromRawLabels(
            sources = result.lines,
            inputFile = inputFiles.firstOrNull(),
            labelerConf = labelerConf,
            labelerParams = labelerParams,
            sampleFiles = sampleFiles,
            encoding = encoding,
            legacyMode = labelerConf.parser.scope == null,
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
        .filter { it.injector.isNullOrEmpty().not() }
    if (paramDefsToInject.isEmpty()) return this

    val js = JavaScript()
    js.setJson("labeler", this)
    for (def in paramDefsToInject) {
        js.setJson("value", paramMap.resolveItem(def.parameter.name, project = null, js = js))
        def.injector.orEmpty().joinToString("\n").let { js.eval(it) }
    }
    val labelerResult = js.getJson<LabelerConf>("labeler")
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
        "defaultExtras",
        "fields",
        "extraFieldNames",
    ).forEach {
        val errorMessage = "Could not inject to change the size of a basic array of LabelerConf: $it"
        when (it) {
            "defaultValues" -> require(labelerResult.defaultValues.size == defaultValues.size) { errorMessage }
            "defaultExtras" -> require(labelerResult.defaultExtras.size == defaultExtras.size) { errorMessage }
            "fields" -> require(labelerResult.fields.size == fields.size) { errorMessage }
            "extraFieldNames" ->
                require(labelerResult.extraFieldNames.size == extraFieldNames.size) { errorMessage }
        }
    }
    require(labelerResult.fields.map { it.name } == fields.map { it.name }) {
        "Could not inject to change the name of a field of LabelerConf"
    }
    return labelerResult
}

/**
 * Should be called from IO threads, because this function runs scripting and may take time
 */
@Suppress("RedundantSuspendModifier")
suspend fun projectOf(
    sampleDirectory: String,
    workingDirectory: String,
    projectName: String,
    cacheDirectory: String,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    plugin: Plugin?,
    pluginParams: ParamMap?,
    inputFilePath: String?,
    encoding: String,
    autoExport: Boolean,
): Result<Project> {
    val moduleDefinitions = if (labelerConf.projectConstructor != null) {
        val js = JavaScript(logHandler = Log.infoFileHandler)

        js.set("debug", isDebug)
        js.set("root", sampleDirectory.toFile())
        js.set("encoding", encoding)
        js.setJson("acceptedSampleExtensions", Sample.acceptableSampleFileExtensions)
        listOf(
            Resources.envJs,
            Resources.fileJs,
            Resources.expectedErrorJs,
            Resources.moduleDefinitionJs,
            Resources.prepareBuildProjectJs,
        ).forEach { js.execResource(it) }
        labelerParams.resolve(project = null, js = js).let { js.setJson("params", it) }
        labelerConf.projectConstructor.scripts.joinToString("\n").let { js.eval(it) }
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

    val modules = runCatching {
        parseModule(moduleDefinitions, plugin, sampleDirectory, labelerConf, labelerParams, pluginParams, encoding)
    }.getOrElse {
        return Result.failure(InvalidCreatedProjectException(it))
    }

    val labelerTypedParams = labelerParams.let { ParamTypedMap.from(it, labelerConf.parameterDefs) }

    return runCatching {
        val injectedLabelerConf = labelerParams.let { labelerConf.injectLabelerParams(it) }

        require(modules.isNotEmpty()) {
            "No entries were found for any module"
        }
        Project(
            version = ProjectVersion,
            rootSampleDirectory = sampleDirectory,
            workingDirectory = workingDirectory,
            projectName = projectName,
            cacheDirectory = cacheDirectory,
            labelerConf = injectedLabelerConf,
            originalLabelerConf = labelerConf,
            labelerParams = labelerTypedParams,
            encoding = encoding,
            modules = modules,
            currentModuleIndex = 0,
            autoExport = autoExport,
        ).validate()
    }.onFailure {
        return Result.failure(InvalidCreatedProjectException(it))
    }
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
        parseModuleGroup(group, labelerConf, labelerParams, encoding)
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
                inputFiles = def.inputFiles.orEmpty().filter { it.exists() },
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
                legacyMode = labelerConf.parser.scope == null,
            )
        }
        else -> {
            def.sampleFiles.map {
                Entry.fromDefaultValues(it.name, it.nameWithoutExtension, labelerConf)
            }
        }
    }
    if (entries.isEmpty()) {
        Log.error("No entries found for module ${def.name}")
        return@mapNotNull null
    }
    Module(
        name = def.name,
        sampleDirectory = def.sampleDirectory.absolutePath,
        entries = entries,
        currentIndex = 0,
        rawFilePath = def.labelFile?.absolutePath,
    )
}

private fun parseModuleGroup(
    moduleDefinitionGroup: List<ModuleDefinition>,
    // TODO: plugin: Plugin?,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    // TODO: pluginParams: ParamMap?,
    encoding: String,
): List<Module> {
    val result = moduleGroupFromRawLabels(moduleDefinitionGroup, labelerConf, labelerParams, encoding)
    require(moduleDefinitionGroup.size == result.size) {
        "Module group size mismatch: ${moduleDefinitionGroup.size} != ${result.size}"
    }
    return moduleDefinitionGroup.zip(result).map { (def, entries) ->
        Module(
            name = def.name,
            sampleDirectory = def.sampleDirectory.absolutePath,
            entries = entries,
            currentIndex = 0,
            rawFilePath = def.labelFile?.absolutePath,
        )
    }
}
