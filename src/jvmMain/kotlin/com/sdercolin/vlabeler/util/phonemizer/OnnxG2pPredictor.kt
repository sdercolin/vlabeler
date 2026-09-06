package com.sdercolin.vlabeler.util.phonemizer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.IntBuffer
import java.util.concurrent.ConcurrentHashMap

object OnnxG2pPredictor {
    private val sessionCache = ConcurrentHashMap<String, OrtSession>()
    private val queryCache = ConcurrentHashMap<String, List<String>>()

    private fun getSession(modelFile: File): OrtSession? {
        if (!modelFile.exists()) return null
        val cached = sessionCache[modelFile.absolutePath]
        if (cached != null) return cached
        return try {
            val session = OrtEnvironment.getEnvironment().createSession(modelFile.absolutePath)
            sessionCache[modelFile.absolutePath] = session
            session
        } catch (_: Throwable) {
            null
        }
    }

    fun predict(
        modelFile: File,
        graphemes: List<String>,
        phonemes: List<String>,
        word: String,
    ): List<String>? {
        if (word.isEmpty()) return null
        val lower = word.lowercase()
        val cacheKey = "${modelFile.absolutePath}:$lower"
        queryCache[cacheKey]?.let { return it }

        val session = getSession(modelFile) ?: return null
        val env = OrtEnvironment.getEnvironment()

        val graphemeIndexes = graphemes
            .drop(4)
            .mapIndexed { index, s -> s to (index + 4) }
            .toMap()

        val encoded = lower.mapNotNull { graphemeIndexes[it.toString()] }
        if (encoded.isEmpty()) return null

        val tgt = mutableListOf(2)
        var t = 0
        val srcLength = encoded.size

        try {
            val srcTensor = OnnxTensor.createTensor(
                env,
                IntBuffer.wrap(encoded.toIntArray()),
                longArrayOf(1, encoded.size.toLong()),
            )
            srcTensor.use {
                while (t < srcLength && tgt.size < 48) {
                    OnnxTensor.createTensor(
                        env,
                        IntBuffer.wrap(tgt.toIntArray()),
                        longArrayOf(1, tgt.size.toLong()),
                    ).use { tgtTensor ->
                        OnnxTensor.createTensor(
                            env,
                            IntBuffer.wrap(intArrayOf(t)),
                            longArrayOf(1),
                        ).use { tTensor ->
                            val inputs = mapOf("src" to srcTensor, "tgt" to tgtTensor, "t" to tTensor)
                            session.run(inputs).use { result ->
                                val outputTensor = result[0] as OnnxTensor
                                val pred = when (val v = outputTensor.value) {
                                    is IntArray -> v.firstOrNull() ?: 2
                                    is LongArray -> v.firstOrNull()?.toInt() ?: 2
                                    is Array<*> -> when (val first = v.firstOrNull()) {
                                        is IntArray -> first.firstOrNull() ?: 2
                                        is LongArray -> first.firstOrNull()?.toInt() ?: 2
                                        is Number -> first.toInt()
                                        else -> 2
                                    }
                                    else -> 2
                                }
                                if (pred != 2) tgt.add(pred) else t++
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            return null
        }

        val decoded = tgt.drop(1).mapNotNull { phonemes.getOrNull(it) }
        if (decoded.isNotEmpty()) queryCache[cacheKey] = decoded
        return decoded.takeIf { it.isNotEmpty() }
    }

    fun clearCache() {
        sessionCache.values.forEach { runCatching { it.close() } }
        sessionCache.clear()
        queryCache.clear()
    }
}
