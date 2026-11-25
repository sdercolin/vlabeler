package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.saveBackupProjectFile
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
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
 * Tests for [saveBackupProjectFile].
 */
class ProjectBackupTest {

    private lateinit var tempDir: File
    private lateinit var project: Project

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
        val sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
        project = createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = sampleDir,
            inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
        )
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private val backupDir get() = project.workingDirectory.resolve("backups")

    private fun listBackups() = backupDir.listFiles().orEmpty().sortedBy { it.lastModified() }

    @Test
    fun testCreatesBackupFile() {
        runBlocking { saveBackupProjectFile(project, maxFileCount = 5) }
        val backups = listBackups()
        assertEquals(1, backups.size)
        assertEquals(project.stringifyJson(), backups.single().readText())
    }

    @Test
    fun testSkipsBackupWhenContentUnchanged() {
        runBlocking {
            saveBackupProjectFile(project, maxFileCount = 5)
            saveBackupProjectFile(project, maxFileCount = 5)
        }
        assertEquals(1, listBackups().size)
    }

    @Test
    fun testRotationKeepsMaxFileCount() {
        runBlocking {
            repeat(3) { index ->
                val modified = project.copy(currentModuleIndex = 0, autoExport = index % 2 == 0)
                saveBackupProjectFile(modified.copy(projectName = project.projectName), maxFileCount = 2)
                // the backup file name has millisecond resolution; avoid name collisions between iterations
                Thread.sleep(5)
            }
        }
        assertEquals(2, listBackups().size)
    }

    @Test
    fun testDisabledWhenMaxFileCountIsZero() {
        runBlocking { saveBackupProjectFile(project, maxFileCount = 0) }
        assertFalse(backupDir.exists())
    }
}
