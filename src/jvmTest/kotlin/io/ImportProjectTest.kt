package io

import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.io.importModulesFromProject
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [importModulesFromProject].
 */
class ImportProjectTest {

    @Test
    fun testNormal() {
        val json = """
            {
                "modules": [
                    {
                        "name": "module1",
                        "entries": [
                            {
                                "sample": "sample1",
                                "name": "entry1",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"]
                            },
                            {
                                "sample": "sample2",
                                "name": "entry2",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"],
                                "notes": {
                                    "done": false,
                                    "star": true,
                                    "tag": "tag1"
                                }
                            }
                        ]
                    },
                    {
                        "name": "module2",
                        "entries": [
                            {
                                "sample": "sample1",
                                "name": "entry1",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"]
                            },
                            {
                                "sample": "sample2",
                                "name": "entry2",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"],
                                "meta": {
                                    "done": false,
                                    "star": true,
                                    "tag": "tag1"
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        val expected = listOf(
            ImportedModule(
                name = "module1",
                entries = listOf(
                    Entry(
                        sample = "sample1",
                        name = "entry1",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                    ),
                    Entry(
                        sample = "sample2",
                        name = "entry2",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                        notes = EntryNotes(
                            star = true,
                            tag = "tag1",
                        ),
                    ),
                ),
                pointSize = 2,
                extraSize = 2,
            ),
            ImportedModule(
                name = "module2",
                entries = listOf(
                    Entry(
                        sample = "sample1",
                        name = "entry1",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                    ),
                    Entry(
                        sample = "sample2",
                        name = "entry2",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                        notes = EntryNotes(
                            star = true,
                            tag = "tag1",
                        ),
                    ),
                ),
                pointSize = 2,
                extraSize = 2,
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual.getOrThrow())
    }

    @Test
    fun testEntriesDirectlyUnderProject() {
        val json = """
            {
                "entries": [
                    {
                        "sample": "sample1",
                        "name": "entry1",
                        "start": 1,
                        "end": 2,
                        "points": [1, 2],
                        "extras": ["a", "b"]
                    },
                    {
                        "sample": "sample2",
                        "name": "entry2",
                        "start": 1,
                        "end": 2,
                        "points": [1, 2],
                        "extras": ["a", "b"],
                        "notes": {
                            "done": false,
                            "star": true,
                            "tag": "tag1"
                        }
                    }
                ]
            }
        """.trimIndent()
        val expected = listOf(
            ImportedModule(
                name = "",
                entries = listOf(
                    Entry(
                        sample = "sample1",
                        name = "entry1",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                    ),
                    Entry(
                        sample = "sample2",
                        name = "entry2",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                        notes = EntryNotes(
                            star = true,
                            tag = "tag1",
                        ),
                    ),
                ),
                pointSize = 2,
                extraSize = 2,
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual.getOrThrow())
    }

    @Test
    fun testBroken() {
        val json = """
            {
                "modules": [
                    {
                        "entries": []
                    },
                    {
                        "name": "module0",
                        "entries": []
                    },
                    {
                        "name": "module1",
                        "entries": [
                            {
                                "sample": "sample1",
                                "name": "entry1",
                                "start": 1,
                                "end": 2,
                                "points": [1],
                                "extras": ["a", "b"]
                            },
                            {
                                "sample": "sample2",
                                "name": "entry2",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"],
                                "notes": {
                                    "done": false,
                                    "star": true,
                                    "tag": "tag1"
                                }
                            }
                        ]
                    },
                    {
                        "name": "module2",
                        "entries": [
                            {
                                "sample": "sample1",
                                "name": "entry1",
                                "start": 1,
                                "points": [1, 2],
                                "extras": ["a", "b"]
                            },
                            {
                                "sample": "sample2",
                                "name": "entry2",
                                "start": 1,
                                "end": 2,
                                "points": [1, 2],
                                "extras": ["a", "b"],
                                "meta": {
                                    "done": false,
                                    "star": true,
                                    "tag": "tag1"
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()
        val expected = listOf(
            ImportedModule(
                name = "module2",
                entries = listOf(
                    Entry(
                        sample = "sample2",
                        name = "entry2",
                        start = 1f,
                        end = 2f,
                        points = listOf(1f, 2f),
                        extras = listOf("a", "b"),
                        notes = EntryNotes(
                            star = true,
                            tag = "tag1",
                        ),
                    ),
                ),
                pointSize = 2,
                extraSize = 2,
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual.getOrThrow())
    }
}
