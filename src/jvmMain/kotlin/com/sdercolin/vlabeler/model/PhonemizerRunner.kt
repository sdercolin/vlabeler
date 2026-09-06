package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.toFile
import java.util.concurrent.ConcurrentHashMap

class G2pBridge(private val pluginDirectory: java.io.File?) {
    fun predict(
        modelFileName: String,
        graphemes: List<*>,
        phonemes: List<*>,
        word: String,
    ): List<String>? {
        val modelFile = pluginDirectory?.resolve(modelFileName) ?: return null
        return com.sdercolin.vlabeler.util.phonemizer.OnnxG2pPredictor.predict(
            modelFile = modelFile,
            graphemes = graphemes.map { it.toString() },
            phonemes = phonemes.map { it.toString() },
            word = word,
        )
    }
}

object PhonemizerRunner {

    private val cache = ConcurrentHashMap<Pair<String, String>, List<String>>()
    private val resourceCache = ConcurrentHashMap<String, List<String>>()
    private val scriptCache = ConcurrentHashMap<String, List<Pair<String, String>>>()

    fun run(plugin: Plugin, input: String): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()

        val cacheKey = plugin.name to trimmed
        cache[cacheKey]?.let { return it }

        val js = JavaScript(
            currentWorkingDirectory = plugin.directory?.absolutePath?.toFile(),
        )
        return try {
            val resourceTexts = resourceCache.computeIfAbsent(plugin.name) { plugin.readResourceFiles() }
            val scripts = scriptCache.computeIfAbsent(plugin.name) {
                plugin.scriptFiles.zip(plugin.readScriptTexts())
            }
            js.set("input", trimmed)
            js.set("resources", resourceTexts)
            js.set("g2p", G2pBridge(plugin.directory))

            scripts.forEach { (file, source) ->
                js.exec(file, source)
            }

            val output = js.getOrNull<Any>("output")
            val phonemes = when {
                output is List<*> -> output.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                output is String -> output.split("[\\s,;]+".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
                else -> trimmed.split("[\\s,;]+".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
            }
            if (phonemes.isNotEmpty()) {
                cache[cacheKey] = phonemes
            }
            phonemes
        } catch (e: Exception) {
            Log.error(e)
            trimmed.split("[\\s,;]+".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
        } finally {
            js.close()
        }
    }

    fun clearCache() {
        cache.clear()
        resourceCache.clear()
        scriptCache.clear()
        com.sdercolin.vlabeler.util.phonemizer.OnnxG2pPredictor.clearCache()
    }
}
