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

class ProjectSettingDialogState(
    val appState: AppState,
    private val finish: () -> Unit,
) {
    val project get() = requireNotNull(appState.project)
    var encoding: String by mutableStateOf(project.encoding)

    val canChangeAutoExport: Boolean
        get() = when {
            project.labelerConf.isSelfConstructed -> true
            project.labelerConf.defaultInputFilePath != null -> true
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

    private fun createNewProject(): Project {
        val newLabelerConf = if (originalLabelerParams == labelerParams) {
            project.labelerConf
        } else {
            project.originalLabelerConf.injectLabelerParams(labelerParams)
        }
        return project.copy(
            encoding = encoding,
            autoExport = autoExport,
            labelerConf = newLabelerConf,
            labelerParams = ParamTypedMap.from(labelerParams, newLabelerConf.parameterDefs),
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
