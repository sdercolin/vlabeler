package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.audio.conversion.WaveConverter
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.deleteRecursivelyLogged
import com.sdercolin.vlabeler.util.findUnusedFile
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import java.io.File

/**
 * Repository for converted audio by [WaveConverter].
 */
@Stable
object ConvertedAudioRepository {

    private lateinit var cacheDirectory: File
    private val cacheMapFile get() = cacheDirectory.resolve("map.json")
    private var cacheMap: MutableMap<String, String> = mutableMapOf()

    fun init(project: Project) {
        cacheDirectory = project.getCacheDir().resolve(WAVE_CACHE_FOLDER_NAME)
        cacheDirectory.mkdirs()
        cacheMap = runCatching {
            cacheMapFile.takeIf { it.exists() }?.readText()?.parseJson<Map<String, String>>()?.toMutableMap()
        }.getOrNull() ?: mutableMapOf()
    }

    suspend fun create(
        project: Project,
        file: File,
        moduleName: String,
        converter: WaveConverter,
        appConf: AppConf,
    ): File {
        val outputFile = getConvertedWavFile(project, moduleName, file)
        outputFile.parentFile?.mkdirs()
        converter.convert(file, outputFile, appConf.painter.conversion)
        cacheMap[project.getSampleFilePath(file)] =
            outputFile.toRelativeString(cacheDirectory).replace(File.separatorChar, '/')
        cacheMapFile.writeText(cacheMap.stringifyJson())
        return outputFile
    }

    private fun Project.getSampleFilePath(sampleFile: File): String =
        sampleFile.toRelativeString(rootSampleDirectory).replace(File.separatorChar, '/')

    private fun getConvertedWavFile(project: Project, moduleName: String, sampleFile: File) =
        cacheMap[project.getSampleFilePath(sampleFile)]
            ?.let { cacheDirectory.resolve(it) }
            ?.takeIf { it.isFile }
            ?: cacheDirectory.findUnusedFile(
                base = "${moduleName}_${sampleFile.name}.converted.wav",
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )

    /**
     * Clear cached sample information files.
     */
    fun clear(project: Project) {
        project.getCacheDir().resolve(WAVE_CACHE_FOLDER_NAME).deleteRecursivelyLogged()
    }

    /**
     * Move the cache from the old cache directory to the new cache directory.
     */
    fun moveTo(oldCacheDirectory: File, newCacheDirectory: File, clearOld: Boolean) {
        Log.debug("Moving cache from $oldCacheDirectory to $newCacheDirectory")
        val oldDirectory = oldCacheDirectory.resolve(WAVE_CACHE_FOLDER_NAME)
        if (oldDirectory.isDirectory.not()) return
        oldDirectory.copyRecursively(newCacheDirectory.resolve(WAVE_CACHE_FOLDER_NAME), overwrite = true)
        if (clearOld) oldDirectory.deleteRecursivelyLogged()
    }

    private const val WAVE_CACHE_FOLDER_NAME = "wav"
}
