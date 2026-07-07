package model

import androidx.compose.runtime.mutableStateOf
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
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
import kotlin.test.assertTrue

class ProjectHistoryTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun history(conf: AppConf = AppConf()) = ProjectHistory(mutableStateOf(conf))

    private fun Project.withTag(tag: String) = updateCurrentModule { editEntryTag(0, tag) }

    private fun Project.withCurrentIndex(index: Int) = updateCurrentModule { copy(currentIndex = index) }

    @Test
    fun `new initializes the stack`() {
        val history = history()
        history.new(baseProject)
        assertEquals(baseProject, history.current)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `push adds a new state`() {
        val history = history()
        history.new(baseProject)
        val edited = baseProject.withTag("edited")
        history.push(edited)
        assertEquals(edited, history.current)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `push deduplicates equal content`() {
        val history = history()
        history.new(baseProject)
        history.push(baseProject.copy())
        assertEquals(baseProject, history.current)
        assertFalse(history.canUndo)
    }

    @Test
    fun `push squashes index-only changes by default`() {
        val history = history()
        history.new(baseProject)
        history.push(baseProject.withCurrentIndex(1))
        // with the default `squashIndex == true`, an index-only change is not pushed
        assertEquals(baseProject, history.current)
        assertFalse(history.canUndo)
    }

    @Test
    fun `push keeps index-only changes when squash is disabled`() {
        val history = history(AppConf(history = AppConf.History(squashIndex = false)))
        history.new(baseProject)
        val indexChanged = baseProject.withCurrentIndex(1)
        history.push(indexChanged)
        assertEquals(indexChanged, history.current)
        assertTrue(history.canUndo)
    }

    @Test
    fun `undo and redo move over pushed states`() {
        val history = history()
        val first = baseProject.withTag("first")
        val second = baseProject.withTag("second")
        history.new(baseProject)
        history.push(first)
        history.push(second)

        history.undo()
        assertEquals(first, history.current)
        assertTrue(history.canUndo)
        assertTrue(history.canRedo)

        history.undo()
        assertEquals(baseProject, history.current)
        assertFalse(history.canUndo)

        // undo without available history is a no-op
        history.undo()
        assertEquals(baseProject, history.current)

        history.redo()
        assertEquals(first, history.current)
        history.redo()
        assertEquals(second, history.current)
        assertFalse(history.canRedo)

        // redo without available history is a no-op
        history.redo()
        assertEquals(second, history.current)
    }

    @Test
    fun `push after undo truncates the redo states`() {
        val history = history()
        val first = baseProject.withTag("first")
        val second = baseProject.withTag("second")
        val third = baseProject.withTag("third")
        history.new(baseProject)
        history.push(first)
        history.push(second)
        history.undo()
        history.undo()

        history.push(third)
        assertEquals(third, history.current)
        assertFalse(history.canRedo)
        history.undo()
        assertEquals(baseProject, history.current)
        assertFalse(history.canUndo)
    }

    @Test
    fun `push evicts the oldest state when max size is reached`() {
        val history = history(AppConf(history = AppConf.History(maxSize = 2)))
        val first = baseProject.withTag("first")
        val second = baseProject.withTag("second")
        history.new(baseProject)
        history.push(first)
        history.push(second)

        assertEquals(second, history.current)
        history.undo()
        assertEquals(first, history.current)
        // the initial state has been evicted
        assertFalse(history.canUndo)
    }

    @Test
    fun `replaceTop replaces the current state without adding`() {
        val history = history()
        val first = baseProject.withTag("first")
        val replaced = baseProject.withTag("replaced")
        history.new(baseProject)
        history.push(first)

        history.replaceTop(replaced)
        assertEquals(replaced, history.current)
        assertTrue(history.canUndo)

        history.undo()
        assertEquals(baseProject, history.current)
        history.redo()
        assertEquals(replaced, history.current)
    }

    @Test
    fun `clear empties the stack`() {
        val history = history()
        history.new(baseProject)
        history.push(baseProject.withTag("first"))

        history.clear()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        val next = baseProject.withTag("next")
        history.new(next)
        assertEquals(next, history.current)
        assertFalse(history.canUndo)
    }

    companion object {

        private val tempDir: File by lazy {
            createTempDirectory("vlabeler-test").toFile().also { dir ->
                Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
            }
        }

        /**
         * A real project (oto fixture, 2 entries) created once and shared by all tests, since [Project] is immutable.
         */
        private val baseProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "oto",
                tempDir.resolve("oto"),
                wavFiles = listOf("_a_ka.wav"),
            )
            createTestProject(
                labeler = TestLabelers.utauOto,
                sampleDirectory = sampleDir,
                inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
            )
        }
    }
}
