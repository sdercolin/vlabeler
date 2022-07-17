package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.model.SampleInfo
import java.io.File

@Stable
object SampleRepository {

    private val map = mutableMapOf<String, Sample>()

    fun load(file: File, appConf: AppConf): Result<SampleInfo> {
        val sample = loadSampleFile(file, appConf).getOrElse {
            return Result.failure(it)
        }
        map[sample.info.name] = sample
        return Result.success(sample.info)
    }

    fun retrieve(name: String): Sample = map.getValue(name).also { map.remove(name) }

    fun clear() {
        map.clear()
        Log.info("SampleRepository clear()")
    }
}
