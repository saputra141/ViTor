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
    val outputDirectory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
        "ViTor"
    ).apply { mkdirs() }
    val outputFile = File(outputDirectory, "ViTor_${System.currentTimeMillis()}.mp4")

    val clippedItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
        )
        .build()

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
        Transformer.Builder(context)
            .addListener(listener)
            .build()
            .start(clippedItem, outputFile.absolutePath)
    } catch (error: Exception) {
        outputFile.delete()
        onFailure(error.message ?: "Tidak dapat memulai proses ekspor.")
    }
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