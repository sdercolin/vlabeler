package testutil

import com.sdercolin.vlabeler.io.asLabelerConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.DefaultLabelerDir

/**
 * Loads the bundled labelers under `resources/common/labelers` for tests.
 *
 * This requires the `compose.application.resources.dir` system property, which is set on the test task in
 * `build.gradle.kts`.
 */
object TestLabelers {

    val utauOto: LabelerConf by lazy { load("oto-labeler") }
    val utauSinger: LabelerConf by lazy { load("utau-singer-labeler") }
    val nnsvsSinger: LabelerConf by lazy { load("nnsvs-singer-labeler") }
    val audacity: LabelerConf by lazy { load("audacity-labeler") }
    val sinsy: LabelerConf by lazy { load("sinsy-labeler") }

    private fun load(directoryName: String): LabelerConf = DefaultLabelerDir
        .resolve(directoryName)
        .resolve(LabelerConf.LABELER_FILE_EXTENSION)
        .asLabelerConf(isBuiltIn = true)
        .getOrThrow()
}
