package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.WaveLoadingAlgorithmVersion
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.findUnusedFile
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
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

    suspend fun load(project: Project, sampleFile: File, moduleName: String, appConf: AppConf): Result<SampleInfo> {
        val maxSampleRate = appConf.painter.amplitude.resampleDownToHz
        val normalize = appConf.painter.amplitude.normalize
        infoMap[moduleName to sampleFile.name]?.let {
            if (it.maxSampleRate == maxSampleRate &&
                it.normalize == normalize &&
                it.getFile(project).exists() &&
                it.lastModified == sampleFile.lastModified()
            ) {
                // Return memory cached sample info
                Log.info("Returning cached sample info for ${sampleFile.name} in module $moduleName")
                return Result.success(it)
            }
        }
        val existingInfo = runCatching {
            getSampleInfoFile(project, sampleFile).takeIf { it.exists() }?.readText()?.parseJson<SampleInfo>()
        }.getOrNull()
        if (existingInfo != null &&
            existingInfo.algorithmVersion == WaveLoadingAlgorithmVersion &&
            existingInfo.normalize == normalize &&
            existingInfo.maxSampleRate == maxSampleRate &&
            existingInfo.getFile(project).exists() &&
            existingInfo.lastModified == sampleFile.lastModified()
        ) {
            // Return file cached sample info
            Log.info("Returning cached sample info for ${sampleFile.name} in module $moduleName")
            infoMap[moduleName to existingInfo.name] = existingInfo
            return Result.success(existingInfo)
        }
        return loadSampleInfo(project, sampleFile, moduleName, appConf)
    }

    private suspend fun loadSampleInfo(
        project: Project,
        sampleFile: File,
        moduleName: String,
        appConf: AppConf,
    ): Result<SampleInfo> {
        Log.debug("Loading sample ${sampleFile.absolutePath}")

        val info = SampleInfo.load(project, sampleFile, appConf).getOrElse {
            return Result.failure(it)
        }
        infoMap[moduleName to info.name] = info
        val infoFile = getSampleInfoFile(project, sampleFile)
        infoFile.writeText(info.stringifyJson())
        cacheMap[project.getSampleFilePath(sampleFile)] =
            infoFile.toRelativeString(cacheDirectory).replace(File.separatorChar, '/')
        cacheMapFile.writeText(cacheMap.stringifyJson())
        return Result.success(info)
    }

    private fun Project.getSampleFilePath(sampleFile: File): String =
        sampleFile.toRelativeString(rootSampleDirectory).replace(File.separatorChar, '/')

    private fun getSampleInfoFile(project: Project, sampleFile: File) =
        cacheMap[project.getSampleFilePath(sampleFile)]
            ?.let { cacheDirectory.resolve(it) }
            ?.takeIf { it.isFile }
            ?: cacheDirectory.findUnusedFile(
                base = "${sampleFile.name}.info.json",
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )

    fun clearMemory() {
        infoMap.clear()
        cacheMap.clear()
    }

    fun clear(project: Project) {
        project.getCacheDir().resolve(SampleInfoCacheFolderName).deleteRecursively()
    }

    private const val SampleInfoCacheFolderName = "sample-info"
}
