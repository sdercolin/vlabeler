package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.WaveLoadingAlgorithmVersion
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File

@Stable
object SampleRepository {

    private lateinit var cacheDirectory: File

    private val infoMap = mutableMapOf<String, SampleInfo>()
    private val dataMap = mutableMapOf<String, Deferred<Sample>>()

    fun init(project: Project) {
        cacheDirectory = project.getCacheDir().resolve(SampleInfoCacheFolderName)
        cacheDirectory.mkdirs()
    }

    suspend fun load(file: File, appConf: AppConf): Result<SampleInfo> {
        infoMap[file.nameWithoutExtension]?.let {
            if (File(it.file).exists()) {
                // Return memory cached sample info
                Log.info("Returning cached sample info for ${file.nameWithoutExtension}")
                return Result.success(it)
            }
        }
        val existingInfo = runCatching {
            getSampleInfoFile(file).takeIf { it.exists() }?.readText()?.parseJson<SampleInfo>()
        }.getOrNull()
        if (existingInfo != null &&
            existingInfo.algorithmVersion == WaveLoadingAlgorithmVersion &&
            existingInfo.maxChunkSize == appConf.painter.maxDataChunkSize &&
            existingInfo.file.toFile().exists()
        ) {
            // Return file cached sample info
            Log.info("Returning cached sample info for ${file.nameWithoutExtension}")
            return Result.success(existingInfo)
        }
        return loadSample(file, appConf).map { it.info }
    }

    private suspend fun loadSample(file: File, appConf: AppConf): Result<Sample> {
        Log.debug("Loading sample ${file.absolutePath}")
        val sample = loadSampleFile(file, appConf).getOrElse {
            return Result.failure(it)
        }
        dataMap[sample.info.name] = coroutineScope { async { sample } }
        infoMap[sample.info.name] = sample.info
        getSampleInfoFile(file).writeText(sample.info.stringifyJson())
        return Result.success(sample)
    }

    suspend fun getSample(file: File, appConf: AppConf): Sample {
        return dataMap.remove(file.nameWithoutExtension)?.await()
            ?: loadSample(file, appConf).getOrElse { throw it }
    }

    fun clear() {
        dataMap.clear()
        Log.info("SampleRepository clear()")
    }

    private fun getSampleInfoFile(wavFile: File) =
        cacheDirectory.resolve("${wavFile.nameWithoutExtension}_info.json")

    private const val SampleInfoCacheFolderName = "sample-info"
}
