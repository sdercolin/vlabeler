package io

import com.sdercolin.vlabeler.io.Sample
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [Sample.listSampleFiles].
 */
class SampleListTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testAcceptedExtensionsAreListed() {
        val accepted = listOf("a.wav", "b.mp3", "c.flac", "d.ogg", "e.m4a")
        accepted.forEach { tempDir.resolve(it).writeText("dummy") }

        val actual = Sample.listSampleFiles(tempDir).map { it.name }.sorted()

        assertEquals(accepted, actual)
    }

    @Test
    fun testOtherFilesAreExcluded() {
        tempDir.resolve("a.wav").writeText("dummy")
        tempDir.resolve("readme.txt").writeText("dummy")
        tempDir.resolve("noextension").writeText("dummy")
        tempDir.resolve("project.lbp").writeText("dummy")

        val actual = Sample.listSampleFiles(tempDir).map { it.name }

        assertEquals(listOf("a.wav"), actual)
    }

    @Test
    fun testNonExistentDirectoryReturnsEmptyList() {
        val actual = Sample.listSampleFiles(tempDir.resolve("not-existing"))

        assertEquals(emptyList(), actual)
    }
}
