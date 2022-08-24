package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.ResourcePath
import java.io.File
import java.time.Instant
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.StreamHandler

object Log {

    val LoggingPath: String = AppDir.resolve(".logs").absolutePath
    private const val InfoLogFileName = "info.log"
    private const val ErrorLogFileName = "error.log"
    private val infoLogger = Logger.getLogger("info")
    private val errorLogger = Logger.getLogger("error")
    private val formatter = object : Formatter() {
        override fun format(record: LogRecord?): String {
            record ?: return ""
            val tag = "[${record.instant}]"
            val message = formatMessage(record)
            val throwable = record.thrown?.stackTraceToString()
            return listOfNotNull(tag, message, throwable).joinToString(" ") + "\n"
        }
    }
    lateinit var infoFileHandler: FileHandler
        private set
    private val errorStreamHandler = StreamHandler(System.err, formatter)

    fun init() {
        val loggingDir = File(LoggingPath)
        if (loggingDir.exists().not()) loggingDir.mkdirs()
        infoFileHandler = FileHandler("$LoggingPath/$InfoLogFileName", true)
        infoFileHandler.formatter = formatter
        infoLogger.useParentHandlers = false
        infoLogger.addHandler(infoFileHandler)
        infoLogger.level = Level.INFO

        val errorHandler = FileHandler("$LoggingPath/$ErrorLogFileName", true)
        errorHandler.formatter = formatter
        errorLogger.useParentHandlers = false
        errorLogger.addHandler(errorHandler)
        errorLogger.addHandler(errorStreamHandler)
        errorLogger.level = Level.SEVERE

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            error("Uncaught exception in Thread ${t.name}: ${e.stackTraceToString()}")
        }

        info("Log initialized")
        val launchInfo = mapOf(
            "javaRuntimeVersion" to runtimeVersion,
            "appVersion" to appVersion,
            "debug" to isDebug,
            "workingDir" to ResourcePath,
        )
        debug("Launched in $osName, ${launchInfo.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
    }

    fun info(message: String) {
        infoLogger.fine(message)
        println("[${Instant.now()}] INFO: $message")
    }

    fun debug(message: String) {
        infoLogger.info(message)
        println("[${Instant.now()}] DEBUG: $message")
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
}
