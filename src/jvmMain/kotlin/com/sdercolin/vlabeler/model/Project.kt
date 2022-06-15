package com.sdercolin.vlabeler.model

import java.io.File

data class Project(
    val workingDirectory: File,
    val entriesBySampleName: Map<String, List<Entry>>,
    val appConf: AppConf,
    val labelerConf: LabelerConf
)