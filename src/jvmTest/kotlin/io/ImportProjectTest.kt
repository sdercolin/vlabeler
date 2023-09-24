package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.io.importModulesFromProject
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [importModulesFromProject].
 */
class ImportProjectTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun testNormal() {
        val json = """
            {
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
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
                                "notes": {
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
                continuous = false,
                extension = "ini",
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
                continuous = false,
                extension = "ini",
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual)
    }

    @Test
    fun testEntriesDirectlyUnderProject() {
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
                continuous = false,
                extension = "ini",
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual)
    }

    @Test
    fun testBroken() {
        val json = """
            {                
                "labelerConf": {
                    "continuous": false,
                    "extension": "ini"
                },
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
                                "notes": {
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
                continuous = false,
                extension = "ini",
            ),
        )

        val actual = importModulesFromProject(json)

        assertEquals(expected, actual)
    }
}
