package com.saputra.vitor

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.graphics.Color as AndroidColor
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.TextOverlay
import java.io.File
import java.io.FileInputStream
import com.saputra.vitor.subtitles.SubtitleCue

enum class VideoLook { ORIGINAL, GRAYSCALE, HIGH_CONTRAST }

/** Exports a selected time range as a new MP4. The source file is never modified. */
@UnstableApi
fun exportTrimmedClip(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    onComplete: (String?) -> Unit,
    onFailure: (String) -> Unit
) {
    require(startMs >= 0L && endMs > startMs) { "Rentang potong tidak valid." }
    val outputFile = createOutputFile(context)
    val clippedItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
        )
        .build()
    startExport(context, outputFile, onComplete, onFailure) { transformer ->
        transformer.start(clippedItem, outputFile.absolutePath)
    }
}

/**
 * Exports a multi-layer edit: ordered video clips, optional looping background music,
 * optional static title overlay, and a GPU video look.
 */
@UnstableApi
fun exportEditedProject(
    context: Context,
    clipUris: List<Uri>,
    musicUri: Uri?,
    musicVolume: Float,
    titleText: String,
    look: VideoLook,
    onComplete: (String?) -> Unit,
    onFailure: (String) -> Unit,
    clipRanges: List<Pair<Long, Long>>? = null,
    subtitleCues: List<SubtitleCue> = emptyList()
) {
    require(clipUris.isNotEmpty()) { "Tambahkan minimal satu klip video." }
    require(musicVolume in 0f..1f) { "Volume musik harus antara 0 dan 100%." }

    val overlayEffects = buildList<androidx.media3.common.Effect> {
        when (look) {
            VideoLook.ORIGINAL -> Unit
            VideoLook.GRAYSCALE -> add(RgbFilter.createGrayscaleFilter())
            VideoLook.HIGH_CONTRAST -> add(Contrast(0.35f))
        }
        if (titleText.isNotBlank()) {
            val text = SpannableString(titleText.trim()).apply {
                setSpan(
                    ForegroundColorSpan(AndroidColor.WHITE),
                    0,
                    length,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            val settings = OverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, -0.78f)
                .setOverlayFrameAnchor(0f, 0f)
                .build()
            add(OverlayEffect(listOf(TextOverlay.createStaticTextOverlay(text, settings))))
        }
    }

    require(clipRanges == null || clipRanges.size == clipUris.size) {
        "Jumlah rentang trim harus sama dengan jumlah klip."
    }
    var timelineOffsetMs = 0L
    val videoItems = clipUris.mapIndexed { index, uri ->
        val range = clipRanges?.get(index)
        val mediaItem = if (range == null) {
            MediaItem.fromUri(uri)
        } else {
            require(range.first >= 0L && range.second > range.first) { "Rentang trim klip tidak valid." }
            MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(range.first)
                        .setEndPositionMs(range.second)
                        .build()
                )
                .build()
        }
        val itemVideoEffects = buildList<androidx.media3.common.Effect> {
            addAll(overlayEffects)
            if (subtitleCues.isNotEmpty()) {
                add(OverlayEffect(listOf(TimedSubtitleOverlay(subtitleCues, timelineOffsetMs))))
            }
        }
        timelineOffsetMs += range?.let { (it.second - it.first).coerceAtLeast(0L) } ?: 0L
        EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), itemVideoEffects))
            .build()
    }
    val videoSequence = EditedMediaItemSequence.Builder(videoItems).build()
    val sequences = mutableListOf(videoSequence)

    if (musicUri != null) {
        val musicEffects = if (musicVolume < 0.999f) {
            listOf(GainAudioProcessor(musicVolume))
        } else {
            emptyList()
        }
        val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(musicUri))
            .setRemoveVideo(true)
            .setEffects(Effects(musicEffects, emptyList()))
            .build()
        sequences.add(
            EditedMediaItemSequence.Builder(musicItem)
                .setIsLooping(true)
                .build()
        )
    }

    val composition = Composition.Builder(sequences).build()
    val outputFile = createOutputFile(context)
    startExport(context, outputFile, onComplete, onFailure) { transformer ->
        transformer.start(composition, outputFile.absolutePath)
    }
}

@UnstableApi
fun exportMergedClips(
    context: Context,
    clipUris: List<Uri>,
    onComplete: (String?) -> Unit,
    onFailure: (String) -> Unit
) = exportEditedProject(
    context = context,
    clipUris = clipUris,
    musicUri = null,
    musicVolume = 1f,
    titleText = "",
    look = VideoLook.ORIGINAL,
    onComplete = onComplete,
    onFailure = onFailure
)

@UnstableApi
private fun startExport(
    context: Context,
    outputFile: File,
    onComplete: (String?) -> Unit,
    onFailure: (String) -> Unit,
    start: (Transformer) -> Unit
) {
    val listener = object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            val galleryUri = publishToGallery(context, outputFile)
            onComplete(galleryUri?.toString() ?: outputFile.absolutePath)
        }

        override fun onError(
            composition: Composition,
            exportResult: ExportResult,
            exportException: ExportException
        ) {
            outputFile.delete()
            onFailure(exportException.message ?: "Terjadi kesalahan saat merender video.")
        }
    }
    try {
        val transformer = Transformer.Builder(context)
            .addListener(listener)
            .build()
        start(transformer)
    } catch (error: Exception) {
        outputFile.delete()
        onFailure(error.message ?: "Tidak dapat memulai proses ekspor.")
    }
}

private fun createOutputFile(context: Context): File {
    val outputDirectory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
        "ViTor"
    ).apply { mkdirs() }
    return File(outputDirectory, "ViTor_${System.currentTimeMillis()}.mp4")
}

private fun publishToGallery(context: Context, source: File): Uri? {
    if (!source.exists() || source.length() == 0L) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        MediaScannerConnection.scanFile(
            context, arrayOf(source.absolutePath), arrayOf("video/mp4"), null
        )
        return null
    }
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ViTor")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val destination = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        resolver.openOutputStream(destination)?.use { output ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Tidak dapat membuka file tujuan.")
        resolver.update(destination, ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }, null, null)
        destination
    } catch (_: Exception) {
        resolver.delete(destination, null, null)
        null
    }
}

/**
 * Draws the active subtitle cue at its timeline timestamp. The overlay is transparent
 * outside cue ranges, so subtitle timing is preserved in the rendered MP4.
 */
@androidx.media3.common.util.UnstableApi
private class TimedSubtitleOverlay(
    private val cues: List<SubtitleCue>,
    private val timelineOffsetMs: Long
) : BitmapOverlay() {
    private val frameWidth = 1280
    private val frameHeight = 720
    private val emptyBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timelineMs = timelineOffsetMs + presentationTimeUs / 1_000L
        val cue = cues.firstOrNull { timelineMs >= it.startMs && timelineMs < it.endMs } ?: return emptyBitmap
        return bitmapCache.getOrPut(cue.id) { createCueBitmap(cue) }
    }

    private fun createCueBitmap(cue: SubtitleCue): Bitmap {
        val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        val layout = StaticLayout.Builder.obtain(
            cue.text, 0, cue.text.length, textPaint, (frameWidth * 0.88f).toInt()
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()
        val horizontalPadding = 42f
        val verticalPadding = 18f
        val left = (frameWidth - layout.width) / 2f - horizontalPadding
        val top = frameHeight * 0.80f - verticalPadding
        val right = left + layout.width + horizontalPadding * 2f
        val bottom = top + layout.height + verticalPadding * 2f
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(175, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(left, top, right, bottom), 18f, 18f, backgroundPaint)
        canvas.save()
        canvas.translate((frameWidth - layout.width) / 2f, top + verticalPadding)
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings =
        OverlaySettings.Builder()
            .setBackgroundFrameAnchor(0f, 0f)
            .setOverlayFrameAnchor(0f, 0f)
            .build()
}

/** PCM gain processor used for the background music track during export. */
@androidx.media3.common.util.UnstableApi
private class GainAudioProcessor(private val gain: Float) :
    androidx.media3.common.audio.BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: androidx.media3.common.audio.AudioProcessor.AudioFormat):
        androidx.media3.common.audio.AudioProcessor.AudioFormat {
        return if (
            inputAudioFormat.encoding == androidx.media3.common.C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == androidx.media3.common.C.ENCODING_PCM_FLOAT
        ) inputAudioFormat else androidx.media3.common.audio.AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: java.nio.ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        when (inputAudioFormat.encoding) {
            androidx.media3.common.C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.short.toInt()
                    val adjusted = (sample * gain).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    output.putShort(adjusted.toShort())
                }
            }
            androidx.media3.common.C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    val sample = inputBuffer.float
                    output.putFloat((sample * gain).coerceIn(-1f, 1f))
                }
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }
}