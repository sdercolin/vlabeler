package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import java.io.File

@Immutable
data class Project(
    val workingDirectory: File,
    val entriesBySampleName: Map<String, List<Entry>>,
    val appConf: AppConf,
    val labelerConf: LabelerConf
)