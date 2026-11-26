package com.saputra.vitor

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private data class EditClip(val uri: Uri, val name: String, val durationMs: Long)

class VideoEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF9A8CFF),
                secondary = Color(0xFF56D6C2),
                background = Color(0xFF101116),
                surface = Color(0xFF1A1B23)
            )) {
                VideoEditorScreen()
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoEditorScreen() {
    val context = LocalContext.current
    val clips = remember { mutableStateListOf<EditClip>() }
    var selected by remember { mutableStateOf<EditClip?>(null) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var exporting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Import video untuk mulai mengedit.") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
                }
            }.getOrNull() ?: "Video ${clips.size + 1}"
            val duration = readDuration(context, uri)
            val clip = EditClip(uri, name, duration)
            clips.add(clip)
            selected = clip
            trimRange = 0f..duration.coerceAtLeast(1L).toFloat()
            status = if (duration > 0) "Klip siap diedit." else "Durasi video tidak dapat dibaca."
        }
    }

    val player = remember { ExoPlayer.Builder(context).build() }
    LaunchedEffect(selected?.uri) {
        selected?.let {
            player.setMediaItem(MediaItem.fromUri(it.uri))
            player.prepare()
            player.playWhenReady = false
            trimRange = 0f..it.durationMs.coerceAtLeast(1L).toFloat()
        } ?: player.clearMediaItems()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF101116)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ViTor Studio", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Editor video native Android", color = Color(0xFFAAAAB8), fontSize = 12.sp)
                }
                Button(onClick = { picker.launch(arrayOf("video/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            }

            Card(
                Modifier.fillMaxWidth().height(230.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                if (selected != null) {
                    AndroidView(
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { playerView -> playerView.player = player }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, null, tint = Color(0xFF9A8CFF), modifier = Modifier.size(44.dp))
                            Text("Preview proyek", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Import klip video untuk mulai", color = Color(0xFF9292A0), fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE VIDEO", color = Color(0xFFAAAAB8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${clips.size} klip", color = Color(0xFFAAAAB8), fontSize = 11.sp)
            }
            if (clips.isEmpty()) {
                Text("Timeline masih kosong. Tambahkan video dengan tombol Import.", color = Color(0xFF9292A0), fontSize = 12.sp)
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    clips.forEach { clip ->
                        Card(
                            onClick = { selected = clip },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected == clip) Color(0xFF383253) else Color(0xFF222330)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.width(210.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, null, tint = Color(0xFFAAA0FF))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(clip.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(formatTime(clip.durationMs), color = Color(0xFFAAAAB8), fontSize = 10.sp)
                                }
                                IconButton(onClick = {
                                    clips.remove(clip)
                                    if (selected == clip) selected = clips.lastOrNull()
                                }) {
                                    Icon(Icons.Default.Delete, "Hapus klip", tint = Color(0xFFFF8A8A))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("POTONG KLIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (selected != null && selected!!.durationMs > 0L) {
                        Text(
                            "${formatTime(trimRange.start.toLong())}  —  ${formatTime(trimRange.endInclusive.toLong())}  /  ${formatTime(selected!!.durationMs)}",
                            color = Color(0xFFBDB8FF), fontSize = 12.sp
                        )
                        RangeSlider(
                            value = trimRange,
                            onValueChange = { trimRange = it },
                            valueRange = 0f..selected!!.durationMs.toFloat(),
                            steps = 0
                        )
                        Text("Atur titik awal dan akhir. Bagian di luar rentang tidak akan masuk ke hasil.", color = Color(0xFF9292A0), fontSize = 10.sp)
                        Button(
                            onClick = {
                                val clip = selected ?: return@Button
                                exporting = true
                                status = "Merender video..."
                                exportTrimmedClip(
                                    context, clip.uri, trimRange.start.toLong(), trimRange.endInclusive.toLong(),
                                    onComplete = { saved ->
                                        exporting = false
                                        status = if (saved != null) "Video tersimpan di Movies/ViTor." else "Ekspor selesai. Periksa folder Movies/ViTor."
                                    },
                                    onFailure = { error ->
                                        exporting = false
                                        status = "Ekspor gagal: $error"
                                    }
                                )
                            },
                            enabled = !exporting && trimRange.endInclusive - trimRange.start >= 500f,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (exporting) "Merender..." else "Potong & Ekspor MP4") }
                    } else {
                        Text("Pilih klip yang durasinya terbaca untuk mengaktifkan alat potong.", color = Color(0xFF9292A0), fontSize = 11.sp)
                    }
                }
            }

            Text(status, color = Color(0xFFBDB8FF), fontSize = 11.sp)
            Text("ViTor 0.4.0 • Fitur edit aktif: trim dan ekspor MP4", color = Color(0xFF686A78), fontSize = 10.sp)
        }
    }
}

private fun readDuration(context: android.content.Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}

private fun formatTime(milliseconds: Long): String {
    val total = (milliseconds / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(total / 60L, total % 60L)
}