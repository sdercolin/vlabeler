package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.util.AppDir
import java.io.File
import java.time.Instant
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.StreamHandler

object Log {

    private val LoggingPath = AppDir.resolve(".logs").absolutePath
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
    private val errorStreamHandler = StreamHandler(System.err, formatter)

    fun init() {
        val loggingDir = File(LoggingPath)
        if (loggingDir.exists().not()) loggingDir.mkdir()

        val infoHandler = FileHandler("$LoggingPath/$InfoLogFileName", true)
        infoHandler.formatter = formatter
        infoLogger.useParentHandlers = false
        infoLogger.addHandler(infoHandler)
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
        debug("Launched in $osName, $runtimeVersion, idDebug=$isDebug")
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
