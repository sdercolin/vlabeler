package com.sdercolin.vlabeler.util

import org.python.util.PythonInterpreter
import java.io.OutputStream
import java.util.Properties

class Python {

    init {
        val props = Properties().apply { put("python.console.encoding", "UTF-8") }
        PythonInterpreter.initialize(System.getProperties(), props, arrayOf())
    }

    private val interpreter = PythonInterpreter().apply {
        systemState.setdefaultencoding("utf8")
    }

    fun setOutputStream(outputStream: OutputStream) {
        interpreter.setOut(outputStream)
    }

    fun setCurrentWorkingDirectory(path: String) {
        interpreter.systemState.currentWorkingDir = path
    }

    fun exec(script: String) {
        interpreter.exec(script)
    }

    fun set(name: String, value: Any) {
        interpreter.set(name, value)
    }

    fun <T : Any> get(name: String, ofClass: Class<T>): T {
        return interpreter.get(name, ofClass)
    }

    inline fun <reified T : Any> get(name: String): T {
        return get(name, T::class.java)
    }

    inline fun <reified T : Any> getOrNull(name: String): T? {
        return kotlin.runCatching { get(name, T::class.java) }.getOrNull()
    }

    fun eval(text: String): Double {
        return interpreter.eval(text).asDouble()
    }

    fun close() = interpreter.close()
}
