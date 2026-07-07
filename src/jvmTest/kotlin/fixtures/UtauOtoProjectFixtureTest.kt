package fixtures

import com.sdercolin.vlabeler.env.Log
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Smoke test: creates a project from the `oto` fixture with the bundled `oto-labeler` (single oto.ini file mode).
 */
class UtauOtoProjectFixtureTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    @Test
    fun testCreateProject() {
        val sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = sampleDir,
            inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
        )

        assertEquals(1, project.modules.size)
        val module = project.modules.single()
        assertEquals(listOf("- a", "a ka"), module.entries.map { it.name })

        // _a_ka.wav=a ka,450.0,120.0,-350.0,100.0,40.0
        val entry = module.entries.first { it.name == "a ka" }
        assertEquals("_a_ka.wav", entry.sample)
        assertEquals(450f, entry.start)
        assertEquals(800f, entry.end)
        assertEquals(listOf(570f, 550f, 490f, 450f), entry.points)
    }
}
