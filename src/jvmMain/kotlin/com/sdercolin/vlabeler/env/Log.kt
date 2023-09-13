package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.flag.FeatureFlags
import com.sdercolin.vlabeler.hasUncaughtError
import com.sdercolin.vlabeler.tracking.event.FatalErrorEvent
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.ResourcePath
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.StreamHandler

/**
 * The logger for the application.
 */
object Log {

    val LoggingPath: String = AppDir.resolve(".logs").absolutePath
    private const val INFO_LOG_FILE_NAME = "info.log"
    private const val ERROR_LOG_FILE_NAME = "error.log"
    private val infoLogger = Logger.getLogger("info")
    private val errorLogger = Logger.getLogger("error")
    private val formatter = object : Formatter() {
        override fun format(record: LogRecord?): String {
            record ?: return ""
            val tag = "[${record.instant}]"
            val message = formatMessage(record)
            val lines = message.split("\n")
            val throwable = record.thrown?.stackTraceToString()
            return lines.joinToString("") { listOfNotNull(tag, it, throwable).joinToString(" ") + "\n" }
        }
    }
    private lateinit var infoFileHandler: FileHandler
    private val errorStreamHandler = StreamHandler(System.err, formatter)

    fun getInfoOutputStream() = FileOutputStream("$LoggingPath/$INFO_LOG_FILE_NAME", true)

    fun init() {
        val loggingDir = File(LoggingPath)
        if (loggingDir.exists().not()) loggingDir.mkdirs()
        infoFileHandler = FileHandler("$LoggingPath/$INFO_LOG_FILE_NAME", true)
        infoFileHandler.formatter = formatter
        infoLogger.useParentHandlers = false
        infoLogger.addHandler(infoFileHandler)
        infoLogger.level = Level.INFO

        val errorHandler = FileHandler("$LoggingPath/$ERROR_LOG_FILE_NAME", true)
        errorHandler.formatter = formatter
        errorLogger.useParentHandlers = false
        errorLogger.addHandler(errorHandler)
        errorLogger.addHandler(errorStreamHandler)
        errorLogger.level = Level.SEVERE

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            hasUncaughtError = true
            error("Uncaught exception in Thread ${t.name}: ${e.stackTraceToString()}")
            fatal(e)
        }

        info("Log initialized")
        val flags = FeatureFlags.all.filter { it.get() }.joinToString { it.key }
        val launchInfo = mapOf(
            "javaRuntimeVersion" to runtimeVersion,
            "appVersion" to appVersion,
            "debug" to isDebug,
            "workingDir" to ResourcePath,
            "appDir" to AppDir,
            "locale" to Locale,
            "flags" to flags,
        )
        debug("Launched in $osInfo, ${launchInfo.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
    }

    fun info(message: String) {
        infoLogger.fine(message)
        message.lines().forEach {
            println("[${Instant.now()}] INFO: $it")
        }
    }

    fun debug(message: String) {
        infoLogger.info(message)
        message.lines().forEach {
            println("[${Instant.now()}] DEBUG: $it")
        }
    }

    fun debug(exception: Throwable) {
        debug(exception.stackTraceToString())
    }

    fun error(message: String) {
        infoLogger.severe(message)
        errorLogger.severe(message)
        errorStreamHandler.flush()
    }

    fun error(exception: Throwable) {
        error(exception.stackTraceToString())
    }

    private fun fatal(exception: Throwable) {
        fatalErrorTracker?.track(
            FatalErrorEvent(
                appVersion = appVersion.toString(),
                runtime = runtimeVersion.toString(),
                osInfo = osInfo,
                isDebug = isDebug,
                locale = Locale.toString(),
                error = exception.stackTraceToString(),
            ),
        )
    }

    var fatalErrorTracker: FatalErrorTracker? = null

    fun enableFineLogging(enabled: Boolean) {
        infoLogger.level = if (enabled) Level.FINE else Level.INFO
    }

    var muted: Boolean = false
        set(value) {
            field = value
            infoLogger.level = if (value) Level.OFF else Level.INFO
            errorLogger.level = if (value) Level.OFF else Level.SEVERE
        }

    fun interface FatalErrorTracker {
        fun track(fatalErrorEvent: FatalErrorEvent)
    }
}
