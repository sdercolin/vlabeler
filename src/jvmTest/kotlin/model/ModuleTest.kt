package model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.model.postApplyLabelerConf
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import testutil.TestLabelers
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private val nonContinuousLabeler get() = TestLabelers.utauOto
    private val continuousLabeler get() = TestLabelers.nnsvsSinger

    private fun entry(
        sample: String,
        name: String,
        start: Float = 0f,
        end: Float = 100f,
        points: List<Float> = emptyList(),
        extras: List<String?> = emptyList(),
    ) = Entry(sample = sample, name = name, start = start, end = end, points = points, extras = extras)

    private fun module(
        entries: List<Entry>,
        currentIndex: Int = 0,
        name: String = "",
        rawFilePath: String? = null,
    ) = Module(
        name = name,
        sampleDirectoryPath = "",
        entries = entries,
        currentIndex = currentIndex,
        rawFilePath = rawFilePath,
    )

    /**
     * 5 entries in 3 samples: a.wav -> [0, 1], b.wav -> [2, 3], c.wav -> [4].
     */
    private val navModule = module(
        listOf(
            entry("a.wav", "e0"),
            entry("a.wav", "e1"),
            entry("b.wav", "e2"),
            entry("b.wav", "e3"),
            entry("c.wav", "e4"),
        ),
    )

    /**
     * 3 continuous entries in a single sample.
     */
    private val continuousModule = module(
        listOf(
            entry("d.wav", "a", 0f, 100f),
            entry("d.wav", "b", 100f, 200f),
            entry("d.wav", "c", 200f, 300f),
        ),
    )

    @Test
    fun `nextEntry and previousEntry move by one and clamp at borders`() {
        assertEquals(1, navModule.nextEntry().currentIndex)
        assertEquals(0, navModule.previousEntry().currentIndex)
        assertEquals(4, navModule.copy(currentIndex = 4).nextEntry().currentIndex)
        assertEquals(3, navModule.copy(currentIndex = 4).previousEntry().currentIndex)
    }

    @Test
    fun `entry navigation skips filtered-out entries`() {
        val filtered = navModule.copy(filteredEntryIndexes = listOf(0, 2, 4))
        assertEquals(2, filtered.nextEntry().currentIndex)
        assertEquals(2, filtered.copy(currentIndex = 4).previousEntry().currentIndex)
        assertEquals(0, filtered.previousEntry().currentIndex)
    }

    @Test
    fun `nextSample moves to the first entry of the next sample`() {
        assertEquals(2, navModule.nextSample().currentIndex)
        assertEquals(4, navModule.copy(currentIndex = 2).nextSample().currentIndex)
        // in the last sample: move to the last entry of the same sample
        assertEquals(4, navModule.copy(currentIndex = 4).nextSample().currentIndex)
    }

    @Test
    fun `previousSample moves to the last entry of the previous sample`() {
        assertEquals(1, navModule.copy(currentIndex = 3).previousSample().currentIndex)
        // in the first sample: move to the first entry of the same sample
        assertEquals(0, navModule.copy(currentIndex = 1).previousSample().currentIndex)
    }

    @Test
    fun `updateEntries replaces entries without touching neighbors when not continuous`() {
        val edited = IndexedEntry(entry("a.wav", "e1-edited", 10f, 90f), 1)
        val result = navModule.updateEntries(listOf(edited), nonContinuousLabeler)
        assertEquals(edited.entry, result.entries[1])
        assertEquals(navModule.entries[0], result.entries[0])
        assertEquals(navModule.entries[2], result.entries[2])
    }

    @Test
    fun `updateEntries adjusts neighbor borders when continuous`() {
        val edited = IndexedEntry(entry("d.wav", "b", 110f, 190f), 1)
        val result = continuousModule.updateEntries(listOf(edited), continuousLabeler)
        assertEquals(110f, result.entries[0].end)
        assertEquals(edited.entry, result.entries[1])
        assertEquals(190f, result.entries[2].start)
    }

    @Test
    fun `updateCurrentEntry edits the entry at currentIndex`() {
        val result = continuousModule.copy(currentIndex = 1)
            .updateCurrentEntry(entry("d.wav", "b2", 120f, 180f), continuousLabeler)
        assertEquals("b2", result.entries[1].name)
        assertEquals(120f, result.entries[0].end)
        assertEquals(180f, result.entries[2].start)
    }

    @Test
    fun `renameEntry only changes the name`() {
        val result = navModule.renameEntry(2, "renamed", nonContinuousLabeler)
        assertEquals("renamed", result.entries[2].name)
        assertEquals(navModule.entries[2].copy(name = "renamed"), result.entries[2])
    }

    @Test
    fun `updateEntryExtra only changes the extras`() {
        val original = module(listOf(entry("a.wav", "e0", extras = listOf("500"))))
        val result = original.updateEntryExtra(0, listOf("600"), nonContinuousLabeler)
        assertEquals(listOf<String?>("600"), result.entries[0].extras)
    }

    @Test
    fun `duplicateEntry inserts a copy after the original`() {
        val result = navModule.duplicateEntry(1, "e1-copy", nonContinuousLabeler)
        assertEquals(6, result.entries.size)
        assertEquals(navModule.entries[1], result.entries[1])
        assertEquals(navModule.entries[1].copy(name = "e1-copy"), result.entries[2])
        assertEquals(1, result.currentIndex)
    }

    @Test
    fun `duplicateEntry splits the entry at the middle when continuous`() {
        val result = continuousModule.duplicateEntry(1, "b2", continuousLabeler)
        assertEquals(4, result.entries.size)
        assertEquals(150f, result.entries[1].end)
        assertEquals("b2", result.entries[2].name)
        assertEquals(150f, result.entries[2].start)
        assertEquals(200f, result.entries[2].end)
    }

    @Test
    fun `removeEntry removes and moves currentIndex to the previous entry`() {
        val result = navModule.copy(currentIndex = 2).removeEntry(2, nonContinuousLabeler)
        assertEquals(listOf("e0", "e1", "e3", "e4"), result.entries.map { it.name })
        assertEquals(1, result.currentIndex)
        assertEquals(0, navModule.removeEntry(0, nonContinuousLabeler).currentIndex)
    }

    @Test
    fun `removeEntry extends the previous entry when continuous`() {
        val result = continuousModule.removeEntry(1, continuousLabeler)
        assertEquals(listOf("a", "c"), result.entries.map { it.name })
        assertEquals(200f, result.entries[0].end)
        assertEquals(200f, result.entries[1].start)
    }

    @Test
    fun `removeEntries removes multiple entries`() {
        val result = navModule.removeEntries(listOf(0, 2), nonContinuousLabeler)
        assertEquals(listOf("e1", "e3", "e4"), result.entries.map { it.name })
    }

    @Test
    fun `cutEntry splits an entry at the given position`() {
        val original = module(
            listOf(entry("a.wav", "orig", 100f, 500f, points = listOf(150f, 450f, 300f, 200f))),
        )
        val result = original.cutEntry(0, 300f, rename = "left", newName = "right", targetEntryIndex = 1)
        assertEquals(2, result.entries.size)
        assertEquals(1, result.currentIndex)

        val first = result.entries[0]
        assertEquals("left", first.name)
        assertEquals(100f, first.start)
        assertEquals(300f, first.end)
        assertEquals(listOf(150f, 300f, 300f, 200f), first.points)
        assertTrue(first.notes.done)

        val second = result.entries[1]
        assertEquals("right", second.name)
        assertEquals(300f, second.start)
        assertEquals(500f, second.end)
        assertEquals(listOf(300f, 450f, 300f, 300f), second.points)
        assertTrue(second.notes.done)
    }

    @Test
    fun `toggle and batch edit entry notes`() {
        assertTrue(navModule.toggleEntryDone(0).entries[0].notes.done)
        assertFalse(navModule.toggleEntryDone(0).toggleEntryDone(0).entries[0].notes.done)
        assertTrue(navModule.toggleEntryStar(1).entries[1].notes.star)
        assertEquals("tag", navModule.editEntryTag(2, "tag").entries[2].notes.tag)

        val allDone = navModule.setEntriesDone(listOf(0, 2), done = true)
        assertEquals(listOf(true, false, true, false, false), allDone.entries.map { it.notes.done })

        val allStarred = navModule.setEntriesStar(listOf(1, 4), star = true)
        assertEquals(listOf(false, true, false, false, true), allStarred.entries.map { it.notes.star })

        val allTagged = navModule.editEntriesTag(listOf(0, 1), "v")
        assertEquals(listOf("v", "v", "", "", ""), allTagged.entries.map { it.notes.tag })
    }

    @Test
    fun `getEntriesForEditing returns a single entry in single edit mode`() {
        val result = navModule.copy(currentIndex = 2).getEntriesForEditing(null, multipleEditMode = false)
        assertEquals(listOf(IndexedEntry(navModule.entries[2], 2)), result)

        val explicit = navModule.getEntriesForEditing(4, multipleEditMode = false)
        assertEquals(listOf(IndexedEntry(navModule.entries[4], 4)), explicit)
    }

    @Test
    fun `getEntriesForEditing returns the whole sample group in multiple edit mode`() {
        val result = navModule.getEntriesForEditing(2, multipleEditMode = true)
        assertEquals(
            listOf(
                IndexedEntry(navModule.entries[2], 2),
                IndexedEntry(navModule.entries[3], 3),
            ),
            result,
        )
    }

    @Test
    fun `sample and raw file paths are resolved against the root sample directory`() {
        val root = File("/root/samples")
        val module = module(
            listOf(entry("a.wav", "e0")),
            name = "C4",
            rawFilePath = File("C4", "oto.ini").path,
        ).copy(sampleDirectoryPath = "C4")
        val project = Project(
            rootSampleDirectoryPath = root.path,
            workingDirectoryPath = ".",
            projectName = "test",
            cacheDirectoryPath = "cache",
            originalLabelerConf = nonContinuousLabeler,
            modules = listOf(module),
            currentModuleIndex = 0,
            autoExport = false,
        )
        val sampleDirectory = root.resolve("C4")
        assertEquals(sampleDirectory, module.getSampleDirectory(project))
        assertEquals(sampleDirectory.resolve("a.wav"), module.getSampleFile(project, "a.wav"))
        assertEquals(sampleDirectory.resolve("a.wav"), module.getCurrentSampleFile(project))
        assertEquals(sampleDirectory.resolve("oto.ini"), module.getRawFile(project))
        assertNull(module.copy(rawFilePath = null).getRawFile(project))
    }

    @Test
    fun `secondary constructor relativizes absolute paths against the root directory`() {
        val root = File("/root/samples")
        val module = Module(
            rootDirectory = root,
            name = "C4",
            sampleDirectory = root.resolve("C4"),
            entries = listOf(entry("a.wav", "e0")),
            currentIndex = 0,
            rawFilePath = root.resolve("C4").resolve("oto.ini"),
        )
        assertEquals("C4", module.sampleDirectoryPath)
        assertEquals(File("C4", "oto.ini").path, module.rawFilePath)

        // relative paths are kept as-is
        val relative = Module(
            rootDirectory = root,
            name = "C4",
            sampleDirectory = File("C4"),
            entries = listOf(entry("a.wav", "e0")),
            currentIndex = 0,
            rawFilePath = null,
        )
        assertEquals("C4", relative.sampleDirectoryPath)
        assertNull(relative.rawFilePath)
    }

    @Test
    fun `isParallelTo requires a shared non-null raw label file`() {
        val first = module(listOf(entry("a.wav", "e0")), name = "a", rawFilePath = "x.lab")
        val second = module(listOf(entry("b.wav", "e1")), name = "b", rawFilePath = "x.lab")
        assertTrue(first.isParallelTo(second))
        assertFalse(first.isParallelTo(first))
        assertFalse(
            first.copy(rawFilePath = null).isParallelTo(second.copy(rawFilePath = null)),
        )
    }

    @Test
    fun `updateOnLoadedSample resolves relative ends of the loaded sample`() {
        val sampleInfo = SampleInfo(
            name = "a.wav",
            file = "a.wav",
            moduleName = "",
            sampleRate = 44100f,
            maxSampleRate = 44100,
            normalize = false,
            normalizeRatio = null,
            channels = 1,
            length = 44100,
            lengthMillis = 1000f,
            chunkSize = 44100,
            chunkCount = 1,
            hasSpectrogram = false,
            hasPower = false,
            powerChannels = 0,
            hasFundamental = false,
            lastModified = 0L,
            algorithmVersion = 1,
        )
        val original = module(
            listOf(
                entry("a.wav", "e0", 0f, 0f).copy(needSync = true),
                entry("a.wav", "e1", 10f, -100f),
                entry("a.wav", "e2", 0f, 500f),
                entry("b.wav", "e3", 0f, 0f).copy(needSync = true),
            ),
        )
        val result = original.updateOnLoadedSample(sampleInfo)
        assertEquals(1000f, result.entries[0].end)
        assertFalse(result.entries[0].needSync)
        assertEquals(900f, result.entries[1].end)
        assertEquals(original.entries[2], result.entries[2])
        // entries of other samples are not touched
        assertEquals(original.entries[3], result.entries[3])
    }

    @Test
    fun `validate rejects an invalid currentIndex`() {
        val invalid = module(listOf(entry("a.wav", "e0")), currentIndex = 5)
        assertFailsWith<IllegalArgumentException> {
            invalid.validate(multipleEditMode = false, labelerConf = continuousLabeler)
        }
    }

    @Test
    fun `validate rejects multiple edit mode for non-continuous labelers`() {
        assertFailsWith<IllegalArgumentException> {
            navModule.validate(multipleEditMode = true, labelerConf = nonContinuousLabeler)
        }
    }

    @Test
    fun `validate rejects non-continuous entries for continuous labelers`() {
        val broken = module(
            listOf(
                entry("d.wav", "a", 0f, 100f),
                entry("d.wav", "b", 150f, 200f),
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            broken.validate(multipleEditMode = true, labelerConf = continuousLabeler)
        }
        // the continuous case passes
        continuousModule.validate(multipleEditMode = true, labelerConf = continuousLabeler)
    }

    @Test
    fun `validate rejects mismatched points or extras sizes`() {
        // the labeler requires 4 points and 1 extra
        val noPoints = module(listOf(entry("a.wav", "e0", extras = listOf("500"))))
        assertFailsWith<IllegalArgumentException> {
            noPoints.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        }
        val noExtras = module(listOf(entry("a.wav", "e0", points = listOf(1f, 2f, 3f, 4f))))
        assertFailsWith<IllegalArgumentException> {
            noExtras.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        }
        val nullExtra = module(
            listOf(entry("a.wav", "e0", points = listOf(1f, 2f, 3f, 4f), extras = listOf(null))),
        )
        assertFailsWith<IllegalArgumentException> {
            // the extra field of the labeler is not optional
            nullExtra.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        }
    }

    @Test
    fun `validate clamps a negative start to zero`() {
        val original = module(
            listOf(entry("a.wav", "e0", -50f, 500f, points = listOf(10f, 20f, 30f, 40f), extras = listOf("500"))),
        )
        val result = original.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        assertEquals(0f, result.entries[0].start)
    }

    @Test
    fun `validate sets end to start when start is greater than end`() {
        val original = module(
            listOf(
                entry("a.wav", "e0", 200f, 100f, points = listOf(200f, 200f, 200f, 200f), extras = listOf("500")),
            ),
        )
        val result = original.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        assertEquals(200f, result.entries[0].end)
    }

    @Test
    fun `validate handles points before start according to overflowBeforeStart`() {
        val original = module(
            listOf(entry("a.wav", "e0", 100f, 500f, points = listOf(90f, 400f, 300f, 200f), extras = listOf("500"))),
        )
        // the bundled labeler uses AdjustBorder
        val adjustedBorder = original.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        assertEquals(90f, adjustedBorder.entries[0].start)

        val adjustPointLabeler =
            nonContinuousLabeler.copy(overflowBeforeStart = LabelerConf.PointOverflow.AdjustPoint)
        val adjustedPoint = original.validate(multipleEditMode = false, labelerConf = adjustPointLabeler)
        assertEquals(100f, adjustedPoint.entries[0].start)
        assertEquals(listOf(100f, 400f, 300f, 200f), adjustedPoint.entries[0].points)

        val errorLabeler = nonContinuousLabeler.copy(overflowBeforeStart = LabelerConf.PointOverflow.Error)
        assertFailsWith<IllegalArgumentException> {
            original.validate(multipleEditMode = false, labelerConf = errorLabeler)
        }
    }

    @Test
    fun `validate handles points after end according to overflowAfterEnd`() {
        val original = module(
            listOf(entry("a.wav", "e0", 100f, 500f, points = listOf(550f, 400f, 300f, 200f), extras = listOf("500"))),
        )
        // the bundled labeler uses AdjustBorder
        val adjustedBorder = original.validate(multipleEditMode = false, labelerConf = nonContinuousLabeler)
        assertEquals(550f, adjustedBorder.entries[0].end)

        val adjustPointLabeler = nonContinuousLabeler.copy(overflowAfterEnd = LabelerConf.PointOverflow.AdjustPoint)
        val adjustedPoint = original.validate(multipleEditMode = false, labelerConf = adjustPointLabeler)
        assertEquals(500f, adjustedPoint.entries[0].end)
        assertEquals(listOf(500f, 400f, 300f, 200f), adjustedPoint.entries[0].points)

        val errorLabeler = nonContinuousLabeler.copy(overflowAfterEnd = LabelerConf.PointOverflow.Error)
        assertFailsWith<IllegalArgumentException> {
            original.validate(multipleEditMode = false, labelerConf = errorLabeler)
        }
    }

    @Test
    fun `postApplyLabelerConf connects entries for continuous labelers`() {
        val entries = listOf(
            entry("d.wav", "a", 0f, 25f),
            entry("d.wav", "c", 70f, 100f),
            entry("d.wav", "b", 30f, 70f),
        )
        val result = entries.postApplyLabelerConf(continuousLabeler)
        assertEquals(listOf("a", "b", "c"), result.map { it.name })
        assertEquals(listOf(30f, 70f, 100f), result.map { it.end })
    }

    @Test
    fun `postApplyLabelerConf removes same-name entries when not allowed`() {
        val entries = listOf(
            entry("a.wav", "x", 0f, 10f),
            entry("a.wav", "y", 10f, 20f),
            entry("b.wav", "x", 0f, 10f),
        )
        val result = entries.postApplyLabelerConf(nonContinuousLabeler)
        assertEquals(listOf("x", "y"), result.map { it.name })
        assertEquals("a.wav", result[0].sample)
    }
}
