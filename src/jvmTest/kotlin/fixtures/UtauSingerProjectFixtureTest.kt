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
import kotlin.test.assertFalse

/**
 * Smoke test: creates a project from the `utau-singer` fixture with the bundled `utau-singer-labeler`, going through
 * the real project constructor and oto parser scripts.
 */
class UtauSingerProjectFixtureTest {

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
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )

        // one module per pitch folder containing samples; the root folder is excluded by default
        assertEquals(listOf("A3", "C4"), project.modules.map { it.name }.sorted())

        val c4 = project.modules.first { it.name == "C4" }
        assertEquals(4, c4.entries.size)

        // _a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0
        val first = c4.entries.first()
        assertEquals("_a_ka.wav", first.sample)
        assertEquals("- a", first.name)
        assertEquals(10f, first.start)
        assertEquals(410f, first.end) // cutoff -400 means 400ms after offset
        // points: [fixed, preutterance, overlap, offset] as absolute positions
        assertEquals(listOf(110f, 90f, 40f, 10f), first.points)
        assertFalse(first.needSync)

        // _i_ki.wav=i ki,450.0,120.0,-350.0,100.0,-20.0: negative overlap shifts the start
        val negativeOvl = c4.entries.first { it.name == "i ki" }
        assertEquals(430f, negativeOvl.start)
        assertEquals(800f, negativeOvl.end)
        assertEquals(listOf(570f, 550f, 430f, 450f), negativeOvl.points)

        val a3 = project.modules.first { it.name == "A3" }
        assertEquals(listOf("- aA3", "a kaA3"), a3.entries.map { it.name })
    }
}
