package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.deleteRecursivelyLogged
import com.sdercolin.vlabeler.util.findUnusedFile
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import java.io.File

/**
 * Repository for sample information.
 */
@Stable
object SampleInfoRepository {

    private lateinit var cacheDirectory: File

    private val infoMap = mutableMapOf<Pair<String, String>, SampleInfo>()

    private val cacheMapFile get() = cacheDirectory.resolve("map.json")
    private var cacheMap: MutableMap<String, String> = mutableMapOf()

    /**
     * Initialize the repository.
     *
     * @param project Current project.
     */
    fun init(project: Project) {
        cacheDirectory = project.getCacheDir().resolve(SAMPLE_INFO_CACHE_FOLDER_NAME)
        cacheDirectory.mkdirs()
        cacheMap = runCatching {
            cacheMapFile.takeIf { it.exists() }?.readText()?.parseJson<Map<String, String>>()?.toMutableMap()
        }.getOrNull() ?: mutableMapOf()
    }

    /**
     * Get sample information from cache or file.
     */
    suspend fun load(project: Project, sampleFile: File, moduleName: String, appConf: AppConf): Result<SampleInfo> {
        infoMap[moduleName to sampleFile.name]?.let {
            if (!it.shouldReload(project, sampleFile, appConf)) {
                // Return memory cached sample info
                Log.info("Returning cached sample info for ${sampleFile.name} in module $moduleName")
                return Result.success(it)
            }
        }
        val existingInfo = runCatching {
            getSampleInfoFile(project, moduleName, sampleFile).takeIf { it.exists() }
                ?.readText()
                ?.parseJson<SampleInfo>()
        }.getOrNull()
        if (existingInfo != null && !existingInfo.shouldReload(project, sampleFile, appConf)) {
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

        val info = SampleInfo.load(project, moduleName, sampleFile, appConf).getOrElse {
            return Result.failure(it)
        }
        infoMap[moduleName to info.name] = info
        val infoFile = getSampleInfoFile(project, moduleName, sampleFile)
        infoFile.parentFile.mkdirs()
        infoFile.writeText(info.stringifyJson())
        cacheMap[project.getSampleFilePath(sampleFile)] =
            infoFile.toRelativeString(cacheDirectory).replace(File.separatorChar, '/')
        cacheDirectory.mkdirs()
        cacheMapFile.writeText(cacheMap.stringifyJson())
        return Result.success(info)
    }

    private fun Project.getSampleFilePath(sampleFile: File): String =
        sampleFile.toRelativeString(rootSampleDirectory).replace(File.separatorChar, '/')

    private fun getSampleInfoFile(project: Project, moduleName: String, sampleFile: File) =
        cacheMap[project.getSampleFilePath(sampleFile)]
            ?.let { cacheDirectory.resolve(it) }
            ?.takeIf { it.isFile }
            ?: cacheDirectory.findUnusedFile(
                base = "${moduleName}_${sampleFile.name}.info.json",
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )

    fun clear(project: Project) {
        infoMap.clear()
        cacheMap.clear()
        project.getCacheDir().resolve(SAMPLE_INFO_CACHE_FOLDER_NAME).deleteRecursivelyLogged()
    }

    /**
     * Move the cache from the old cache directory to the new cache directory.
     */
    fun moveTo(oldCacheDirectory: File, newCacheDirectory: File, clearOld: Boolean) {
        Log.debug("Moving cache from $oldCacheDirectory to $newCacheDirectory")
        val oldDirectory = oldCacheDirectory.resolve(SAMPLE_INFO_CACHE_FOLDER_NAME)
        if (oldDirectory.isDirectory.not()) return
        oldDirectory.copyRecursively(newCacheDirectory.resolve(SAMPLE_INFO_CACHE_FOLDER_NAME), overwrite = true)
        if (clearOld) oldDirectory.deleteRecursivelyLogged()
    }

    private const val SAMPLE_INFO_CACHE_FOLDER_NAME = "sample-info"
}
