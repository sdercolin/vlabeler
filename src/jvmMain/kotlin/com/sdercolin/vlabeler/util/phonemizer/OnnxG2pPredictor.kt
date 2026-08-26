package com.sdercolin.vlabeler.util.phonemizer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.IntBuffer
import java.util.concurrent.ConcurrentHashMap

class OnnxG2pPredictor(
    private val modelResourcePath: String,
    private val graphemes: List<String>,
    private val phonemes: List<String>,
) {
    private val graphemeIndexes: Map<String, Int> = graphemes
        .drop(4)
        .mapIndexed { index, s -> s to (index + 4) }
        .toMap()

    private val session: OrtSession? by lazy {
        try {
            val bytes = Phonemizer::class.java.getResourceAsStream(modelResourcePath)?.readBytes() ?: return@lazy null
            OrtEnvironment.getEnvironment().createSession(bytes)
        } catch (_: Throwable) {
            null
        }
    }

    private val cache = ConcurrentHashMap<String, List<String>>()

    fun predict(word: String): List<String>? {
        if (word.isEmpty()) return null
        val lower = word.lowercase()
        cache[lower]?.let { return it }

        val activeSession = session ?: return null
        val env = OrtEnvironment.getEnvironment()

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
                    OnnxTensor.createTensor(env, IntBuffer.wrap(tgt.toIntArray()), longArrayOf(1, tgt.size.toLong())).use { tgtTensor ->
                        OnnxTensor.createTensor(env, IntBuffer.wrap(intArrayOf(t)), longArrayOf(1)).use { tTensor ->
                            val inputs = mapOf("src" to srcTensor, "tgt" to tgtTensor, "t" to tTensor)
                            activeSession.run(inputs).use { result ->
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
        if (decoded.isNotEmpty()) cache[lower] = decoded
        return decoded.takeIf { it.isNotEmpty() }
    }
}
