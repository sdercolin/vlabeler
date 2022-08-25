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
import java.io.File

@Stable
object SampleInfoRepository {

    private lateinit var cacheDirectory: File

    private val infoMap = mutableMapOf<String, SampleInfo>()

    fun init(project: Project) {
        cacheDirectory = project.getCacheDir().resolve(SampleInfoCacheFolderName)
        cacheDirectory.mkdirs()
        // TODO: handle case that the cache directory is not created
    }

    fun load(file: File, appConf: AppConf): Result<SampleInfo> {
        infoMap[file.nameWithoutExtension]?.let {
            if (File(it.file).exists() && it.lastModified == file.lastModified()) {
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
            existingInfo.file.toFile().exists() &&
            existingInfo.lastModified == file.lastModified()
        ) {
            // Return file cached sample info
            Log.info("Returning cached sample info for ${file.nameWithoutExtension}")
            return Result.success(existingInfo)
        }
        return loadSampleInfo(file, appConf)
    }

    private fun loadSampleInfo(file: File, appConf: AppConf): Result<SampleInfo> {
        Log.debug("Loading sample ${file.absolutePath}")

        val info = SampleInfo.load(file, appConf).getOrElse {
            return Result.failure(it)
        }
        infoMap[info.name] = info
        getSampleInfoFile(file).writeText(info.stringifyJson())
        return Result.success(info)
    }

    private fun getSampleInfoFile(wavFile: File) =
        cacheDirectory.resolve("${wavFile.nameWithoutExtension}_info.json")

    private const val SampleInfoCacheFolderName = "sample-info"
}
