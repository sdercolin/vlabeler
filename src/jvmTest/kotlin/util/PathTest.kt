package util

import com.sdercolin.vlabeler.env.isWindows
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.asPathRelativeToHome
import com.sdercolin.vlabeler.util.asSimplifiedPaths
import com.sdercolin.vlabeler.util.isValidFileName
import com.sdercolin.vlabeler.util.lastPathSection
import com.sdercolin.vlabeler.util.pathSections
import com.sdercolin.vlabeler.util.resolveHome
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Tests for pure path utilities in `Path.kt`.
 */
class PathTest {

    @Test
    fun testPathSections() {
        assertContentEquals(listOf("a", "b", "c"), "a/b/c".pathSections)
        assertContentEquals(listOf("a", "b", "c"), "a\\b\\c".pathSections)
        assertContentEquals(listOf("a", "b", "c"), "/a/b/c/".pathSections)
        assertContentEquals(listOf("a", "b", "c.wav"), "a\\b/c.wav".pathSections)
        assertContentEquals(listOf("a"), "a".pathSections)
    }

    @Test
    fun testLastPathSection() {
        assertEquals("c.wav", "a/b/c.wav".lastPathSection)
        assertEquals("c.wav", "a\\b\\c.wav".lastPathSection)
        assertEquals("a", "a".lastPathSection)
    }

    @Test
    fun testIsValidFileName() {
        assertEquals(true, "file.txt".isValidFileName())
        assertEquals(true, "日本語 ファイル.wav".isValidFileName())
        assertEquals(false, "".isValidFileName())
        assertEquals(false, "   ".isValidFileName())
        assertEquals(false, "a/b.txt".isValidFileName())
        assertEquals(false, "a\\b.txt".isValidFileName())
        assertEquals(false, "a:b".isValidFileName())
        assertEquals(false, "a*b".isValidFileName())
        assertEquals(false, "a?b".isValidFileName())
        assertEquals(false, "a\"b".isValidFileName())
        assertEquals(false, "a<b>".isValidFileName())
        assertEquals(false, "a|b".isValidFileName())
        assertEquals(false, "a\u0000b".isValidFileName())
        assertEquals(true, "a b".isValidFileName())
    }

    @Test
    fun testResolveHome() {
        val home = HomeDir.absolutePath
        assertEquals("$home/foo/bar", "~/foo/bar".resolveHome())
        assertEquals(home, "~".resolveHome())
        assertEquals("/foo/bar", "/foo/bar".resolveHome())
    }

    @Test
    fun testAsPathRelativeToHome() {
        val home = HomeDir.absolutePath
        val input = "$home/foo/bar"
        val expected = if (isWindows) input else "~/foo/bar"
        assertEquals(expected, input.asPathRelativeToHome())
        assertEquals("/nonexistent-root/foo", "/nonexistent-root/foo".asPathRelativeToHome())
    }

    @Test
    fun testAsSimplifiedPathsAllDistinct() {
        val paths = listOf("a/b/c.wav", "d/e/f.wav")
        assertContentEquals(listOf("c.wav", "f.wav"), paths.asSimplifiedPaths())
    }

    @Test
    fun testAsSimplifiedPathsWithDuplicatedFileNames() {
        val paths = listOf("root/a/x.wav", "root/b/x.wav")
        assertContentEquals(listOf(".../a/x.wav", ".../b/x.wav"), paths.asSimplifiedPaths())
    }

    @Test
    fun testAsSimplifiedPathsWithDuplicatedFileNamesAtRoot() {
        val paths = listOf("a/x.wav", "b/x.wav")
        assertContentEquals(listOf("a/x.wav", "b/x.wav"), paths.asSimplifiedPaths())
    }

    @Test
    fun testAsSimplifiedPathsWithTwoDuplicatedLevels() {
        val paths = listOf("a/b/x.wav", "c/b/x.wav")
        assertContentEquals(listOf("a/b/x.wav", "c/b/x.wav"), paths.asSimplifiedPaths())
    }
}
