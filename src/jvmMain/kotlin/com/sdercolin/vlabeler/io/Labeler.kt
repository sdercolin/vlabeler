package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.RecordDir
import java.io.File

fun LabelerConf.getSavedParamsFile(): File = RecordDir.resolve(name + LabelerSavedParamsFileExtension)

private const val LabelerSavedParamsFileExtension = ".param.saved.json"
