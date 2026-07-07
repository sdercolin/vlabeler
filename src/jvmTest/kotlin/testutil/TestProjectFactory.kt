package testutil

import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.util.DefaultEncoding
import com.sdercolin.vlabeler.util.ParamMap
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Creates a [Project] through the real creation flow ([projectOf]), including the labeler's project constructor and
 * parser scripts, in the same way as the "New Project" flow of the application.
 */
fun createTestProject(
    labeler: LabelerConf,
    sampleDirectory: File,
    workingDirectory: File = sampleDirectory,
    projectName: String = "test-project",
    labelerParams: ParamMap = labeler.getDefaultParams(),
    inputFilePath: String? = null,
    encoding: String = DefaultEncoding,
): Project = runBlocking {
    TestEnv.ensureLogDirectory()
    projectOf(
        sampleDirectory = sampleDirectory.absolutePath,
        workingDirectory = workingDirectory.absolutePath,
        projectName = projectName,
        cacheDirectory = Project.getDefaultCacheDirectory(workingDirectory.absolutePath, projectName),
        rawLabelerConf = labeler,
        labelerParams = labelerParams,
        plugin = null,
        pluginParams = null,
        inputFilePath = inputFilePath,
        encoding = encoding,
        autoExport = false,
    ).getOrThrow()
}
