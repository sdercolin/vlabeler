package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.exception.PluginUnexpectedRuntimeException
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringCertain
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import java.io.File

/**
 * Type of a module (subproject) management operation defined in [LabelerConf.ModuleManagement].
 */
enum class ModuleOperationType(val defaultNameStringKey: Strings) {
    Add(Strings.ModuleOperationAddDefaultName),
    Rename(Strings.ModuleOperationRenameDefaultName),
    Remove(Strings.ModuleOperationRemoveDefaultName),
    Duplicate(Strings.ModuleOperationDuplicateDefaultName),
}

/**
 * A [BasePlugin] wrapper of a [LabelerConf.ModuleOperation], so that the parameter dialog and parameter persistence
 * used by plugins can be reused for module operations.
 */
@Immutable
data class ModuleOperationDescriptor(
    val labeler: LabelerConf,
    val type: ModuleOperationType,
) : BasePlugin {

    val operation: LabelerConf.ModuleOperation
        get() = requireNotNull(labeler.moduleManagement.getOperation(type)) {
            "Module operation `$type` is not defined by labeler `${labeler.name}`"
        }

    override val name: String
        get() = "${labeler.name}.$MODULE_OPERATION_NAME_SECTION.${type.name.lowercase()}"

    override val version: Int
        get() = labeler.version

    override val displayedName: LocalizedJsonString
        get() = operation.displayedName
            ?: LocalizedJsonString(
                Language.entries.associate { it.code to stringCertain(type.defaultNameStringKey, it) },
            )

    override val description: LocalizedJsonString
        get() = operation.description ?: "".toLocalized()

    override val author: String
        get() = labeler.author

    override val email: String
        get() = labeler.email

    override val website: String
        get() = labeler.website

    override val directory: File?
        get() = labeler.directory

    override val parameterDefs: List<Parameter<*>>
        get() = operation.parameters

    override val resourceFiles: List<String>
        get() = labeler.resourceFiles

    override fun getSavedParamsFile(): File = RecordDir.resolve(name + MODULE_OPERATION_SAVED_PARAMS_FILE_EXTENSION)

    companion object {
        private const val MODULE_OPERATION_NAME_SECTION = "moduleManagement"
        private const val MODULE_OPERATION_SAVED_PARAMS_FILE_EXTENSION = ".param.json"
    }
}

/**
 * Run a module (subproject) management operation defined by the labeler of the given [project]. The scripts are
 * executed in the same environment as a macro plugin with `Project` scope, except for plugin-specific inputs.
 *
 * @return The updated project.
 */
fun runModuleOperation(
    descriptor: ModuleOperationDescriptor,
    params: ParamMap,
    project: Project,
    onReport: (LocalizedJsonString) -> Unit,
): Project {
    val labelerConf = project.labelerConf
    val js = JavaScript(currentWorkingDirectory = labelerConf.directory)
    val result = runCatching {
        js.set("debug", isDebug)
        js.setJson("labeler", labelerConf)
        val labelerParams = project.labelerParams.resolve(labelerConf).resolve(project, js)
        js.setJson("labelerParams", labelerParams)
        js.setJson("params", params.resolve(project, js))
        js.setJson("resources", labelerConf.readResourceFiles())

        listOf(
            Resources.classEntryJs,
            Resources.classModuleJs,
            Resources.expectedErrorJs,
            Resources.reportJs,
            Resources.envJs,
            Resources.fileJs,
        ).forEach { js.execResource(it) }

        js.setJson("modules", project.modules.map { it.toJs(project) })
        js.set("projectRootDirectory", project.rootSampleDirectory)
        js.eval("projectRootDirectory = new File(projectRootDirectory)")
        js.set("currentModuleIndex", project.currentModuleIndex)

        js.eval(descriptor.operation.scripts.getScripts(labelerConf.directory))

        val modules = js.getJsonOrNull<List<JsModule>>("modules")
            ?.map { it.toModule(project.rootSampleDirectory) }
        val newProject = if (modules != null) {
            project.copy(
                modules = modules,
                currentModuleIndex = js.getOrNull("currentModuleIndex") ?: project.currentModuleIndex,
            ).validate().makeRelativePathsIfPossible()
        } else {
            project
        }
        val report = js.getOrNull<String>("reportText")?.parseJson<LocalizedJsonString>()
        if (report != null) {
            onReport(report)
        }
        newProject
    }.getOrElse {
        val expected = js.getOrNull("expectedError") ?: false
        js.close()
        if (expected) {
            throw PluginRuntimeException(it, it.message?.parseJson())
        } else {
            throw PluginUnexpectedRuntimeException(it)
        }
    }
    js.close()
    return result
}
