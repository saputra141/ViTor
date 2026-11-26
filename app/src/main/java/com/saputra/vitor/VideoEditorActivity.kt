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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
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
import com.saputra.vitor.subtitles.SubtitleCue
import com.saputra.vitor.subtitles.SrtCodec
import com.saputra.vitor.timeline.AudioLayer
import com.saputra.vitor.timeline.ProjectFileStore
import com.saputra.vitor.timeline.TextLayer
import com.saputra.vitor.timeline.TimelineProject
import com.saputra.vitor.timeline.VideoClip
import java.io.File
import java.util.UUID

private data class EditClip(
    val id: String,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs
)

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
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var titleText by remember { mutableStateOf("") }
    var musicVolume by remember { mutableStateOf(0.75f) }
    var selectedLook by remember { mutableStateOf(VideoLook.ORIGINAL) }
    var projectName by remember { mutableStateOf("Untitled project") }
    var projectId by remember { mutableStateOf<String?>(null) }
    var savedProjects by remember { mutableStateOf(emptyList<ProjectFileStore.ProjectSummary>()) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var musicDurationMs by remember { mutableStateOf(0L) }
    val projectStore = remember(context) { ProjectFileStore(File(context.filesDir, "ViTorProjects")) }
    val subtitleCues = remember { mutableStateListOf<SubtitleCue>() }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("File subtitle tidak bisa dibaca.")
            }.onSuccess { source ->
                runCatching { SrtCodec.parse(source) }
                    .onSuccess { cues ->
                        subtitleCues.clear()
                        subtitleCues.addAll(cues)
                        status = "${cues.size} subtitle berhasil diimpor."
                    }
                    .onFailure { status = "SRT tidak valid: ${it.message}" }
            }.onFailure { status = "Gagal membaca subtitle: ${it.message}" }
        }
    }

    val subtitleSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(SrtCodec.write(subtitleCues.toList()))
                } ?: throw IllegalStateException("File tujuan tidak bisa dibuka.")
            }.onSuccess { status = "Subtitle berhasil disimpan sebagai SRT." }
                .onFailure { status = "Gagal menyimpan SRT: ${it.message}" }
        }
    }

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
            val clip = EditClip(UUID.randomUUID().toString(), uri, name, duration, 0L, duration.coerceAtLeast(1L))
            clips.add(clip)
            selected = clip
            trimRange = 0f..duration.coerceAtLeast(1L).toFloat()
            status = if (duration > 0) "Klip siap diedit." else "Durasi video tidak dapat dibaca."
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            musicUri = uri
            musicDurationMs = readDuration(context, uri).coerceAtLeast(1L)
            status = "Musik latar ditambahkan ke proyek."
        }
    }

    val player = remember { ExoPlayer.Builder(context).build() }
    LaunchedEffect(selected?.id) {
        selected?.let {
            player.setMediaItem(MediaItem.fromUri(it.uri))
            player.prepare()
            player.playWhenReady = false
            trimRange = it.trimStartMs.toFloat()..it.trimEndMs.coerceAtLeast(1L).toFloat()
        } ?: player.clearMediaItems()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF101116)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ViTor Studio", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(projectName, color = Color(0xFFAAAAB8), fontSize = 12.sp)
                    }
                    Button(onClick = { picker.launch(arrayOf("video/*")) }, enabled = !exporting) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        runCatching {
                            val duration = clips.sumOf { it.durationMs.coerceAtLeast(1L) }
                            val videoTracks = clips.map { clip ->
                                VideoClip(
                                    id = clip.id,
                                    sourceUri = clip.uri.toString(),
                                    sourceDurationMs = clip.durationMs.coerceAtLeast(1L),
                                    trimStartMs = clip.trimStartMs.coerceIn(0L, (clip.durationMs - 1L).coerceAtLeast(0L)),
                                    trimEndMs = clip.trimEndMs.coerceIn(clip.trimStartMs + 1L, clip.durationMs.coerceAtLeast(clip.trimStartMs + 1L))
                                )
                            }
                            val audioTracks = musicUri?.let { uri ->
                                listOf(AudioLayer(
                                    id = "background-music",
                                    sourceUri = uri.toString(),
                                    sourceEndMs = musicDurationMs.coerceAtLeast(1L),
                                    volume = musicVolume,
                                    loop = true
                                ))
                            }.orEmpty()
                            val textTracks = if (titleText.isNotBlank()) {
                                listOf(TextLayer("main-title", titleText, 0L, duration.coerceAtLeast(2_000L)))
                            } else emptyList()
                            val project = TimelineProject(
                                name = projectName,
                                videoClips = videoTracks,
                                audioLayers = audioTracks,
                                textLayers = textTracks,
                                subtitleCues = subtitleCues.toList(),
                                videoLook = selectedLook.name
                            )
                            val id = projectId ?: UUID.randomUUID().toString()
                            val saved = projectStore.save(project, id)
                            projectId = saved.projectId
                            status = "Proyek berhasil disimpan."
                        }.onFailure { status = "Gagal menyimpan proyek: ${it.message}" }
                    }, enabled = !exporting) { Text("Simpan Proyek") }
                    OutlinedButton(onClick = {
                        savedProjects = projectStore.list()
                        showProjectDialog = true
                    }, enabled = !exporting) { Text("Buka Proyek") }
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
                        val clipIndex = clips.indexOf(clip)
                        Card(
                            onClick = { selected = clip },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected == clip) Color(0xFF383253) else Color(0xFF222330)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.width(250.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, null, tint = Color(0xFFAAA0FF))
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(clip.name, color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(formatTime(clip.durationMs), color = Color(0xFFAAAAB8), fontSize = 10.sp)
                                }
                                IconButton(
                                    onClick = {
                                        if (clipIndex > 0) {
                                            val previous = clips[clipIndex - 1]
                                            clips[clipIndex - 1] = clip
                                            clips[clipIndex] = previous
                                        }
                                    },
                                    enabled = clipIndex > 0
                                ) {
                                    Icon(Icons.Default.ArrowBack, "Geser klip ke kiri", tint = Color(0xFFBDB8FF))
                                }
                                IconButton(
                                    onClick = {
                                        if (clipIndex < clips.lastIndex) {
                                            val next = clips[clipIndex + 1]
                                            clips[clipIndex + 1] = clip
                                            clips[clipIndex] = next
                                        }
                                    },
                                    enabled = clipIndex < clips.lastIndex
                                ) {
                                    Icon(Icons.Default.ArrowForward, "Geser klip ke kanan", tint = Color(0xFFBDB8FF))
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
                Text("Urutan dari kiri ke kanan adalah urutan hasil video. Gunakan panah untuk mengatur posisi klip.", color = Color(0xFF9292A0), fontSize = 10.sp)
                Button(
                    onClick = {
                        exporting = true
                        status = "Merender video, audio, teks, dan efek..."
                        exportEditedProject(
                            context = context,
                            clipUris = clips.map { it.uri },
                            musicUri = musicUri,
                            musicVolume = musicVolume,
                            titleText = titleText,
                            look = selectedLook,
                            clipRanges = clips.map { it.trimStartMs to it.trimEndMs },
                            subtitleCues = subtitleCues.toList(),
                            onComplete = {
                                exporting = false
                                status = "Render selesai. File MP4 disimpan ke Movies/ViTor."
                            },
                            onFailure = { error ->
                                exporting = false
                                status = "Render gagal: $error"
                            }
                        )
                    },
                    enabled = clips.isNotEmpty() && !exporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (exporting) "Sedang merender..." else "Render Proyek MP4")
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LAYER AUDIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        if (musicUri == null) "Belum ada musik latar" else "Musik latar siap dirender",
                        color = Color(0xFFBDB8FF), fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, enabled = !exporting) {
                            Text(if (musicUri == null) "Tambah Musik" else "Ganti Musik")
                        }
                        if (musicUri != null) {
                            OutlinedButton(onClick = { musicUri = null; musicDurationMs = 0L }, enabled = !exporting) {
                                Text("Hapus Musik")
                            }
                        }
                    }
                    Text("Volume musik: ${(musicVolume * 100).toInt()}%", color = Color(0xFFAAAAB8), fontSize = 11.sp)
                    Slider(value = musicVolume, onValueChange = { musicVolume = it }, valueRange = 0f..1f, enabled = musicUri != null && !exporting)
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TRACK SUBTITLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${subtitleCues.size} baris subtitle", color = Color(0xFFBDB8FF), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { subtitlePicker.launch(arrayOf("application/x-subrip", "text/plain", "application/octet-stream")) }, enabled = !exporting) {
                            Text("Import SRT")
                        }
                        OutlinedButton(onClick = { subtitleSaver.launch("ViTor_Subtitles.srt") }, enabled = !exporting && subtitleCues.isNotEmpty()) {
                            Text("Ekspor SRT")
                        }
                        Button(onClick = {
                            val nextStart = subtitleCues.lastOrNull()?.endMs ?: 0L
                            subtitleCues.add(
                                SubtitleCue(
                                    id = "vitor-${System.currentTimeMillis()}",
                                    startMs = nextStart,
                                    endMs = nextStart + 2_000L,
                                    text = "Subtitle baru"
                                )
                            )
                        }, enabled = !exporting) { Text("Tambah") }
                    }
                    if (subtitleCues.isEmpty()) {
                        Text("Impor file .srt atau tambah baris subtitle baru.", color = Color(0xFF9292A0), fontSize = 11.sp)
                    } else {
                        subtitleCues.toList().forEachIndexed { index, cue ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${formatTime(cue.startMs)} – ${formatTime(cue.endMs)}", color = Color(0xFFBDB8FF), fontSize = 10.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { subtitleCues.remove(cue) }) {
                                        Icon(Icons.Default.Delete, "Hapus subtitle", tint = Color(0xFFFF8A8A))
                                    }
                                }
                                TextField(
                                    value = cue.text,
                                    onValueChange = { updated ->
                                        if (updated.isNotBlank()) subtitleCues[index] = cue.copy(text = updated)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Teks subtitle ${index + 1}") },
                                    enabled = !exporting
                                )
                            }
                        }
                    }
                    Text("Subtitle dengan rentang waktu akan ditampilkan pada bagian video yang sesuai saat render MP4.", color = Color(0xFF9292A0), fontSize = 10.sp)
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23))
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TEKS & FILTER VIDEO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextField(
                        value = titleText,
                        onValueChange = { titleText = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Teks overlay pada video") },
                        placeholder = { Text("Tulis judul atau caption...") },
                        singleLine = true,
                        enabled = !exporting
                    )
                    Text("Filter", color = Color(0xFFAAAAB8), fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { selectedLook = VideoLook.ORIGINAL }, enabled = !exporting) {
                            Text(if (selectedLook == VideoLook.ORIGINAL) "✓ Asli" else "Asli")
                        }
                        OutlinedButton(onClick = { selectedLook = VideoLook.GRAYSCALE }, enabled = !exporting) {
                            Text(if (selectedLook == VideoLook.GRAYSCALE) "✓ Hitam putih" else "Hitam putih")
                        }
                        OutlinedButton(onClick = { selectedLook = VideoLook.HIGH_CONTRAST }, enabled = !exporting) {
                            Text(if (selectedLook == VideoLook.HIGH_CONTRAST) "✓ Kontras" else "Kontras")
                        }
                    }
                    Text("Teks dan filter diterapkan saat render ke seluruh klip proyek.", color = Color(0xFF9292A0), fontSize = 10.sp)
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
                            onValueChange = { range ->
                                trimRange = range
                                selected?.let { current ->
                                    val index = clips.indexOfFirst { it.id == current.id }
                                    if (index >= 0) {
                                        val updated = current.copy(
                                            trimStartMs = range.start.toLong().coerceAtLeast(0L),
                                            trimEndMs = range.endInclusive.toLong().coerceAtLeast(range.start.toLong() + 1L)
                                        )
                                        clips[index] = updated
                                        selected = updated
                                    }
                                }
                            },
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
                                    onComplete = {
                                        exporting = false
                                        status = "Video hasil potong selesai. Periksa folder Movies/ViTor."
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
            Text("ViTor 0.6.0 • Video, musik, teks, filter, trim, dan render", color = Color(0xFF686A78), fontSize = 10.sp)
        }
        if (showProjectDialog) {
            AlertDialog(
                onDismissRequest = { showProjectDialog = false },
                title = { Text("Buka proyek") },
                text = {
                    if (savedProjects.isEmpty()) {
                        Text("Belum ada proyek tersimpan.")
                    } else {
                        Column(
                            Modifier.height(260.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedProjects.forEach { summary ->
                                TextButton(
                                    onClick = {
                                        runCatching {
                                            val project = projectStore.load(summary.projectId)
                                            clips.clear()
                                            project.videoClips.forEach { saved ->
                                                val uri = Uri.parse(saved.sourceUri)
                                                val name = queryDisplayName(context, uri) ?: "Video ${clips.size + 1}"
                                                clips.add(EditClip(
                                                    saved.id, uri, name, saved.sourceDurationMs,
                                                    saved.trimStartMs, saved.trimEndMs
                                                ))
                                            }
                                            selected = clips.firstOrNull()
                                            selected?.let { clip ->
                                                val saved = project.videoClips.first { it.id == clip.id }
                                                trimRange = saved.trimStartMs.toFloat()..saved.trimEndMs.toFloat()
                                            }
                                            val audio = project.audioLayers.firstOrNull()
                                            musicUri = audio?.let { Uri.parse(it.sourceUri) }
                                            musicDurationMs = audio?.durationMs ?: 0L
                                            musicVolume = audio?.volume ?: 0.75f
                                            titleText = project.textLayers.firstOrNull()?.text.orEmpty()
                                            selectedLook = runCatching { VideoLook.valueOf(project.videoLook) }.getOrDefault(VideoLook.ORIGINAL)
                                            subtitleCues.clear()
                                            subtitleCues.addAll(project.subtitleCues)
                                            projectName = project.name
                                            projectId = summary.projectId
                                            status = "Proyek berhasil dibuka."
                                            showProjectDialog = false
                                        }.onFailure { status = "Gagal membuka proyek: ${it.message}" }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(summary.name, color = Color.White)
                                        Text("ID: ${summary.projectId}", color = Color(0xFF9292A0), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProjectDialog = false }) { Text("Tutup") }
                }
            )
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()

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
