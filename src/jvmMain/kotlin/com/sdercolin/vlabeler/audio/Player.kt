package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.normalize
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AudioPlaybackRequest
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.JobQueue
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

@Stable
class Player(
    private var playbackConfig: AppConf.Playback,
    private var maxSampleRate: Int,
    private val coroutineScope: CoroutineScope,
    private val state: PlayerState,
) {
    private var file: File? = null
    private var format: AudioFormat? = null
    private var line: SourceDataLine? = null
    private var data: ByteArray? = null
    private var openJob: Job? = null
    private var countingJob: Job? = null
    private var writingJob: Job? = null

    suspend fun load(newFile: File) {
        if (file == newFile) return
        openJob?.cancelAndJoin()
        stop()
        file = newFile
        openJob = coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                Log.info("Player.load(\"${newFile.absolutePath}\")")
                val line = AudioSystem.getAudioInputStream(newFile).use { stream ->
                    format = stream.format.normalize(maxSampleRate)
                    data = AudioSystem.getAudioInputStream(format, stream).use {
                        val bytes = it.readAllBytes()
                        Log.info("Player.load: read ${bytes.size} bytes")
                        bytes
                    }
                    AudioSystem.getSourceDataLine(format)
                }
                this@Player.line = line
                line.open()
            }.onFailure {
                Log.error(it)
            }
        }
    }

    fun toggle(frameRange: FloatRange? = null) {
        file ?: return
        coroutineScope.launch {
            if (state.isPlaying) stop() else {
                if (frameRange != null) {
                    playSection(frameRange)
                } else {
                    play()
                }
            }
        }
    }

    private suspend fun awaitLoad() {
        openJob?.join()
    }

    private fun play() {
        coroutineScope.launch {
            Log.info("Player.play()")
            awaitLoad()
            state.startPlaying()
            startCounting()
            startWriting()
        }
    }

    private suspend fun startWriting(startFrame: Int = 0, endFrame: Int? = null, cancelFormer: Boolean = true) {
        if (cancelFormer) writingJob?.cancelAndJoin() else writingJob?.join()
        val line = line ?: return
        val format = format ?: return
        val data = data ?: return
        line.start()
        writingJob = coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val offset = startFrame * format.frameSize
                val length = endFrame?.let { (it - startFrame) * format.frameSize }
                    ?.coerceAtMost(data.size - offset)
                    ?: (data.size - offset)
                line.write(data, offset, length)
                line.drain()
                if (state.isPlaying) {
                    stop()
                }
            }.onFailure {
                if (it !is CancellationException) {
                    Log.error(it)
                }
            }
        }
    }

    private suspend fun startCounting(startFrame: Int = 0, endFrame: Int? = null) {
        countingJob?.cancelAndJoin()
        countingJob = coroutineScope.launch(Dispatchers.Default) {
            val firstTime = System.currentTimeMillis()
            val firstFrame = startFrame.toFloat()
            var frame = firstFrame
            val sampleRate = format?.sampleRate ?: return@launch
            state.setFramePosition(frame)
            while (true) {
                delay(PlayingTimeInterval)
                if (!state.isPlaying) break
                frame = firstFrame + (System.currentTimeMillis() - firstTime) * sampleRate / 1000
                if (endFrame != null && frame > endFrame) break
                state.setFramePosition(frame)
            }
        }
    }

    private fun playSection(frameRange: FloatRange) {
        playSection(frameRange.start, frameRange.endInclusive)
    }

    fun playSection(startFramePosition: Float, endFramePosition: Float) {
        coroutineScope.launch {
            if (state.isPlaying) {
                stop()
            }
            Log.info("Player.playSection($startFramePosition, $endFramePosition)")
            awaitLoad()
            val startFrame = startFramePosition.roundToInt()
            val endFrame = endFramePosition.roundToInt()
            state.startPlaying()
            startCounting(startFrame, endFrame)
            startWriting(startFrame, endFrame)
        }
    }

    private val playByCursorJobQueue = JobQueue(coroutineScope, playbackConfig.playOnDragging.eventQueueSize)

    fun playByCursor(frame: Float) {
        playByCursorJobQueue.post {
            coroutineScope.launch {
                if (state.isPlaying) {
                    stop()
                }
                Log.info("Player.playByCursor($frame)")
                awaitLoad()
                val format = format ?: return@launch
                val data = data ?: return@launch
                val totalFrameCount = (data.size - 1) / format.frameSize
                val radius = playbackConfig.playOnDragging.rangeRadiusMillis
                val centerFrame = frame.roundToInt()
                if (centerFrame < 0 || centerFrame >= totalFrameCount) return@launch
                val radiusFrame = radius * format.sampleRate / 1000
                val startFrame = (centerFrame - radiusFrame).roundToInt().coerceAtLeast(0)
                val endFrame = (centerFrame + radiusFrame)
                    .roundToInt()
                    .coerceAtMost(totalFrameCount)

                startWriting(startFrame, endFrame, cancelFormer = false)
                writingJob?.join()
                line?.flush()
            }
        }
    }

    private suspend fun stop() {
        Log.info("Player.stop()")
        state.stopPlaying()
        countingJob?.cancelAndJoin()
        countingJob = null
        line?.run {
            stop()
            flush()
        }
        extraLine?.run {
            stop()
            flush()
        }
        writingJob?.cancelAndJoin()
        writingJob = null
    }

    fun close() {
        Log.info("Player.close()")
        openJob?.cancel()
        countingJob?.cancel()
        line?.run {
            stop()
            flush()
            close()
        }
        extraLine?.run {
            stop()
            flush()
            close()
        }
    }

    fun loadNewConfIfNeeded(appConf: AppConf) {
        if (playbackConfig != appConf.playback) {
            playbackConfig = appConf.playback
        }
        if (maxSampleRate != appConf.painter.amplitude.resampleDownToHz) {
            maxSampleRate = appConf.painter.amplitude.resampleDownToHz
        }
    }

    private var extraLine: SourceDataLine? = null

    fun handleRequest(request: AudioPlaybackRequest) {
        when (request) {
            is AudioPlaybackRequest.PlayFile -> coroutineScope.launch(Dispatchers.IO) {
                if (state.isPlaying) {
                    stop()
                }
                Log.info("Player.load(\"${request.path}\")")
                val file = request.path.toFile()
                val stream = AudioSystem.getAudioInputStream(file)
                val format = stream.format.normalize(maxSampleRate)
                val data = AudioSystem.getAudioInputStream(format, stream).use {
                    val bytes = it.readAllBytes()
                    Log.info("Player.load: read ${bytes.size} bytes")
                    bytes
                }
                val line = AudioSystem.getSourceDataLine(format)
                stream.close()
                this@Player.extraLine = line
                line.open()
                line.start()
                state.startPlaying()
                writingJob = coroutineScope.launch(Dispatchers.IO) {
                    val offset = (toFrame(request.offset.toFloat(), format.sampleRate) * format.frameSize).toInt()
                    val length = request.duration?.let { toFrame(it.toFloat(), format.sampleRate) * format.frameSize }
                        ?.toInt()
                        ?.coerceAtMost(data.size)
                        ?: data.size
                    line.write(data, offset, length)
                    line.drain()
                    line.flush()
                }
                writingJob?.join()
            }
        }
    }

    companion object {
        private const val PlayingTimeInterval = 5L
    }
}
