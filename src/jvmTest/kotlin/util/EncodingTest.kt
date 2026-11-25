package util

import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.DefaultEncoding
import com.sdercolin.vlabeler.util.detectEncoding
import com.sdercolin.vlabeler.util.encodingNameEquals
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [encodingNameEquals], [DefaultEncoding] and [detectEncoding].
 */
class EncodingTest {

    @Test
    fun testEncodingNameEquals() {
        assertEquals(true, encodingNameEquals("UTF-8", "UTF-8"))
        assertEquals(true, encodingNameEquals("UTF-8", "utf-8"))
        assertEquals(true, encodingNameEquals("UTF-8", "UTF_8"))
        assertEquals(true, encodingNameEquals("UTF-8", "UTF 8"))
        assertEquals(true, encodingNameEquals("Shift-JIS", "Shift_JIS"))
        assertEquals(true, encodingNameEquals("shift jis", "Shift-JIS"))
        assertEquals(false, encodingNameEquals("UTF-8", "UTF-16"))
        assertEquals(false, encodingNameEquals("Shift-JIS", "EUC-JP"))
    }

    @Test
    fun testDefaultEncoding() {
        assertEquals("UTF-8", DefaultEncoding)
        assertEquals(DefaultEncoding, AvailableEncodings.first())
    }

    @Test
    fun testDetectEncodingUtf8() {
        val text = "こんにちは、世界。これはテストです。日本語の文章を書いています。"
        val detected = text.toByteArray(Charsets.UTF_8).detectEncoding()
        assertEquals("UTF-8", detected)
    }
}
