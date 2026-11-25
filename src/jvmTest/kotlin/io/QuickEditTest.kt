package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.QuickEditRequest
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [QuickEditRequest] construction and [QuickEditRequest.create].
 *
 * The request construction part drives the bundled `quickProjectBuilder` scripts of the labelers in the same way as
 * `startQuickEdit`, without the [com.sdercolin.vlabeler.ui.AppState] dependency.
 */
class QuickEditTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    /**
     * Runs the labeler's quick project builder script against [file], mirroring the script execution part of
     * `startQuickEdit` in `io/QuickEdit.kt`.
     */
    private fun buildRequestViaScript(labelerConf: LabelerConf, file: File): QuickEditRequest {
        val builder = labelerConf.quickProjectBuilders.single()
        val js = JavaScript()
        listOf(
            Resources.envJs,
            Resources.fileJs,
            Resources.expectedErrorJs,
        ).forEach { js.execResource(it) }
        js.set("debug", false)
        js.set("input", file)
        js.eval("input = new File(input)")
        val savedParams = runBlocking { labelerConf.loadSavedParamsJson(labelerConf.getSavedParamsFile()) }
        js.set("savedParams", savedParams)
        js.eval("savedParams = JSON.parse(savedParams)")
        js.eval(builder.scripts.getScripts(labelerConf.directory))
        js.eval("projectFile = projectFile.getAbsolutePath()")
        js.eval("sampleDirectory = sampleDirectory.getAbsolutePath()")
        if (js.hasValue("cacheDirectory")) {
            js.eval("cacheDirectory = cacheDirectory.getAbsolutePath()")
        }
        if (!js.hasValue("params")) {
            js.eval("params = savedParams")
        }
        js.eval("params = JSON.stringify(params)")
        val request = QuickEditRequest(
            labelerConf = labelerConf,
            builder = builder,
            projectFile = js.get("projectFile"),
            sampleDirectory = js.get("sampleDirectory"),
            cacheDirectory = js.getOrNull("cacheDirectory"),
            inputFile = file.absolutePath,
            params = labelerConf.parseParamMap(js.get("params")),
            encoding = js.getOrNull("encoding"),
        )
        js.close()
        return request
    }

    private fun deployOtoVoicebank(): File = TestFixtures.deploy(
        "oto",
        tempDir.resolve("voice"),
        wavFiles = listOf("_a_ka.wav"),
    )

    private fun otoRequest(
        projectFile: String,
        sampleDirectory: String,
        cacheDirectory: String? = null,
        inputFile: String? = null,
        encoding: String? = null,
    ): QuickEditRequest {
        val labeler = TestLabelers.utauOto
        return QuickEditRequest(
            labelerConf = labeler,
            builder = labeler.quickProjectBuilders.single(),
            projectFile = projectFile,
            sampleDirectory = sampleDirectory,
            cacheDirectory = cacheDirectory,
            inputFile = inputFile,
            params = labeler.getDefaultParams(),
            encoding = encoding,
        )
    }

    private fun QuickEditRequest.createProject(): Project = runBlocking { create() }.getOrThrow()

    @Test
    fun testOtoBuilderScriptCreatesProject() {
        val voicebank = deployOtoVoicebank()
        val otoFile = voicebank.resolve("oto.ini")

        val request = buildRequestViaScript(TestLabelers.utauOto, otoFile)

        assertEquals(voicebank.resolve("voice_oto_quickedit.lbp").absolutePath, request.projectFile)
        assertEquals(voicebank.absolutePath, request.sampleDirectory)
        assertNull(request.cacheDirectory)
        assertEquals("Shift-JIS", request.encoding)
        assertEquals(otoFile.absolutePath, request.inputFile)
        assertEquals(true, request.params["useNegativeOvl"])

        val project = request.createProject()
        assertEquals("voice_oto_quickedit", project.projectName)
        assertEquals("Shift-JIS", project.encoding)
        assertTrue(project.autoExport)
        assertTrue(project.isUsingDefaultCacheDirectory)
        assertEquals(voicebank.resolve("voice_oto_quickedit.lbp").absolutePath, project.projectFile.absolutePath)
        assertEquals(1, project.modules.size)
        assertEquals(listOf("- a", "a ka"), project.modules.single().entries.map { it.name })
        assertEquals(listOf("_a_ka.wav", "_a_ka.wav"), project.modules.single().entries.map { it.sample })
    }

    @Test
    fun testUtauSingerBuilderScriptCreatesMultiModuleProject() {
        val singer = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )

        val request = buildRequestViaScript(TestLabelers.utauSinger, singer)

        assertEquals(singer.resolve("singer_quickedit.lbp").absolutePath, request.projectFile)
        assertEquals(singer.absolutePath, request.sampleDirectory)
        assertNull(request.cacheDirectory)
        assertEquals("Shift-JIS", request.encoding)

        val project = request.createProject()
        assertEquals("singer_quickedit", project.projectName)
        assertTrue(project.autoExport)
        assertEquals(setOf("A3", "C4"), project.modules.map { it.name }.toSet())
        assertEquals(2, project.modules.first { it.name == "A3" }.entries.size)
        assertEquals(4, project.modules.first { it.name == "C4" }.entries.size)
    }

    @Test
    fun testCreateWithExplicitCacheDirectory() {
        val voicebank = deployOtoVoicebank()
        val cacheDirectory = tempDir.resolve("custom-caches")

        val project = otoRequest(
            projectFile = voicebank.resolve("project.lbp").absolutePath,
            sampleDirectory = voicebank.absolutePath,
            cacheDirectory = cacheDirectory.absolutePath,
            inputFile = voicebank.resolve("oto.ini").absolutePath,
            encoding = "Shift-JIS",
        ).createProject()

        assertEquals(cacheDirectory.absolutePath, project.cacheDirectory.absolutePath)
        assertEquals(false, project.isUsingDefaultCacheDirectory)
    }

    @Test
    fun testCreateWithNullEncodingUsesDefaultCharset() {
        val voicebank = deployOtoVoicebank()

        val project = otoRequest(
            projectFile = voicebank.resolve("project.lbp").absolutePath,
            sampleDirectory = voicebank.absolutePath,
            inputFile = voicebank.resolve("oto.ini").absolutePath,
            encoding = null,
        ).createProject()

        assertEquals(Charset.defaultCharset().name(), project.encoding)
    }

    @Test
    fun testCreateWithUnknownEncodingFallsBackToDefaultCharset() {
        val voicebank = deployOtoVoicebank()

        val project = otoRequest(
            projectFile = voicebank.resolve("project.lbp").absolutePath,
            sampleDirectory = voicebank.absolutePath,
            inputFile = voicebank.resolve("oto.ini").absolutePath,
            encoding = "x-no-such-encoding",
        ).createProject()

        assertEquals(Charset.defaultCharset().name(), project.encoding)
    }

    @Test
    fun testCreateFailsWhenWorkingDirectoryDoesNotExist() {
        val voicebank = deployOtoVoicebank()

        val request = otoRequest(
            projectFile = tempDir.resolve("missing").resolve("project.lbp").absolutePath,
            sampleDirectory = voicebank.absolutePath,
        )

        val t = assertFailsWith<IllegalArgumentException> { runBlocking { request.create() } }
        assertTrue(requireNotNull(t.message).startsWith("Working directory"))
    }

    @Test
    fun testCreateFailsWhenSampleDirectoryDoesNotExist() {
        val request = otoRequest(
            projectFile = tempDir.resolve("project.lbp").absolutePath,
            sampleDirectory = tempDir.resolve("missing").absolutePath,
        )

        val t = assertFailsWith<IllegalArgumentException> { runBlocking { request.create() } }
        assertTrue(requireNotNull(t.message).startsWith("Sample directory"))
    }

    @Test
    fun testCreateFailsWhenCacheDirectoryParentDoesNotExist() {
        val voicebank = deployOtoVoicebank()

        val request = otoRequest(
            projectFile = voicebank.resolve("project.lbp").absolutePath,
            sampleDirectory = voicebank.absolutePath,
            cacheDirectory = tempDir.resolve("missing").resolve("caches").absolutePath,
        )

        val t = assertFailsWith<IllegalArgumentException> { runBlocking { request.create() } }
        assertTrue(requireNotNull(t.message).contains("does not exist"))
    }
}
