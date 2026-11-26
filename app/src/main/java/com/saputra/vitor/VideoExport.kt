package com.saputra.vitor

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileInputStream

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

    startExport(
        context = context,
        outputFile = outputFile,
        onComplete = onComplete,
        onFailure = onFailure
    ) { transformer ->
        transformer.start(clippedItem, outputFile.absolutePath)
    }
}

/**
 * Concatenates clips in the exact order supplied by the editor timeline.
 * Media3 transcodes incompatible inputs as needed; unsupported codec/track
 * combinations are reported through onFailure instead of silently dropping clips.
 */
@UnstableApi
fun exportMergedClips(
    context: Context,
    clipUris: List<Uri>,
    onComplete: (String?) -> Unit,
    onFailure: (String) -> Unit
) {
    require(clipUris.size >= 2) { "Pilih minimal dua klip untuk digabung." }
    val outputFile = createOutputFile(context)
    val editedItems = clipUris.map { uri ->
        EditedMediaItem.Builder(MediaItem.fromUri(uri)).build()
    }
    val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
    val composition = Composition.Builder(videoSequence).build()

    startExport(
        context = context,
        outputFile = outputFile,
        onComplete = onComplete,
        onFailure = onFailure
    ) { transformer ->
        transformer.start(composition, outputFile.absolutePath)
    }
}

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
            onComplete(galleryUri?.toString())
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
    val destination = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null

    return try {
        resolver.openOutputStream(destination)?.use { output ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Tidak dapat membuka file tujuan.")
        val completed = ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        resolver.update(destination, completed, null, null)
        destination
    } catch (_: Exception) {
        resolver.delete(destination, null, null)
        null
    }
}