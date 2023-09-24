import com.sdercolin.vlabeler.util.containsFileRecursively
import com.sdercolin.vlabeler.util.findUnusedFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [containsFileRecursively] and [findUnusedFile].
 */
class FileUtilTest {

    @Test
    fun testFindUnusedFileNoExisting() {
        val root = File("/tmp")
        val baseName = "base.kt"
        val unusedFilePath = root.findUnusedFile(baseName, setOf()).absolutePath
        assertEquals("/tmp/base.kt", unusedFilePath)
    }

    @Test
    fun testFindUnusedFileExisting() {
        val root = File("/tmp")
        val baseName = "base.kt"
        val existingFilePath = "/tmp/base.kt"
        val unusedFilePath = root.findUnusedFile(baseName, setOf(existingFilePath)).absolutePath
        assertEquals("/tmp/base.1.kt", unusedFilePath)
    }

    @Test
    fun testFindUnusedFileExistingMultiple() {
        val root = File("/tmp")
        val baseName = "base.kt"
        val existingFilePaths = setOf("/tmp/base.kt", "/tmp/base.1.kt", "/tmp/base.2.kt")
        val unusedFilePath = root.findUnusedFile(baseName, existingFilePaths).absolutePath
        assertEquals("/tmp/base.3.kt", unusedFilePath)
    }

    @Test
    fun testFindUnusedFileExistingMultipleWithGap() {
        val root = File("/tmp")
        val baseName = "base.kt"
        val existingFilePaths = setOf("/tmp/base.kt", "/tmp/base.1.kt", "/tmp/base.3.kt")
        val unusedFilePath = root.findUnusedFile(baseName, existingFilePaths).absolutePath
        assertEquals("/tmp/base.2.kt", unusedFilePath)
    }

    @Test
    fun testContainsFileRecursivelyDirectChild() {
        val parent = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/util")
        val child = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/util/File.kt")
        assertEquals(true, parent.containsFileRecursively(child))
    }

    @Test
    fun testContainsFileRecursivelyIndirectChild() {
        val parent = File("/src/jvmMain/kotlin/com/sdercolin/")
        val child = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/util/File.kt")
        assertEquals(true, parent.containsFileRecursively(child))
    }

    @Test
    fun testContainsFileRecursivelyNotChild() {
        val parent = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/ui")
        val child = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/util/File.kt")
        assertEquals(false, parent.containsFileRecursively(child))
    }

    @Test
    fun testContainsFileRecursivelyNotChild2() {
        val parent = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/ui")
        val child = File("/src/jvmMain/kotlin/com/sdercolin/")
        assertEquals(false, parent.containsFileRecursively(child))
    }

    @Test
    fun testContainsFileRecursivelyChildIsRelative() {
        val parent = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/ui")
        val child = File("wav")
        assertEquals(false, parent.containsFileRecursively(child))
    }

    @Test
    fun testContainsFileRecursivelyParentIsRelative() {
        val parent = File("vlabeler")
        val child = File("/src/jvmMain/kotlin/com/sdercolin/vlabeler/ui")
        assertEquals(false, parent.containsFileRecursively(child))
    }
}
