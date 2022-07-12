package com.sdercolin.vlabeler.util

import kotlinx.serialization.Serializable
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.io.Closeable
import java.io.File
import java.io.OutputStream

class JavaScript(outputStream: OutputStream? = null, currentWorkingDirectory: File? = null) : Closeable {

    private val context = Context.newBuilder()
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup { true }
        .allowIO(true)
        .currentWorkingDirectory(currentWorkingDirectory?.toPath() ?: HomeDir.toPath())
        .out(outputStream ?: System.out)
        .build()

    private val bindings get() = context.getBindings("js")

    fun eval(source: String): Value? = context.eval("js", source)

    fun <T : Any> getOrNull(name: String, ofClass: Class<T>): T? {
        val value = bindings.getMember(name) ?: return null
        if (value.isNull) return null
        return value.`as`(ofClass)
    }

    inline fun <reified T : Any> get(name: String): T {
        return requireNotNull(getOrNull(name, T::class.java))
    }

    inline fun <reified T : Any> getOrNull(name: String): T? {
        return getOrNull(name, T::class.java)
    }

    fun set(name: String, value: Any?) {
        bindings.putMember(name, value)
    }

    fun <T> setArray(name: String, value: List<T>) {
        set(name, value)
        eval("$name = Java.from($name)")
    }

    inline fun <reified T : @Serializable Any> setObject(name: String, value: T) {
        set(name, toJson(value))
        eval("$name = JSON.parse($name)")
    }

    inline fun <reified T : @Serializable Any> getObject(name: String): T {
        val json = eval("JSON.stringify($name)")!!.asString()
        return parseJson(json)
    }

    override fun close() {
        context.close()
    }
}
