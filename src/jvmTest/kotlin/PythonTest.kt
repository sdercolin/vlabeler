import com.sdercolin.vlabeler.util.Python
import kotlin.test.Test
import kotlin.test.assertEquals

class PythonTest {

    @Test
    fun testFloat() {
        val python = Python()
        python.exec("a=1\nb=2.1")
        assertEquals(2.1f, python.get("b"))

        python.exec("c=a+b")
        assertEquals(3.1f, python.get("c"))
    }

    @Test
    fun testUnicodeString() {
        val python = Python()
        python.set("a", "あ")
        assertEquals("あ", python.get("a"))
    }

    @Test
    fun testEval() {
        val python = Python()
        assertEquals(1.7, python.eval("(3+5.5)*2/(11-1)"))
    }
}
