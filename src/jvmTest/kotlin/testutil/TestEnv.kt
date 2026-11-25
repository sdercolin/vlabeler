package testutil

import com.sdercolin.vlabeler.env.Log
import java.io.File

/**
 * Prepares the environment required by production code paths that are exercised in tests.
 */
object TestEnv {

    /**
     * Ensures the logging directory exists. In the application it is created by [Log.init], which is not called in
     * tests, but creating a [com.sdercolin.vlabeler.util.JavaScript] instance with the default output stream opens
     * the info log file directly and fails if the directory is missing (e.g. on a clean CI machine).
     */
    fun ensureLogDirectory() {
        File(Log.LoggingPath).mkdirs()
    }
}
