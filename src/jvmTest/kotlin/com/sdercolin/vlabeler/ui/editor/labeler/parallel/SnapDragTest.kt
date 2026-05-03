package com.sdercolin.vlabeler.ui.editor.labeler.parallel

import com.sdercolin.vlabeler.model.EmbeddedScripts
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapDragTest {

    private val entryConverter = EntryConverter(sampleRate = 44100f, resolution = 1)

    private fun minimalLabelerConf() = LabelerConf(
        name = "test",
        extension = "txt",
        author = "test",
        defaultValues = listOf(0f, 0f),
        parser = LabelerConf.Parser(
            scope = LabelerConf.Scope.Entry,
            scripts = EmbeddedScripts(null, listOf("")),
        ),
        writer = LabelerConf.Writer(),
    )

    private fun makeEntry(sample: String, start: Float, end: Float, index: Int = 0) =
        Entry(sample = sample, name = "e$index", start = start, end = end, points = listOf(), extras = listOf())

    private fun makeModule(name: String, entries: List<Entry>, rawFilePath: String = "/shared.TextGrid") =
        Module(
            name = name,
            sampleDirectoryPath = "/samples",
            entries = entries,
            currentIndex = 0,
            rawFilePath = rawFilePath,
        )

    private fun makeProject(vararg modules: Module) =
        Project(
            rootSampleDirectoryPath = "/",
            workingDirectoryPath = "/work",
            projectName = "test",
            cacheDirectoryPath = "/cache",
            originalLabelerConf = minimalLabelerConf(),
            modules = modules.toList(),
            currentModuleIndex = 0,
            autoExport = false,
        )

    @Test
    fun getSnapTargetsReturnsEmptyWhenNoParallelModules() {
        val module = makeModule(
            "tier1",
            listOf(
                makeEntry("a.wav", 0f, 100f),
                makeEntry("a.wav", 100f, 200f),
            ),
        )
        val project = makeProject(module)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        val targets = snapDrag.getSnapTargets(50f)
        assertTrue(targets.isEmpty(), "No parallel modules means no snap targets")
    }

    @Test
    fun getSnapTargetsReturnsTargetsFromParallelModule() {
        // Two modules sharing the same rawFilePath => parallel
        val entries1 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 200f, 1),
        )
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 150f, 0),
            makeEntry("a.wav", 150f, 300f, 1),
        )
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val project = makeProject(mod1, mod2)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        // The border in mod2 is at entry[0].end = 150ms
        // Convert to pixel to find the snap position
        val borderPx = entryConverter.convertToPixel(150f)

        val targets = snapDrag.getSnapTargets(borderPx)
        assertEquals(1, targets.size, "Should find one target (from tier2)")
        assertEquals("tier2", targets[0].moduleName)
        assertEquals(0, targets[0].entryIndex)
    }

    @Test
    fun getSnapTargetsReturnsMultipleTargetsFromMultipleParallelModules() {
        val entries1 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 200f, 1),
        )
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 250f, 1),
        )
        val entries3 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 300f, 1),
        )
        // All three share the same rawFilePath
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val mod3 = makeModule("tier3", entries3)
        val project = makeProject(mod1, mod2, mod3)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        // All borders at 100ms in parallel modules (tier2, tier3)
        val borderPx = entryConverter.convertToPixel(100f)

        val targets = snapDrag.getSnapTargets(borderPx)
        assertEquals(2, targets.size, "Should find targets from both tier2 and tier3")
        val moduleNames = targets.map { it.moduleName }.toSet()
        assertTrue(moduleNames.contains("tier2"))
        assertTrue(moduleNames.contains("tier3"))
    }

    @Test
    fun getSnapTargetsDeduplicatesPerModuleWhenMultipleEntriesMatch() {
        // Module with multiple borders near the same position — only closest per module
        val entries1 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 200f, 1),
            makeEntry("a.wav", 200f, 300f, 2),
        )
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 200f, 1),
        )
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val project = makeProject(mod1, mod2)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        val borderPx = entryConverter.convertToPixel(100f)
        val targets = snapDrag.getSnapTargets(borderPx)
        // Only one target per module
        assertEquals(1, targets.size)
        assertEquals("tier2", targets[0].moduleName)
        assertEquals(0, targets[0].entryIndex)
    }

    @Test
    fun getSnapTargetsReturnsEmptyWhenPositionFarFromAllBorders() {
        val entries1 = listOf(
            makeEntry("a.wav", 0f, 100f, 0),
            makeEntry("a.wav", 100f, 200f, 1),
        )
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 500f, 0),
            makeEntry("a.wav", 500f, 1000f, 1),
        )
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val project = makeProject(mod1, mod2)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        // Query at a position very far from 500ms border
        val targets = snapDrag.getSnapTargets(10f)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun snapReturnsSnappedPositionWithinDistance() {
        val entries1 = listOf(makeEntry("a.wav", 0f, 100f, 0))
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 150f, 0),
            makeEntry("a.wav", 150f, 300f, 1),
        )
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val project = makeProject(mod1, mod2)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        val borderPx = entryConverter.convertToPixel(150f)
        // Query within 10px of border
        val result = snapDrag.snap(borderPx + 5f)
        assertEquals(borderPx, result, "Should snap to the border position")
    }

    @Test
    fun snapReturnsOriginalPositionWhenTooFarFromAnyBorder() {
        val entries1 = listOf(makeEntry("a.wav", 0f, 100f, 0))
        val entries2 = listOf(
            makeEntry("a.wav", 0f, 150f, 0),
            makeEntry("a.wav", 150f, 300f, 1),
        )
        val mod1 = makeModule("tier1", entries1)
        val mod2 = makeModule("tier2", entries2)
        val project = makeProject(mod1, mod2)
        val snapDrag = SnapDrag(project, 10000f, entryConverter)

        val borderPx = entryConverter.convertToPixel(150f)
        // Query at 100px from border — beyond SNAP_DISTANCE=10
        val farPosition = borderPx + 100f
        val result = snapDrag.snap(farPosition)
        assertEquals(farPosition, result, "Should not snap when too far")
    }
}
