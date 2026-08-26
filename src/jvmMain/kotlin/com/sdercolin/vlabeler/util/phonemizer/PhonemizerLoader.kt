package com.sdercolin.vlabeler.util.phonemizer

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.CustomPhonemizerDir
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PhonemizerDescriptor(
    val id: String,
    val displayName: String,
    val version: Int = 1,
    val script: String? = null,
    val displayNameKey: String? = null,
)

private fun File.loadAsJsPhonemizer(descriptor: PhonemizerDescriptor): PhonemizerPlugin? {
    val scriptFile = descriptor.script?.let { resolve(it) } ?: return null
    if (!scriptFile.exists()) return null
    val scriptText = scriptFile.readText()
    return JsPhonemizer(descriptor, scriptText)
}

private class JsPhonemizer(
    private val descriptor: PhonemizerDescriptor,
    private val scriptText: String,
) : PhonemizerPlugin {
    override val id: String = descriptor.id
    override val displayNameKey: Strings = runCatching {
        descriptor.displayNameKey?.let { Strings.valueOf(it) }
    }.getOrNull() ?: Strings.PhonemizerLanguageRaw

    override fun phonemize(text: String): List<String> {
        val js = JavaScript()
        return try {
            js.set("input", text)
            js.eval(scriptText)
            val output: Any? = js.get("output")
            when {
                output is List<*> -> output.mapNotNull { item -> item as? String }
                output is String -> output.split("[\\s,;]+".toRegex()).filter { s -> s.isNotBlank() }
                else -> text.split("[\\s,;]+".toRegex()).filter { s -> s.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.error(e)
            text.split("[\\s,;]+".toRegex()).filter { s -> s.isNotBlank() }
        } finally {
            js.close()
        }
    }
}

fun loadExternalPhonemizers() {
    loadServicePhonemizers()
    loadJsPhonemizers()
}

private fun loadServicePhonemizers() {
    try {
        val loader = java.util.ServiceLoader.load(PhonemizerPlugin::class.java)
        for (phonemizer in loader) {
            try {
                PhonemizerRegistry.register(phonemizer)
                Log.info("Loaded service phonemizer: ${phonemizer.id}")
            } catch (e: Exception) {
                Log.error(e)
            }
        }
    } catch (e: Exception) {
        Log.error(e)
    }
}

private fun loadJsPhonemizers() {
    val dir = CustomPhonemizerDir
    if (!dir.exists() || !dir.isDirectory) return
    val subDirs = dir.getChildren().filter { it.isDirectory }
    val singleFiles = dir.getChildren().filter { it.extension == "json" }
    val candidates = (subDirs + singleFiles).distinct()

    for (candidate in candidates) {
        val descriptorFile = when {
            candidate.isDirectory -> candidate.resolve("phonemizer.json")
            candidate.extension == "json" -> candidate
            else -> continue
        }
        if (!descriptorFile.exists()) continue
        try {
            val descriptor = descriptorFile.readText().parseJson<PhonemizerDescriptor>()
            val phonemizer = when {
                descriptor.script != null -> descriptorFile.parentFile.loadAsJsPhonemizer(descriptor)
                else -> null
            } ?: continue
            PhonemizerRegistry.register(phonemizer)
            Log.info("Loaded external phonemizer: ${descriptor.id}")
        } catch (e: Exception) {
            Log.error(e)
        }
    }
}
