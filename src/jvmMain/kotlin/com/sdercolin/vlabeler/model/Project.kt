package com.sdercolin.vlabeler.model

import java.io.File

data class Project(
    val workingPath: File,
    val samples: List<SampleInfo>,
    val entriesBySampleName: Map<String, List<Entry>>
)