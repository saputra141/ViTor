package com.saputra.vitor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private data class WorkspaceMedia(val uri: Uri, val name: String)
private data class TimelineClip(val id: Int, val media: WorkspaceMedia)

private val editorBg = Color(0xFF111218)
private val panelBg = Color(0xFF1B1D26)
private val panelAlt = Color(0xFF242632)
private val accent = Color(0xFF9B8CFF)
private val muted = Color(0xFF9295A5)

@Composable
fun GebangWorkspace(onOpenCatalog: () -> Unit) {
    val context = LocalContext.current
    val media = remember { mutableStateListOf<WorkspaceMedia>() }
    val timelineClips = remember { mutableStateListOf<TimelineClip>() }
    var nextClipId by remember { mutableIntStateOf(1) }
    var selectedClipId by remember { mutableIntStateOf(-1) }
    var selected by remember { mutableStateOf<WorkspaceMedia?>(null) }
    var activeTool by remember { mutableStateOf("Media") }
    var notice by remember { mutableStateOf("Workspace Android • fondasi antarmuka") }
    var showInspector by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = queryDisplayName(context, uri) ?: "Media ${media.size + 1}"
            val item = WorkspaceMedia(uri, name)
            media.add(item)
            selected = item
            notice = "Media ditambahkan: $name"
        }
    }
    val saveProject = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                val root = JSONObject()
                    .put("format", "vitor-android-project")
                    .put("version", 1)
                    .put("media", JSONArray().apply {
                        media.forEach { item -> put(JSONObject().put("name", item.name).put("uri", item.uri.toString())) }
                    })
                    .put("timeline", JSONArray().apply {
                        timelineClips.forEachIndexed { index, clip ->
                            put(JSONObject().put("id", clip.id).put("mediaUri", clip.media.uri.toString())
                                .put("mediaName", clip.media.name).put("track", "VIDEO 1").put("order", index))
                        }
                    })
                context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
                    ?: error("Tidak bisa membuka file tujuan")
            }.onSuccess { notice = "Metadata proyek tersimpan. Format ini belum kompatibel dengan Gebang desktop." }
                .onFailure { notice = "Gagal menyimpan proyek: ${it.message}" }
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
    DisposableEffect(Unit) { onDispose { player.release() } }

    Surface(Modifier.fillMaxSize(), color = editorBg) {
        Column(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Top application bar
            Row(
                Modifier.fillMaxWidth().height(42.dp).background(panelBg, RoundedCornerShape(9.dp)).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(27.dp).background(accent, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, null, tint = Color(0xFF15131F), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Gebang Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(12.dp))
                Text("Project 01", color = muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                ToolbarAction(Icons.Default.Undo, "Undo") { notice = "Undo belum tersedia pada engine Android." }
                ToolbarAction(Icons.Default.Redo, "Redo") { notice = "Redo belum tersedia pada engine Android." }
                ToolbarAction(Icons.Default.Save, "Simpan") { saveProject.launch("Gebang-Project.json") }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { notice = "Export belum aktif: mesin render native belum tersedia." },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.height(31.dp)
                ) { Text("Export", color = Color(0xFF171421), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = onOpenCatalog, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Collections, "Katalog sumber", tint = muted)
                }
            }

            // Main editing workspace
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Tool rail
                Column(
                    Modifier.width(54.dp).fillMaxHeight().background(panelBg, RoundedCornerShape(9.dp)).padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val tools = listOf(
                        "Media" to Icons.Default.FolderOpen,
                        "Audio" to Icons.Default.MusicNote,
                        "Teks" to Icons.Default.TextFields,
                        "Efek" to Icons.Default.AutoFixHigh,
                        "Transisi" to Icons.Default.CompareArrows,
                        "Layer" to Icons.Default.Layers,
                        "Filter" to Icons.Default.Tune
                    )
                    tools.forEach { (name, icon) ->
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                .background(if (activeTool == name) Color(0xFF39334F) else Color.Transparent, RoundedCornerShape(7.dp))
                                .clickable { activeTool = name },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(onClick = { activeTool = name }, modifier = Modifier.size(38.dp)) {
                                Icon(icon, name, tint = if (activeTool == name) accent else muted, modifier = Modifier.size(19.dp))
                            }
                            Text(name, color = if (activeTool == name) Color.White else muted, fontSize = 8.sp, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { notice = "Pengaturan belum tersedia." }) {
                        Icon(Icons.Default.Settings, "Pengaturan", tint = muted)
                    }
                }

                // Media bin
                Column(
                    Modifier.width(170.dp).fillMaxHeight().background(panelBg, RoundedCornerShape(9.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(activeTool.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { picker.launch(arrayOf("video/*", "audio/*", "image/*")) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, "Import media", tint = accent)
                        }
                    }
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("video/*", "audio/*", "image/*")) },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) { Text("+ Import media", fontSize = 10.sp) }
                    if (activeTool == "Media") {
                        if (media.isEmpty()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("Media bin kosong\nImport file untuk mulai", color = muted, fontSize = 10.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(media) { item ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .background(if (selected == item) Color(0xFF39334F) else panelAlt, RoundedCornerShape(6.dp))
                                            .clickable { selected = item }.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Movie, null, tint = accent, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(item.name, color = Color.White, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Panel $activeTool\nBelum terhubung ke engine.", color = muted, fontSize = 10.sp)
                        }
                    }
                }

                // Preview center and inspector
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                Modifier.fillMaxWidth().height(30.dp).background(panelBg, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PLAYER / PREVIEW", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("Fit  •  16:9", color = muted, fontSize = 9.sp)
                                IconButton(onClick = { notice = "Pengaturan rasio belum tersedia." }, modifier = Modifier.size(25.dp)) {
                                    Icon(Icons.Default.AspectRatio, null, tint = muted, modifier = Modifier.size(15.dp))
                                }
                            }
                            Box(
                                Modifier.weight(1f).fillMaxWidth().background(Color(0xFF08090D), RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected != null) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { PlayerView(it).apply { this.player = player; useController = true } },
                                        update = { it.player = player }
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Movie, null, tint = Color(0xFF71669F), modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Preview", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Pilih media untuk melihat pratinjau", color = muted, fontSize = 10.sp)
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().height(34.dp).background(panelBg, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { player.seekTo(0) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.SkipPrevious, null, tint = muted)
                                }
                                IconButton(onClick = { player.playWhenReady = !player.isPlaying }, modifier = Modifier.size(30.dp)) {
                                    Icon(if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = accent)
                                }
                                Text("00:00:00", color = Color.White, fontSize = 10.sp)
                                Spacer(Modifier.weight(1f))
                                Text("00:00:00", color = muted, fontSize = 10.sp)
                            }
                        }
                        if (showInspector) {
                            Column(
                                Modifier.width(160.dp).fillMaxHeight().background(panelBg, RoundedCornerShape(8.dp)).padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("PROPERTIES", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { showInspector = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, "Tutup panel", tint = muted, modifier = Modifier.size(15.dp))
                                    }
                                }
                                Text("Selection", color = muted, fontSize = 9.sp)
                                Text(selected?.name ?: "No selection", color = Color.White, fontSize = 10.sp, maxLines = 2)
                                HorizontalDivider(color = Color(0xFF353744))
                                Text("Transform", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                PropertyLine("Position X", "—")
                                PropertyLine("Position Y", "—")
                                PropertyLine("Scale", "—")
                                PropertyLine("Rotation", "—")
                                HorizontalDivider(color = Color(0xFF353744))
                                Text("Audio", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                PropertyLine("Volume", "—")
                                Text("Inspector ini masih tampilan. Kontrol transform dan efek belum tersambung.", color = muted, fontSize = 8.sp)
                            }
                        } else {
                            IconButton(onClick = { showInspector = true }) { Icon(Icons.Default.Tune, "Buka properties", tint = accent) }
                        }
                    }

                    // Timeline area
                    Column(
                        Modifier.fillMaxWidth().height(154.dp).background(panelBg, RoundedCornerShape(9.dp)).padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TIMELINE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text("${media.size} media", color = muted, fontSize = 9.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { notice = "Zoom timeline belum tersedia." }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ZoomOut, null, tint = muted, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { notice = "Zoom timeline belum tersedia." }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ZoomIn, null, tint = muted, modifier = Modifier.size(15.dp))
                            }
                        }
                        TimelineTrack("VIDEO 1", selected?.name ?: "Tarik media ke track (drag belum aktif)", Color(0xFF55477F))
                        TimelineTrack("AUDIO 1", "Audio track • belum aktif", Color(0xFF315D62))
                        TimelineTrack("TEXT 1", "Text / captions • belum aktif", Color(0xFF72533B))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(22.dp).background(panelBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(Color(0xFF65C99A), RoundedCornerShape(50)))
                Spacer(Modifier.width(6.dp))
                Text(notice, color = muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Text("Android ARM64 • UI prototype", color = Color(0xFF686B7A), fontSize = 8.sp)
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
    }
}.getOrNull()

@Composable
private fun ToolbarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, label, tint = muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun PropertyLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = muted, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
private fun TimelineTrack(label: String, clipName: String, tint: Color) {
    Row(Modifier.fillMaxWidth().height(29.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = muted, fontSize = 8.sp, modifier = Modifier.width(54.dp))
        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF12141B), RoundedCornerShape(4.dp)).padding(3.dp)) {
            Box(
                Modifier.fillMaxWidth(0.46f).fillMaxHeight()
                    .background(tint.copy(alpha = 0.65f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(clipName, color = Color.White, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
