package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.InvalidCreatedProjectException
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.io.fromRawLabels
import com.sdercolin.vlabeler.model.Project.Companion.ProjectVersion
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.orEmpty
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.nio.charset.Charset

@Serializable
@Immutable
data class Project(
    val version: Int = 0,
    val sampleDirectory: String,
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

    @Serializable
    @Immutable
    data class Module(
        val name: String,
        val sampleDirectory: String,
        val entries: List<Entry>,
        val currentIndex: Int,
        val rawFilePath: String? = null,
        val entryFilter: EntryFilter? = null,
    ) {
        @Transient
        private val filteredEntryIndexes: List<Int> =
            entries.indices.filter { entryFilter?.matches(entries[it]) ?: true }

        @Transient
        private val entryIndexGroups: List<Pair<String, List<Int>>> = entries.indexGroupsConnected()

        @Transient
        private val filteredEntryIndexGroupIndexes: List<Pair<Int, List<Int>>> = entryIndexGroups
            .mapIndexed { index, pair -> index to pair.second }
            .map { (groupIndex, entryIndexes) ->
                groupIndex to entryIndexes.filter { entryFilter?.matches(entries[it]) ?: true }
            }
            .filter { it.second.isNotEmpty() }

        @Transient
        private val entryGroups: List<Pair<String, List<Entry>>> = entries.entryGroupsConnected()

        val currentEntry: Entry
            get() = entries[currentIndex]

        private val currentGroupIndex: Int
            get() = getGroupIndex(currentIndex)

        val currentSampleName: String
            get() = currentEntry.sample

        val currentSampleFile: File
            get() = getSampleFile(currentSampleName)

        fun getSampleFile(sampleName: String): File {
            return File(sampleDirectory, sampleName)
        }

        @Transient
        val entryCount: Int = entries.size
        private fun getGroupIndex(entryIndex: Int) = entryIndexGroups.indexOfFirst { it.second.contains(entryIndex) }

        fun getEntriesForEditing(index: Int?, multipleEditMode: Boolean) = if (!multipleEditMode) {
            listOf(getEntryForEditing(index ?: currentIndex))
        } else {
            getEntriesInGroupForEditing(getGroupIndex(index ?: currentIndex))
        }

        private fun getEntryForEditing(index: Int = currentIndex) = IndexedEntry(
            entry = entries[index],
            index = index,
        )

        fun getEntriesInGroupForEditing(groupIndex: Int = currentGroupIndex) = entryIndexGroups[groupIndex].second
            .map {
                IndexedEntry(
                    entry = entries[it],
                    index = it,
                )
            }

        fun updateOnLoadedSample(sampleInfo: SampleInfo): Module {
            val entries = entries.toMutableList()
            val changedEntries = entries.withIndex()
                .filter { it.value.sample == sampleInfo.name }
                .filter { it.value.end <= 0f }
                .map {
                    val end = sampleInfo.lengthMillis + it.value.end
                    it.copy(value = it.value.copy(end = end))
                }
            if (changedEntries.isEmpty()) return this
            changedEntries.forEach { entries[it.index] = it.value }
            return copy(entries = entries)
        }

        fun updateEntries(editedEntries: List<IndexedEntry>, labelerConf: LabelerConf): Module {
            val entries = entries.toMutableList()
            if (labelerConf.continuous) {
                val previousIndex = editedEntries.first().index - 1
                entries.getOrNull(previousIndex)
                    ?.takeIf { it.sample == editedEntries.first().sample }
                    ?.copy(end = editedEntries.first().start)
                    ?.let { entries[previousIndex] = it }
                val nextIndex = editedEntries.last().index + 1
                entries.getOrNull(nextIndex)
                    ?.takeIf { it.sample == editedEntries.last().sample }
                    ?.copy(start = editedEntries.last().end)
                    ?.let { entries[nextIndex] = it }
            }
            editedEntries.forEach {
                entries[it.index] = it.entry
            }
            return copy(entries = entries)
        }

        private fun updateEntry(editedEntry: IndexedEntry, labelerConf: LabelerConf) =
            updateEntries(listOf(editedEntry), labelerConf)

        fun markEntriesAsDone(editedIndexes: Set<Int>): Module {
            val entries = entries.toMutableList()
            editedIndexes.forEach {
                entries[it] = entries[it].done()
            }
            return copy(entries = entries)
        }

        fun renameEntry(index: Int, newName: String, labelerConf: LabelerConf): Module {
            val editedEntry = getEntryForEditing(index)
            val renamed = editedEntry.entry.copy(name = newName)
            return updateEntry(editedEntry.edit(renamed), labelerConf)
        }

        fun duplicateEntry(index: Int, newName: String, labelerConf: LabelerConf): Module {
            val entries = entries.toMutableList()
            var original = entries[index]
            var duplicated = original.copy(name = newName)
            if (labelerConf.continuous) {
                val splitPoint = (original.start + original.end) / 2
                original = original.copy(end = splitPoint)
                duplicated = duplicated.copy(start = splitPoint)
                entries[index] = original
            }
            entries.add(index + 1, duplicated)
            return copy(entries = entries, currentIndex = index)
        }

        fun removeCurrentEntry(labelerConf: LabelerConf): Module {
            val index = currentIndex
            val entries = entries.toMutableList()
            val removed = requireNotNull(entries.removeAt(index))
            val newIndex = index - 1
            if (labelerConf.continuous) {
                val previousIndex = index - 1
                entries.getOrNull(previousIndex)
                    ?.takeIf { it.sample == removed.sample }
                    ?.copy(end = removed.end)
                    ?.let { entries[previousIndex] = it }
            }
            return copy(entries = entries, currentIndex = newIndex)
        }

        fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?): Module {
            val entries = entries.toMutableList()
            val entry = entries[index]
            val editedCurrentEntry = entry.copy(
                name = rename ?: entry.name,
                end = position,
                points = entry.points.map { it.coerceAtMost(position) },
                notes = entry.notes.copy(done = true),
            )
            val newEntry = entry.copy(
                name = newName,
                start = position,
                points = entry.points.map { it.coerceAtLeast(position) },
                notes = entry.notes.copy(done = true),
            )
            entries[index] = editedCurrentEntry
            entries.add(index + 1, newEntry)
            val newIndex = targetEntryIndex ?: index
            return copy(entries = entries, currentIndex = newIndex)
        }

        fun nextEntry() = switchEntry(reverse = false)
        fun previousEntry() = switchEntry(reverse = true)
        private fun switchEntry(reverse: Boolean): Module {
            val targetIndex = if (reverse) {
                filteredEntryIndexes.lastOrNull { it < currentIndex } ?: currentIndex
            } else {
                filteredEntryIndexes.firstOrNull { it > currentIndex } ?: currentIndex
            }
            return copy(currentIndex = targetIndex)
        }

        fun nextSample() = switchSample(reverse = false)
        fun previousSample() = switchSample(reverse = true)
        private fun switchSample(reverse: Boolean): Module {
            val currentGroupIndex = getGroupIndex(currentIndex)
            val targetGroupIndex = if (reverse) {
                filteredEntryIndexGroupIndexes.lastOrNull { it.first < currentGroupIndex }?.first ?: currentGroupIndex
            } else {
                filteredEntryIndexGroupIndexes.firstOrNull { it.first > currentGroupIndex }?.first ?: currentGroupIndex
            }
            val targetEntryIndex = if (targetGroupIndex == currentGroupIndex) {
                val indexesInCurrentGroup = filteredEntryIndexGroupIndexes.first { it.first == targetGroupIndex }.second
                if (reverse) indexesInCurrentGroup.first() else indexesInCurrentGroup.last()
            } else {
                val indexesInTargetGroup = filteredEntryIndexGroupIndexes.first { it.first == targetGroupIndex }.second
                if (reverse) indexesInTargetGroup.last() else indexesInTargetGroup.first()
            }
            return copy(currentIndex = targetEntryIndex)
        }

        fun hasSwitchedSample(previous: Module?) = previous != null && previous.currentSampleName != currentSampleName

        fun toggleEntryDone(index: Int): Module {
            val entry = entries[index]
            val editedEntry = entry.doneToggled()
            return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
        }

        fun toggleEntryStar(index: Int): Module {
            val entry = entries[index]
            val editedEntry = entry.starToggled()
            return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
        }

        fun editEntryTag(index: Int, tag: String): Module {
            val entry = entries[index]
            val editedEntry = entry.tagEdited(tag)
            return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
        }

        fun validate(multipleEditMode: Boolean, labelerConf: LabelerConf): Module {
            // Check multiMode enabled
            if (multipleEditMode) require(
                labelerConf.continuous,
            ) { "Multi-entry mode can only be used in continuous labelers." }

            // Check currentIndex valid
            requireNotNull(entries.getOrNull(currentIndex)) { "Invalid currentIndex: $currentIndex" }

            // Check continuous
            if (labelerConf.continuous) {
                entryGroups.forEach { (_, entries) ->
                    entries.zipWithNext().forEach {
                        require(it.first.end == it.second.start) {
                            "Not continuous between entries: $it"
                        }
                    }
                }
            }

            // check entry name duplicates
            if (!labelerConf.allowSameNameEntry) {
                val names = entries.map { it.name }
                require(names.distinct().size == names.size) { "Duplicate entry names found." }
            }

            // Check points
            val entries = entries.map {
                require(it.points.size == labelerConf.fields.size) {
                    "Point size doesn't match in entry: $it. Required point size = ${labelerConf.fields.size}"
                }
                require(it.extras.size == labelerConf.extraFieldNames.size) {
                    "Extra size doesn't match in entry: $it. Required extra size = ${labelerConf.extraFieldNames.size}"
                }
                if (it.end > 0) require(it.start <= it.end) {
                    "Start is greater than end in entry: $it"
                }

                var entryResult = it

                if (it.start < 0) {
                    entryResult = entryResult.copy(start = 0f)
                }

                // do not check right border, because we don't know the length of the audio file

                it.points.forEachIndexed { index, point ->
                    runCatching {
                        require(point >= entryResult.start) {
                            "Point $point is smaller than start in entry: $it"
                        }
                    }.onFailure { t ->
                        when (labelerConf.overflowBeforeStart) {
                            LabelerConf.PointOverflow.AdjustBorder -> {
                                entryResult = entryResult.copy(start = point)
                            }
                            LabelerConf.PointOverflow.AdjustPoint -> {
                                val points = entryResult.points.toMutableList()
                                points[index] = entryResult.start
                                entryResult = entryResult.copy(points = points)
                            }
                            LabelerConf.PointOverflow.Error -> throw t
                        }
                    }
                    if (it.end > 0) {
                        runCatching {
                            require(point <= entryResult.end) {
                                "Point $point is greater than end in entry: $it"
                            }
                        }.onFailure { t ->
                            when (labelerConf.overflowAfterEnd) {
                                LabelerConf.PointOverflow.AdjustBorder -> {
                                    entryResult = entryResult.copy(end = point)
                                }
                                LabelerConf.PointOverflow.AdjustPoint -> {
                                    val points = entryResult.points.toMutableList()
                                    points[index] = entryResult.end
                                    entryResult = entryResult.copy(points = points)
                                }
                                LabelerConf.PointOverflow.Error -> throw t
                            }
                        }
                    }
                }
                entryResult
            }

            return copy(entries = entries)
        }
    }

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
        modules.forEach { it.validate(multipleEditMode, labelerConf) }
    }

    companion object {
        const val ProjectVersion = 1

        const val ProjectFileExtension = "lbp"
        private const val DefaultCacheDirectorySuffix = ".$ProjectFileExtension.caches"

        fun getDefaultCacheDirectory(location: String, projectName: String): String {
            return File(location, "$projectName$DefaultCacheDirectorySuffix").absolutePath
        }
    }
}

private fun generateEntriesByPlugin(
    labelerConf: LabelerConf,
    labelerParams: ParamMap?,
    sampleFiles: List<File>,
    plugin: Plugin,
    params: ParamMap?,
    inputFiles: List<File>,
    encoding: String,
): Result<List<Entry>> = runCatching {
    when (
        val result =
            runTemplatePlugin(plugin, params.orEmpty(), inputFiles, encoding, sampleFiles, labelerConf)
    ) {
        is TemplatePluginResult.Parsed -> {
            val entries = result.entries.map {
                it.copy(
                    points = it.points.take(labelerConf.fields.count()),
                    extras = it.extras.take(labelerConf.extraFieldNames.count()),
                )
            }.map { it.toEntry(fallbackSample = sampleFiles.first().nameWithoutExtension) }

            entries.postApplyLabelerConf(labelerConf)
        }
        is TemplatePluginResult.Raw -> fromRawLabels(
            sources = result.lines,
            inputFile = inputFiles.firstOrNull(),
            labelerConf = labelerConf,
            labelerParams = labelerParams,
            sampleFiles = sampleFiles,
        )
    }
}

fun List<Entry>.postApplyLabelerConf(
    labelerConf: LabelerConf,
): List<Entry> = toContinuous(labelerConf.continuous)
    .distinct(labelerConf.allowSameNameEntry)

private fun List<Entry>.indexGroupsConnected(): List<Pair<String, List<Int>>> = withIndex()
    .fold(listOf<Pair<String, MutableList<IndexedValue<Entry>>>>()) { acc, entry ->
        val lastGroup = acc.lastOrNull()
        if (lastGroup == null || lastGroup.first != entry.value.sample) {
            acc.plus(entry.value.sample to mutableListOf(entry))
        } else {
            lastGroup.second.add(entry)
            acc
        }
    }.map { group -> group.first to group.second.map { it.index } }

private fun List<Entry>.entryGroupsConnected() = indexGroupsConnected().map { group ->
    group.first to group.second.map { this[it] }
}

private fun List<Entry>.toContinuous(continuous: Boolean): List<Entry> {
    if (!continuous) return this
    return entryGroupsConnected()
        .flatMap { (_, entries) ->
            entries
                .sortedBy { it.start }
                .distinctBy { it.start }
                .let {
                    it.zipWithNext { current, next ->
                        current.copy(end = next.start)
                    }.plus(it.last())
                }
        }
}

private fun List<Entry>.distinct(allowDuplicated: Boolean): List<Entry> {
    if (allowDuplicated) return this
    return distinctBy { it.name }
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
    labelerParams: ParamMap?,
    plugin: Plugin?,
    pluginParams: ParamMap?,
    inputFilePath: String,
    encoding: String,
    autoExportTargetPath: String?,
): Result<Project> {
    val moduleDefinitions = if (labelerConf.projectConstructor != null) {
        val js = JavaScript(logHandler = Log.infoFileHandler)

        js.set("root", sampleDirectory.toFile())
        js.setJson("acceptedSampleExtensions", Sample.acceptableSampleFileExtensions)
        listOf(
            Resources.fileJs,
            Resources.moduleDefinitionJs,
            Resources.prepareBuildProjectJs,
        ).forEach { js.execResource(it) }
        labelerParams?.resolve(project = null, js = js)?.let { js.setJson("params", it) }
        labelerConf.projectConstructor.scripts.joinToString("\n").let { js.eval(it) }
        val modules = js.getJson<List<RawModuleDefinition>>("modules")
        js.close()
        modules.map { it.toModuleDefinition() }
    } else {
        val sampleDirectoryFile = File(sampleDirectory)
        val sampleFiles = Sample.listSampleFiles(sampleDirectoryFile)
        val inputFile = inputFilePath.ifEmpty { null }?.toFile()
        listOf(
            ModuleDefinition(
                name = "",
                sampleDirectory = sampleDirectoryFile,
                sampleFiles = sampleFiles,
                sampleNames = sampleFiles.map { it.nameWithoutExtension },
                inputFiles = listOfNotNull(inputFile),
                labelFile = inputFile ?: labelerConf.defaultInputFilePath?.let { sampleDirectoryFile.resolve(it) },
            ),
        )
    }

    val modules = moduleDefinitions.mapNotNull { def ->
        val existingSingleInputFile = def.inputFiles?.firstOrNull { it.exists() }
        val entries = when {
            plugin != null -> {
                generateEntriesByPlugin(
                    labelerConf = labelerConf,
                    labelerParams = labelerParams,
                    sampleFiles = def.sampleFiles,
                    plugin = plugin,
                    params = pluginParams,
                    inputFiles = def.inputFiles.orEmpty().filter { it.exists() },
                    encoding = encoding,
                )
                    .getOrElse {
                        return Result.failure(it)
                    }
            }
            existingSingleInputFile != null -> {
                fromRawLabels(
                    existingSingleInputFile.readLines(Charset.forName(encoding)),
                    existingSingleInputFile,
                    labelerConf,
                    labelerParams,
                    def.sampleFiles,
                )
            }
            else -> {
                def.sampleNames.map {
                    Entry.fromDefaultValues(it, it, labelerConf)
                }
            }
        }
        if (entries.isEmpty()) {
            Log.error("No entries found for module ${def.name}")
            return@mapNotNull null
        }
        Project.Module(
            name = def.name,
            sampleDirectory = def.sampleDirectory.absolutePath,
            entries = entries,
            currentIndex = 0,
            rawFilePath = def.labelFile?.absolutePath,
        )
    }

    val labelerTypedParams = labelerParams?.let { ParamTypedMap.from(it, labelerConf.parameterDefs) }

    return runCatching {
        val injectedLabelerConf = labelerParams?.let { labelerConf.injectLabelerParams(it) } ?: labelerConf

        require(modules.isNotEmpty()) {
            "No entries were found for any module"
        }
        Project(
            version = ProjectVersion,
            sampleDirectory = sampleDirectory,
            workingDirectory = workingDirectory,
            projectName = projectName,
            cacheDirectory = cacheDirectory,
            labelerConf = injectedLabelerConf,
            originalLabelerConf = labelerConf,
            labelerParams = labelerTypedParams,
            encoding = encoding,
            modules = modules,
            currentModuleIndex = 0,
            autoExport = autoExportTargetPath != null,
        ).validate()
    }.onFailure {
        return Result.failure(InvalidCreatedProjectException(it))
    }
}
