package com.sdercolin.vlabeler.repository.update

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.repository.update.model.Release
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.repository.update.model.UpdateChannel
import com.sdercolin.vlabeler.util.Url
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.appendPathSegments
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository for application update.
 */
class UpdateRepository {

    private suspend fun <T> useClient(
        withoutTimeout: Boolean = false,
        block: suspend HttpClient.() -> T,
    ) = HttpClient(Apache) {
        expectSuccess = true
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.debug(message)
                }
            }
            level = if (isDebug) LogLevel.INFO else LogLevel.NONE
        }
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
        install(HttpTimeout) {
            if (withoutTimeout.not()) {
                requestTimeoutMillis = 5000
            }
        }
    }.let {
        val result = it.block()
        it.close()
        result
    }

    private suspend fun getResponse(vararg paths: String): Result<HttpResponse> =
        useClient {
            runCatching {
                get(Url.GITHUB_API_ROOT) {
                    url { appendPathSegments(*paths) }
                    headers {
                        append("Accept", "application/vnd.github.v3+json")
                    }
                }
            }
        }

    suspend fun fetchUpdate(updateChannel: UpdateChannel): Result<Update?> {
        return getResponse("releases")
            .mapCatching<List<Release>, HttpResponse> { it.body() }
            .map { Update.from(it, updateChannel) }
            .onFailure { if (it !is CancellationException) Log.error(it) }
    }

    suspend fun downloadUpdate(file: File, url: String, onProgress: (Float) -> Unit): Result<Unit> = runCatching {
        useClient(withoutTimeout = true) {
            prepareGet(url).execute { httpResponse ->
                if (file.exists()) {
                    if (file.length() == httpResponse.contentLength()) {
                        onProgress(1f)
                        return@execute
                    } else {
                        Log.debug("Deleting outdated update file: ${file.absolutePath}")
                        file.delete()
                    }
                }
                val channel: ByteReadChannel = httpResponse.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        file.appendBytes(bytes)
                        httpResponse.contentLength()?.let {
                            onProgress(file.length().toFloat() / it)
                        }
                    }
                }
            }
        }
    }
}
