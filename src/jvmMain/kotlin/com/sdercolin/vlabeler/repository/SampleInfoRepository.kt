package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.WaveLoadingAlgorithmVersion
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toFileOrNull
import java.io.File

@Stable
object SampleInfoRepository {

    private lateinit var cacheDirectory: File

    private val infoMap = mutableMapOf<Pair<String, String>, SampleInfo>()

    private val cacheMapFile get() = cacheDirectory.resolve("map.json")
    private var cacheMap: MutableMap<String, String> = mutableMapOf()

    fun init(project: Project) {
        cacheDirectory = project.getCacheDir().resolve(SampleInfoCacheFolderName)
        cacheDirectory.mkdirs()
        cacheMap = runCatching {
            cacheMapFile.takeIf { it.exists() }?.readText()?.parseJson<Map<String, String>>()?.toMutableMap()
        }.getOrNull() ?: mutableMapOf()
    }

    suspend fun load(file: File, moduleName: String, appConf: AppConf): Result<SampleInfo> {
        val maxSampleRate = appConf.painter.amplitude.resampleDownToHz
        val normalize = appConf.painter.amplitude.normalize
        infoMap[moduleName to file.name]?.let {
            if (it.maxSampleRate == maxSampleRate &&
                it.normalize == normalize &&
                it.file.toFile().exists() &&
                it.lastModified == file.lastModified()
            ) {
                // Return memory cached sample info
                Log.info("Returning cached sample info for ${file.name} in module $moduleName")
                return Result.success(it)
            }
        }
        val existingInfo = runCatching {
            getSampleInfoFile(file, moduleName).takeIf { it.exists() }?.readText()?.parseJson<SampleInfo>()
        }.getOrNull()
        if (existingInfo != null &&
            existingInfo.algorithmVersion == WaveLoadingAlgorithmVersion &&
            existingInfo.normalize == normalize &&
            existingInfo.maxSampleRate == maxSampleRate &&
            existingInfo.file.toFile().exists() &&
            existingInfo.lastModified == file.lastModified()
        ) {
            // Return file cached sample info
            Log.info("Returning cached sample info for ${file.name} in module $moduleName")
            val info = existingInfo.copy(moduleName = moduleName)
            infoMap[moduleName to info.name] = info
            return Result.success(info)
        }
        return loadSampleInfo(file, moduleName, appConf)
    }

    private suspend fun loadSampleInfo(file: File, moduleName: String, appConf: AppConf): Result<SampleInfo> {
        Log.debug("Loading sample ${file.absolutePath}")

        val info = SampleInfo.load(file, moduleName, appConf).getOrElse {
            return Result.failure(it)
        }
        infoMap[moduleName to info.name] = info
        val infoFile = getSampleInfoFile(file, moduleName)
        infoFile.writeText(info.stringifyJson())
        cacheMap[file.absolutePath] = infoFile.absolutePath
        cacheMapFile.writeText(cacheMap.stringifyJson())
        return Result.success(info)
    }

    private fun getSampleInfoFile(wavFile: File, moduleName: String) =
        cacheMap[wavFile.absolutePath]?.toFileOrNull(ensureIsFile = true)
            ?: cacheDirectory.resolve(File(moduleName))
                .also { it.mkdirs() }
                .resolve("${wavFile.name}.info.json")

    fun clearMemory() {
        infoMap.clear()
        cacheMap.clear()
    }

    fun clear(project: Project) {
        project.getCacheDir().resolve(SampleInfoCacheFolderName).deleteRecursively()
    }

    private const val SampleInfoCacheFolderName = "sample-info"
}
