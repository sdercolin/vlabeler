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
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

/**
 * Handle business logic of audio playback.
 *
 * This class handles the actual playback of audio files, and notify the UI of the playback progress.
 *
 * @property playbackConfig The playback configuration.
 * @property maxSampleRate The maximum sample rate of the audio. Audio files with higher sample rate will be
 *     down-sampled.
 * @property coroutineScope The coroutine scope to run the player.
 * @property state The [PlayerState] containing all the state of the player.
 * @property listener The listener to notify the UI of the playback progress.
 */
@Stable
class Player(
    private var playbackConfig: AppConf.Playback,
    private var maxSampleRate: Int,
    private val coroutineScope: CoroutineScope,
    private val state: PlayerState,
    private val listener: Listener,
) : AudioSectionPlayer {
    /**
     * The listener to notify the UI of the playback progress.
     */
    interface Listener {

        /**
         * Called when the player starts playing.
         *
         * @param fromFrame The frame position to start playing from. Called with `null` if the player plays from the
         *     beginning.
         */
        fun onStartPlaying(fromFrame: Long? = null)

        /**
         * Called when the player stops playing.
         */
        fun onStopPlaying()

        /**
         * Called when the player detects a change in the frame position.
         *
         * @param position The updated frame position.
         */
        fun onFramePositionChanged(position: Float)
    }

    private var file: File? = null
    private var format: AudioFormat? = null
    private var line: SourceDataLine? = null
    private var data: ByteArray? = null
    private var openJob: Job? = null
    private var countingJob: Job? = null
    private var writingJob: Job? = null

    fun load(newFile: File) {
        if (file == newFile) return
        openJob?.cancel()
        file = newFile
        openJob = coroutineScope.launch {
            stop()
            withContext(Dispatchers.IO) {
                runCatching {
                    Log.debug("Player.load(\"${newFile.absolutePath}\")")
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
            listener.onStartPlaying()
            startCounting()
            startWriting()
        }
    }

    /**
     * Write the audio data to the output line.
     *
     * @param startFrame The frame position to start writing from.
     * @param endFrame The frame position to end writing at.
     * @param repeat Whether to repeat the audio data.
     * @param cancelFormer Whether to cancel the former writing job.
     * @param backgroundPlay Whether the playing is not showing in UI. If true, [state::isPlaying] is not set to true,
     *     so we should not check it in the loop.
     */
    private suspend fun startWriting(
        startFrame: Int = 0,
        endFrame: Int? = null,
        repeat: Boolean = false,
        cancelFormer: Boolean = true,
        backgroundPlay: Boolean = false,
    ) {
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
                var first = true
                while (state.isPlaying || (backgroundPlay && first)) {
                    line.write(data, offset, length)
                    line.drain()
                    if (state.isPlaying && !repeat) {
                        stop()
                    }
                    first = false
                }
            }.onFailure {
                if (it !is CancellationException) {
                    Log.error(it)
                }
            }
        }
    }

    private suspend fun startCounting(startFrame: Int = 0, endFrame: Int? = null, repeat: Boolean = false) {
        countingJob?.cancelAndJoin()
        countingJob = coroutineScope.launch(Dispatchers.Default) {
            var firstTime = System.currentTimeMillis()
            val firstFrame = startFrame.toFloat()
            var frame = firstFrame
            val sampleRate = format?.sampleRate ?: return@launch
            listener.onFramePositionChanged(frame)
            while (true) {
                delay(PLAYING_TIME_INTERVAL)
                if (!state.isPlaying) break
                frame = firstFrame + (System.currentTimeMillis() - firstTime) * sampleRate / 1000
                if (repeat) {
                    if (endFrame != null && frame > endFrame) {
                        frame = startFrame.toFloat() + frame - endFrame
                        firstTime = System.currentTimeMillis() - ((frame - startFrame) * 1000 / sampleRate).toLong()
                    }
                } else {
                    if (endFrame != null && frame > endFrame) break
                }
                listener.onFramePositionChanged(frame)
            }
        }
    }

    private fun playSection(frameRange: FloatRange) {
        playSection(frameRange.start.coerceAtLeast(0f), frameRange.endInclusive)
    }

    override fun playSection(startFrame: Float, endFrame: Float?, repeat: Boolean) {
        coroutineScope.launch {
            if (state.isPlaying) {
                stop()
            }
            Log.info("Player.playSection($startFrame, $endFrame, repeat=$repeat)")
            awaitLoad()
            val startFrameInt = startFrame.roundToInt()
            val endFrameInt = endFrame?.roundToInt()
            listener.onStartPlaying(startFrameInt.toLong())
            startCounting(startFrameInt, endFrameInt, repeat)
            startWriting(startFrameInt, endFrameInt, repeat = repeat)
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
                startWriting(startFrame, endFrame, cancelFormer = false, backgroundPlay = true)
                writingJob?.join()
                line?.flush()
            }
        }
    }

    private suspend fun stop() {
        Log.info("Player.stop()")
        listener.onStopPlaying()
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
        extraLine = null
        writingJob?.cancelAndJoin()
        writingJob = null
    }

    fun close() {
        Log.info("Player.close()")
        openJob?.cancel()
        openJob = null
        countingJob?.cancel()
        countingJob = null
        writingJob?.cancel()
        writingJob = null
        line?.run {
            stop()
            flush()
            close()
        }
        line = null
        extraLine?.run {
            stop()
            flush()
            close()
        }
        extraLine = null
        format = null
        data = null
        file = null
    }

    fun loadNewConfIfNeeded(appConf: AppConf) {
        if (playbackConfig != appConf.playback) {
            playbackConfig = appConf.playback
        }
        if (maxSampleRate != appConf.painter.amplitude.resampleDownToHz) {
            maxSampleRate = appConf.painter.amplitude.resampleDownToHz
        }
    }

    /**
     * An extra line to play audio data from [AudioPlaybackRequest]s. It's created by each request and should be cleaned
     * up after the request is finished.
     */
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
                listener.onStartPlaying()
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
                if (state.isPlaying) {
                    stop()
                }
            }
        }
    }

    companion object {
        private const val PLAYING_TIME_INTERVAL = 5L
    }
}
