package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.RecordDir
import com.sdercolin.vlabeler.util.mergeDefaults
import com.sdercolin.vlabeler.util.orEmpty
import kotlinx.serialization.json.JsonObject
import java.io.File

fun LabelerConf.getSavedParamsFile(): File = RecordDir.resolve(name + LabelerSavedParamsFileExtension)

fun LabelerConf.getResolvedParamsWithDefaults(params: ParamMap?, js: JavaScript): JsonObject {
    val defaultMap = parameters.associate { it.parameter.name to it.parameter.defaultValue }
    return params.orEmpty().mergeDefaults(defaultMap).resolve(project = null, js = js)
}

private const val LabelerSavedParamsFileExtension = ".labeler.param.json"
