import com.sdercolin.vlabeler.util.JavaScript
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaScriptTest {

    @Test
    fun testDouble() {
        JavaScript().use {
            it.eval("a=1\nb=2.1")
            assertEquals(2.1, it.get("b"))
            assertEquals(3.1, it.eval("a+b")?.asDouble())
        }
    }

    @Test
    fun testUnicodeString() {
        JavaScript().use {
            it.set("a", "あ")
            assertEquals("あ", it.get("a"))
        }
    }

    @Test
    fun testNull() {
        JavaScript().use {
            it.set("a", null)
            it.eval("b=null")
            assert(it.getOrNull<Any>("a") == null)
            assert(it.getOrNull<Any>("b") == null)
        }
    }

    @Test
    fun testUndefined() {
        JavaScript().use {
            assert(it.getOrNull<Any>("a") == null)
        }
    }

    @Test
    fun testList() {
        JavaScript().use {
            val list = listOf(1, 2, 3)
            it.setArray("list", list)
            it.eval("list[0]=3\nlist.push(4)")
            assertEquals(listOf(3, 2, 3, 4), it.get("list"))
        }
    }

    @Test
    fun testMap() {
        JavaScript().use {
            val map = mapOf("a" to 1, "b" to 2)
            it.set("map", map)
            it.eval("map[\"a\"]=3\nmap[\"c\"]=1")
            assertEquals(mapOf("a" to 3, "b" to 2, "c" to 1), it.get("map"))
        }
    }

    @Test
    fun testObject() {
        JavaScript().use {
            val pair = Pair("c", 1)
            it.setObject("pair", pair)
            it.eval("pair.first = \"a\"")
            assertEquals(Pair("a", 1), it.getObject("pair"))
        }
    }
}
