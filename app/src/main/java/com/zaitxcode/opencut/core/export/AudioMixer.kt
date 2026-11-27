package com.zaitxcode.opencut.core.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.zaitxcode.opencut.core.model.Project
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Mixes the audio layers of a project and encodes the result with AAC.
 *
 * Every audio segment is decoded to 16 bit PCM, resampled to a common sample rate,
 * mixed into a stereo buffer and finally encoded into the same MP4 container as the
 * video track.
 */
class AudioMixer(private val context: Context) {

    private val sampleRate = 44_100
    private val channels = 2

    /**
     * Encodes the project's audio.
     *
     * The audio is encoded before the container is started, because a muxer only
     * accepts a new track while it has not been started yet. The result carries the
     * output format of the encoder and the encoded samples, so that the exporter
     * can add the track and write the samples in the right order.
     */
    fun encode(
        project: Project,
        durationUs: Long,
    ): EncodedAudio? {
        val plans = audioPlans(project)
        if (plans.isEmpty()) return null

        val totalSamples = maxOf(1, (durationUs * sampleRate / 1_000_000L).toInt())
        // Accumulate in 32-bit space and clamp once at the end. Clamping after
        // every layer would make the result depend on track order and distort mixes.
        val accumulator = IntArray(totalSamples * channels)

        for (plan in plans) {
            val pcm = decodeToPcm(plan.uri) ?: continue
            val gain = (plan.volume * 0.8f).coerceIn(0f, 4f)
            val targetStart = (plan.startUs * sampleRate / 1_000_000L).toInt()
                .coerceAtLeast(0)
            val sourceStart = (plan.sourceStartUs * sampleRate / 1_000_000L).toInt()
                .coerceAtLeast(0)
            val sourceFrames = (plan.sourceDurationUs * sampleRate / 1_000_000L)
                .coerceAtLeast(0L)
            val targetFrames = (plan.targetDurationUs * sampleRate / 1_000_000L)
                .coerceAtLeast(0L)
            val framesToMix = minOf(targetFrames, (totalSamples - targetStart).toLong())
            if (framesToMix <= 0L || sourceFrames <= 0L) continue

            // Map each output frame across the segment's trimmed source window.
            // This honours both left/right trims and speed changes.
            for (frame in 0 until framesToMix.toInt()) {
                val sourceFrame = sourceStart + (
                    frame.toDouble() * sourceFrames / targetFrames
                ).toInt()
                if (sourceFrame >= pcm.size / channels) break
                val destinationFrame = targetStart + frame
                for (channel in 0 until channels) {
                    val sourceChannel = if (pcm.size / channels == 1) 0 else channel
                    val sample = (pcm[sourceFrame * channels + sourceChannel] * gain).toInt()
                    val destination = destinationFrame * channels + channel
                    accumulator[destination] += sample
                }
            }
        }

        val mixed = ShortArray(accumulator.size) { index ->
            accumulator[index].coerceIn(-32768, 32767).toShort()
        }
        return encodeAac(mixed, totalSamples)
    }

    /** One audio layer of the project. */
    private data class AudioPlan(
        val uri: Uri,
        val startUs: Long,
        val sourceStartUs: Long,
        val sourceDurationUs: Long,
        val targetDurationUs: Long,
        val volume: Float,
    )

    private fun audioPlans(project: Project): List<AudioPlan> {
        val plans = mutableListOf<AudioPlan>()
        for (track in project.tracks) {
            if (track.type != com.zaitxcode.opencut.core.model.TrackType.AUDIO || track.muted) continue
            for (segment in track.segments) {
                if (segment.muted) continue
                val material = project.materials.find(segment.materialId) ?: continue
                if (material.path.isBlank()) continue
                plans += AudioPlan(
                    uri = Uri.parse(material.path),
                    startUs = segment.target.start,
                    sourceStartUs = segment.source.start,
                    sourceDurationUs = segment.source.duration,
                    targetDurationUs = segment.target.duration,
                    volume = segment.volume,
                )
            }
        }
        return plans
    }

    /**
     * Decodes the audio track of a file into 16 bit PCM.
     *
     * The decoder output is converted to the target sample rate with linear
     * interpolation, which keeps the mixer simple while avoiding obvious pitch
     * artefacts.
     */
    private fun decodeToPcm(uri: Uri): ShortArray? = runCatching {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, emptyMap())
        var trackIndex = -1
        var format: MediaFormat? = null
        for (index in 0 until extractor.trackCount) {
            val candidate = extractor.getTrackFormat(index)
            val mime = candidate.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                trackIndex = index
                format = candidate
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            return null
        }
        extractor.selectTrack(trackIndex)

        val sourceRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val output = ArrayList<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        while (true) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    val size = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val buffer = codec.getOutputBuffer(outputIndex)
                if (buffer != null && bufferInfo.size > 0) {
                    buffer.position(bufferInfo.offset)
                    buffer.limit(bufferInfo.offset + bufferInfo.size)
                    val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (shorts.hasRemaining()) output.add(shorts.get())
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                break
            }
        }
        codec.stop()
        codec.release()
        extractor.release()

        val raw = ShortArray(output.size) { output[it] }
        // The mixer works in stereo. Duplicate mono sources and downmix wider
        // layouts by taking their first two channels instead of misreading samples.
        val sourceFrames = raw.size / sourceChannels
        val stereo = ShortArray(sourceFrames * channels)
        for (frame in 0 until sourceFrames) {
            val left = raw[frame * sourceChannels]
            val right = if (sourceChannels > 1) raw[frame * sourceChannels + 1] else left
            stereo[frame * channels] = left
            stereo[frame * channels + 1] = right
        }
        if (sourceRate == sampleRate) stereo else resample(stereo, sourceRate)
    }.getOrNull()

    /** Linear resampling of interleaved 16 bit PCM. */
    private fun resample(input: ShortArray, sourceRate: Int): ShortArray {
        if (input.isEmpty() || sourceRate <= 0) return input
        val sourceFrames = input.size / channels
        val targetFrames = maxOf(1L, sourceFrames.toLong() * sampleRate / sourceRate).toInt()
        val output = ShortArray(targetFrames * channels)
        for (frame in 0 until targetFrames) {
            val sourcePosition = frame.toDouble() * sourceRate / sampleRate
            val index = sourcePosition.toInt().coerceIn(0, maxOf(0, sourceFrames - 1))
            val next = minOf(index + 1, maxOf(0, sourceFrames - 1))
            val fraction = (sourcePosition - index).toFloat()
            for (channel in 0 until channels) {
                val a = input[index * channels + channel].toFloat()
                val b = input[next * channels + channel].toFloat()
                output[frame * channels + channel] = (a + (b - a) * fraction).toInt().toShort()
            }
        }
        return output
    }

    /** Encodes the mixed PCM with AAC. */
    private fun encodeAac(pcm: ShortArray, sampleCount: Int): EncodedAudio? {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channels,
        ).apply {
            setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var outputFormat: MediaFormat? = null
        val samples = mutableListOf<EncodedSample>()
        var sampleOffset = 0

        var inputDone = false
        while (true) {
            if (!inputDone) {
                val inputIndex = encoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null && sampleOffset < sampleCount) {
                        val chunkSamples = minOf(inputBuffer.capacity() / 2 / channels, sampleCount - sampleOffset)
                        val bytes = ByteBuffer.allocate(chunkSamples * channels * 2)
                            .order(ByteOrder.nativeOrder())
                        for (i in 0 until chunkSamples * channels) {
                            bytes.putShort(pcm[sampleOffset * channels + i])
                        }
                        val data = bytes.array()
                        inputBuffer.clear()
                        inputBuffer.put(data)
                        encoder.queueInputBuffer(
                            inputIndex,
                            0,
                            data.size,
                            sampleOffset * 1_000_000L / sampleRate,
                            0,
                        )
                        sampleOffset += chunkSamples
                    } else {
                        // The end of the stream lets the encoder flush the packets
                        // it still holds, so the track is not cut short.
                        encoder.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    }
                }
            }
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormat = encoder.outputFormat
                continue
            }
            if (outputIndex >= 0) {
                val encoded = encoder.getOutputBuffer(outputIndex)
                if (encoded != null && bufferInfo.size > 0) {
                    encoded.position(bufferInfo.offset)
                    encoded.limit(bufferInfo.offset + bufferInfo.size)
                    val copy = ByteArray(bufferInfo.size)
                    encoded.get(copy)
                    samples += EncodedSample(
                        data = copy,
                        presentationTimeUs = bufferInfo.presentationTimeUs,
                        flags = bufferInfo.flags,
                    )
                }
                encoder.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        encoder.stop()
        encoder.release()
        val encodedFormat = outputFormat ?: return null
        return EncodedAudio(format = encodedFormat, samples = samples)
    }
}

/** The encoded audio of a project, ready to be written into a container. */
data class EncodedAudio(
    val format: MediaFormat,
    val samples: List<EncodedSample>,
)

/** One encoded audio packet. */
data class EncodedSample(
    val data: ByteArray,
    val presentationTimeUs: Long,
    val flags: Int,
)
