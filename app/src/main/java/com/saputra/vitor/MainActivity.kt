package com.saputra.vitor

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private data class VideoClip(val uri: Uri, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF9A8CFF),
                secondary = Color(0xFF56D6C2),
                background = Color(0xFF101116),
                surface = Color(0xFF1A1B23)
            )) {
                ViTorEditor()
            }
        }
    }
}

@Composable
private fun ViTorEditor() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clips = remember { mutableStateListOf<VideoClip>() }
    var selected by remember { mutableStateOf<VideoClip?>(null) }
    var status by remember { mutableStateOf("Pilih video untuk memulai proyek.") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
                }
            }.getOrNull() ?: "Video " + (clips.size + 1)
            val clip = VideoClip(uri, name)
            clips.add(clip)
            selected = clip
            status = "Video berhasil ditambahkan ke proyek."
        }
    }

    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(selected?.uri) {
        selected?.let {
            player.setMediaItem(MediaItem.fromUri(it.uri))
            player.prepare()
            player.playWhenReady = false
        } ?: player.clearMediaItems()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101116)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ViTor", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Text("Video Studio • Project 01", color = Color(0xFFAAAAB8), fontSize = 12.sp)
                }
                Button(onClick = { picker.launch(arrayOf("video/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050608))
            ) {
                if (selected != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                useController = true
                            }
                        },
                        update = { it.player = player }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF8D83E8), modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Preview video", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Import video untuk melihat preview", color = Color(0xFF9292A0), fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TIMELINE", color = Color(0xFFAAAAB8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(clips.size.toString() + " klip", color = Color(0xFFAAAAB8), fontSize = 11.sp)
            }

            if (clips.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(112.dp)
                        .background(Color(0xFF1A1B23), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada klip. Tekan Import untuk menambahkan video.", color = Color(0xFF9292A0), fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clips.forEach { clip ->
                        Card(
                            onClick = { selected = clip },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected == clip) Color(0xFF383253) else Color(0xFF222330)
                            )
                        ) {
                            Row(
                                modifier = Modifier.width(190.dp).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFAAA0FF))
                                Spacer(Modifier.width(8.dp))
                                Text(clip.name, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    clips.remove(clip)
                                    if (selected == clip) selected = clips.lastOrNull()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus klip", tint = Color(0xFFFF8A8A))
                                }
                            }
                        }
                    }
                }
            }

            Text("LAYER", color = Color(0xFFAAAAB8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerChip(Icons.Default.Movie, "Video")
                LayerChip(Icons.Default.AudioFile, "Audio")
                LayerChip(Icons.Default.TextFields, "Teks")
            }

            Spacer(Modifier.weight(1f))
            Text(status, color = Color(0xFF8F90A0), fontSize = 11.sp)
            Text("ViTor 0.2.0 • Native Android ARM64", color = Color(0xFF686A78), fontSize = 10.sp)
        }
    }
}

@Composable
private fun LayerChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    OutlinedButton(onClick = { }) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp)
    }
}
