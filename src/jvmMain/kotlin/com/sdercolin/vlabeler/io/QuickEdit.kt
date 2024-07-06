package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.InvalidCreatedProjectException
import com.sdercolin.vlabeler.exception.QuickProjectBuilderRuntimeException
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toFileOrNull
import io.ktor.utils.io.charsets.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.Charset

data class QuickEditRequest(
    val labelerConf: LabelerConf,
    val builder: LabelerConf.QuickProjectBuilder,
    val projectFile: String,
    val sampleDirectory: String,
    val cacheDirectory: String?,
    val inputFile: String?,
    val params: ParamMap,
    val encoding: String?,
) {

    suspend fun create(): Result<Project> {
        val workingDirectory = projectFile.toFile().parent
        require(workingDirectory.toFile().isDirectory) { "Working directory $workingDirectory does not exist" }
        val projectName = projectFile.toFile().nameWithoutExtension
        requireNotNull(
            this.sampleDirectory.toFileOrNull(ensureIsDirectory = true, ensureExists = true),
        ) {
            "Sample directory $sampleDirectory does not exist"
        }
        val cacheDirectoryParent = this.cacheDirectory?.toFile()?.parent
        if (cacheDirectoryParent != null) {
            requireNotNull(
                cacheDirectoryParent.toFileOrNull(ensureIsDirectory = true, ensureExists = true),
            ) {
                "The parent of cache directory $cacheDirectoryParent does not exist"
            }
        }
        val cacheDirectory = this.cacheDirectory ?: Project.getDefaultCacheDirectory(workingDirectory, projectName)
        return projectOf(
            sampleDirectory = sampleDirectory,
            workingDirectory = workingDirectory,
            projectName = projectName,
            cacheDirectory = cacheDirectory,
            rawLabelerConf = labelerConf,
            labelerParams = params,
            plugin = null,
            pluginParams = null,
            inputFilePath = inputFile,
            encoding = encoding?.let { encoding -> AvailableEncodings.find { encodingNameEquals(it, encoding) } }
                ?: Charset.defaultCharset().name,
            autoExport = true, // quick edit requires auto export
        )
    }
}

/**
 * Create a quick edit project from a file.
 */
fun startQuickEdit(
    scope: CoroutineScope,
    labelerConf: LabelerConf,
    builder: LabelerConf.QuickProjectBuilder,
    file: File,
    appState: AppState,
) {
    scope.launch(Dispatchers.IO) {
        runCatching {
            val js = JavaScript()
            listOf(
                Resources.envJs,
                Resources.fileJs,
                Resources.expectedErrorJs,
            ).forEach { js.execResource(it) }
            js.set("debug", isDebug)
            js.set("input", file)
            js.eval("input = new File(input)")
            val savedParams = labelerConf.loadSavedParamsJson(labelerConf.getSavedParamsFile())
            js.set("savedParams", savedParams)
            js.eval("savedParams = JSON.parse(savedParams)")
            val script = builder.scripts.getScripts(labelerConf.directory)
            runCatching {
                js.eval(script)
                js.eval("projectFile = projectFile.getAbsolutePath()")
                js.eval("sampleDirectory = sampleDirectory.getAbsolutePath()")
                if (js.hasValue("cacheDirectory")) {
                    js.eval("cacheDirectory = cacheDirectory.getAbsolutePath()")
                }
                if (!js.hasValue("params")) {
                    js.eval("params = savedParams")
                }
                js.eval("params = JSON.stringify(params)")
            }.onFailure { t ->
                val expected = js.getOrNull("expectedError") ?: false
                js.close()
                if (expected) {
                    throw QuickProjectBuilderRuntimeException(t, t.message?.parseJson())
                } else {
                    throw InvalidCreatedProjectException(t)
                }
            }
            val projectFile = js.get<String>("projectFile")
            val sampleDirectory = js.get<String>("sampleDirectory")
            val cacheDirectory = js.getOrNull<String>("cacheDirectory")
            val encoding = js.getOrNull<String>("encoding")
            val params = labelerConf.parseParamMap(js.get<String>("params"))
            js.close()
            val request = QuickEditRequest(
                projectFile = projectFile,
                sampleDirectory = sampleDirectory,
                cacheDirectory = cacheDirectory,
                params = params,
                encoding = encoding,
                labelerConf = labelerConf,
                builder = builder,
                inputFile = file.absolutePath,
            )
            appState.consumeQuickEditRequest(request)
        }.onFailure {
            appState.showError(it)
        }
    }
}
