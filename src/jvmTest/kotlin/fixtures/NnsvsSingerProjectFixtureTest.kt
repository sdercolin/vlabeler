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
import kotlin.test.assertTrue

/**
 * Smoke test: creates a project from the `nnsvs-singer` fixture with the bundled `nnsvs-singer-labeler`, going
 * through the real project constructor and lab parser scripts.
 */
class NnsvsSingerProjectFixtureTest {

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
            "nnsvs-singer",
            tempDir.resolve("nnsvs-singer"),
            wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
            wavDurationMs = 1500,
        )
        val project = createTestProject(
            labeler = TestLabelers.nnsvsSinger,
            sampleDirectory = sampleDir,
        )

        // the labeler is continuous, so the multiple edit mode is enabled by default
        assertTrue(project.multipleEditMode)

        // one module per wav file
        assertEquals(listOf("doremi", "legato"), project.modules.map { it.name }.sorted())

        val doremi = project.modules.first { it.name == "doremi" }
        assertEquals(listOf("pau", "d", "o", "r", "e", "pau"), doremi.entries.map { it.name })
        assertTrue(doremi.entries.all { it.sample == "doremi.wav" })

        // lab times are in 100 ns units and are converted to ms
        assertEquals(0f, doremi.entries.first().start)
        assertEquals(250f, doremi.entries.first().end)
        assertEquals(1200f, doremi.entries.last().start)
        assertEquals(1500f, doremi.entries.last().end)

        // entries of a continuous labeler are connected
        doremi.entries.zipWithNext().forEach { (previous, next) ->
            assertEquals(previous.end, next.start)
        }

        val legato = project.modules.first { it.name == "legato" }
        assertEquals(listOf("pau", "a", "pau"), legato.entries.map { it.name })
    }
}
