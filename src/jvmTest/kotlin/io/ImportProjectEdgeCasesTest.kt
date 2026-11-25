package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.io.importModulesFromProject
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.Project
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the edge cases of [importModulesFromProject], [ImportedModule.validate] and
 * [ImportedModule.isCompatibleWith], complementing [ImportProjectTest].
 */
class ImportProjectEdgeCasesTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun entryJson(name: String, points: String = "[1, 2]", extras: String = """["a"]""") = """
        {
            "sample": "sample1",
            "name": "$name",
            "start": 1,
            "end": 2,
            "points": $points,
            "extras": $extras
        }
    """.trimIndent()

    @Test
    fun testInvalidJsonThrows() {
        // malformed input is a structural failure, so the caller can surface it instead of silently importing nothing
        assertFailsWith<Exception> { importModulesFromProject("not a json") }
    }

    @Test
    fun testMissingLabelerConfThrows() {
        val json = """
            {
                "modules": [
                    {
                        "name": "module1",
                        "entries": [${entryJson("entry1")}]
                    }
                ]
            }
        """.trimIndent()

        assertFailsWith<Exception> { importModulesFromProject(json) }
    }

    @Test
    fun testNoEntriesAndNoModulesReturnsEmptyList() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                }
            }
        """.trimIndent()

        assertEquals(emptyList(), importModulesFromProject(json))
    }

    @Test
    fun testModulesNotAnArrayReturnsEmptyList() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
                "modules": {
                    "name": "module1"
                }
            }
        """.trimIndent()

        assertEquals(emptyList(), importModulesFromProject(json))
    }

    @Test
    fun testEmptyLegacyEntryArrayReturnsEmptyList() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
                "entries": []
            }
        """.trimIndent()

        assertEquals(emptyList(), importModulesFromProject(json))
    }

    @Test
    fun testDuplicateModuleNamesKeepFirst() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
                "modules": [
                    {
                        "name": "dup",
                        "entries": [${entryJson("first")}]
                    },
                    {
                        "name": "dup",
                        "entries": [${entryJson("second")}]
                    }
                ]
            }
        """.trimIndent()

        val actual = importModulesFromProject(json)

        assertEquals(1, actual.size)
        assertEquals("dup", actual.single().name)
        assertEquals(listOf("first"), actual.single().entries.map { it.name })
    }

    @Test
    fun testLegacyEntriesAndModulesAreBothImported() {
        val json = """
            {
                "labelerConf": {
                    "continuous": true,
                    "extension": "lab"
                },
                "entries": [${entryJson("legacy")}],
                "modules": [
                    {
                        "name": "module1",
                        "entries": [${entryJson("modern")}]
                    }
                ]
            }
        """.trimIndent()

        val actual = importModulesFromProject(json)

        assertEquals(listOf("", "module1"), actual.map { it.name })
        assertEquals(listOf("legacy"), actual[0].entries.map { it.name })
        assertEquals(listOf("modern"), actual[1].entries.map { it.name })
        actual.forEach {
            assertEquals(true, it.continuous)
            assertEquals("lab", it.extension)
        }
    }

    @Test
    fun testNotesAreImported() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
                "entries": [
                    {
                        "sample": "sample1",
                        "name": "entry1",
                        "start": 1,
                        "end": 2,
                        "points": [1, 2],
                        "extras": ["a"],
                        "notes": {
                            "done": true,
                            "star": true,
                            "tag": "some-tag"
                        }
                    }
                ]
            }
        """.trimIndent()

        val actual = importModulesFromProject(json)

        assertEquals(
            EntryNotes(done = true, star = true, tag = "some-tag"),
            actual.single().entries.single().notes,
        )
    }

    @Test
    fun testLegacyMetaNotesAreImported() {
        // "meta" is the legacy name of "notes"; it is stripped before strict entry deserialization and mapped to
        // the notes of the imported entry
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
                "entries": [
                    {
                        "sample": "sample1",
                        "name": "entry1",
                        "start": 1,
                        "end": 2,
                        "points": [1, 2],
                        "extras": ["a"],
                        "meta": {
                            "done": true,
                            "star": false,
                            "tag": "legacy-tag"
                        }
                    }
                ]
            }
        """.trimIndent()

        val actual = importModulesFromProject(json)

        assertEquals(
            EntryNotes(done = true, star = false, tag = "legacy-tag"),
            actual.single().entries.single().notes,
        )
    }

    @Test
    fun testValidateThrowsOnInconsistentSizes() {
        val module = ImportedModule(
            name = "module1",
            entries = listOf(
                Entry(
                    sample = "sample1",
                    name = "entry1",
                    start = 1f,
                    end = 2f,
                    points = listOf(1f),
                    extras = listOf("a"),
                ),
            ),
            pointSize = 2,
            extraSize = 1,
            continuous = false,
            extension = "ini",
        )

        assertFailsWith<IllegalArgumentException> { module.validate() }
    }

    @Test
    fun testIsCompatibleWith() {
        val tempDir = createTempDirectory("vlabeler-test").toFile()
        try {
            val project = createOtoProject(tempDir)
            val labeler = project.labelerConf
            val compatible = importedModuleOf(
                pointCount = labeler.fields.size,
                extraCount = labeler.extraFields.size,
                continuous = labeler.continuous,
                extension = labeler.extension,
            )

            assertTrue(compatible.isCompatibleWith(project))
            assertFalse(compatible.copy(pointSize = labeler.fields.size + 1).isCompatibleWith(project))
            assertFalse(compatible.copy(extraSize = labeler.extraFields.size + 1).isCompatibleWith(project))
            assertFalse(compatible.copy(continuous = labeler.continuous.not()).isCompatibleWith(project))
            assertFalse(compatible.copy(extension = "txt").isCompatibleWith(project))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun createOtoProject(tempDir: File): Project {
        val voicebank = TestFixtures.deploy(
            "oto",
            tempDir.resolve("voice"),
            wavFiles = listOf("_a_ka.wav"),
        )
        return createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = voicebank,
            inputFilePath = voicebank.resolve("oto.ini").absolutePath,
        )
    }

    private fun importedModuleOf(
        pointCount: Int,
        extraCount: Int,
        continuous: Boolean,
        extension: String,
    ) = ImportedModule(
        name = "imported",
        entries = listOf(
            Entry(
                sample = "sample1",
                name = "entry1",
                start = 1f,
                end = 2f,
                points = List(pointCount) { it.toFloat() },
                extras = List(extraCount) { "extra$it" },
            ),
        ),
        pointSize = pointCount,
        extraSize = extraCount,
        continuous = continuous,
        extension = extension,
    )
}
