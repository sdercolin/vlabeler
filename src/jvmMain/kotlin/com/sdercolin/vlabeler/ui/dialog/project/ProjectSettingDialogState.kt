package com.sdercolin.vlabeler.ui.dialog.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.injectLabelerParams
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.resolve
import kotlinx.coroutines.launch
import java.nio.file.Files
import kotlin.io.path.Path

class ProjectSettingDialogState(
    val appState: AppState,
    private val finish: () -> Unit,
) {
    val project get() = requireNotNull(appState.project)
    var encoding: String by mutableStateOf(project.encoding)

    val isOutputFileEditable: Boolean
        get() = project.modules.size == 1 && project.labelerConf.isSelfConstructed.not()

    var outputFile: String? by mutableStateOf(
        project.currentModule.getRawFile(project)?.absolutePath.orEmpty().takeIf { isOutputFileEditable },
    )
        private set

    var isShowingOutputFileDialog by mutableStateOf(false)

    fun updateOutputFile(newOutputFile: String) {
        outputFile = newOutputFile
    }

    val isOutputFileValid: Boolean
        get() {
            val outputFile = outputFile ?: return true
            return Files.isWritable(Path(outputFile))
        }

    val canChangeAutoExport: Boolean
        get() = when {
            project.labelerConf.isSelfConstructed -> true
            project.labelerConf.defaultInputFilePath != null -> true
            project.modules.any { it.rawFilePath != null } -> true
            outputFile.isNullOrEmpty().not() && isOutputFileValid -> true
            else -> false
        }
    var autoExport: Boolean by mutableStateOf(project.autoExport)
    private val originalLabelerParams: ParamMap = project.labelerParams.resolve(project.labelerConf)
    var labelerParams: ParamMap by mutableStateOf(originalLabelerParams)
        private set
    var labelerSavedParams: ParamMap by mutableStateOf(originalLabelerParams)
        private set
    var labelerError: Boolean by mutableStateOf(false)
        private set

    var isShowingLabelerDialog by mutableStateOf(false)

    fun updateLabelerParams(newParams: ParamMap) {
        labelerParams = newParams
        labelerError = project.labelerConf.checkParams(newParams, null) == false
    }

    fun saveLabelerParams(params: ParamMap) {
        appState.mainScope.launch {
            project.labelerConf.saveParams(params, project.labelerConf.getSavedParamsFile())
            labelerSavedParams = params
            updateLabelerParams(params)
        }
    }

    val isError get() = labelerError || !isOutputFileValid

    private fun createNewProject(): Project {
        val newLabelerConf = if (originalLabelerParams == labelerParams) {
            project.labelerConf
        } else {
            project.originalLabelerConf.injectLabelerParams(labelerParams)
        }
        val modulesUpdated = if (outputFile == null) {
            project.modules
        } else {
            project.modules.map { module ->
                module.copy(
                    rawFilePath = outputFile?.ifEmpty { null } ?: module.rawFilePath,
                )
            }
        }
        return project.copy(
            encoding = encoding,
            autoExport = autoExport,
            labelerConf = newLabelerConf,
            labelerParams = ParamTypedMap.from(labelerParams, newLabelerConf.parameterDefs),
            modules = modulesUpdated,
        )
    }

    fun cancel() {
        finish()
    }

    fun submit() {
        val newProject = createNewProject()
        appState.updateProject(newProject)
        finish()
    }
}
