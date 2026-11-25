package util

import com.sdercolin.vlabeler.util.calculateMD5
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [calculateMD5].
 */
class MD5Test {

    @Test
    fun testKnownContent() {
        val file = createTempFile(prefix = "md5-test", suffix = ".txt")
        file.writeText("hello world")
        val result = calculateMD5(file.toFile())
        file.toFile().delete()
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", result)
    }

    @Test
    fun testEmptyFile() {
        val file = createTempFile(prefix = "md5-test", suffix = ".txt")
        val result = calculateMD5(file.toFile())
        file.toFile().delete()
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }

    @Test
    fun testBinaryContentLargerThanBuffer() {
        val file = createTempFile(prefix = "md5-test", suffix = ".bin")
        // 8192 bytes of 0x00: needs multiple reads with the 4096-byte buffer
        file.writeBytes(ByteArray(8192))
        val result = calculateMD5(file.toFile())
        file.toFile().delete()
        assertEquals("0829f71740aab1ab98b33eae21dee122", result)
    }
}
