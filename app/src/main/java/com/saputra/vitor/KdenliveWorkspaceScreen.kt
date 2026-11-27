package com.saputra.vitor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KdBg = Color(0xFF17191D)
private val KdPanel = Color(0xFF22252B)
private val KdPanel2 = Color(0xFF2B2F36)
private val KdLine = Color(0xFF3A3F48)
private val KdText = Color(0xFFE5E7EB)
private val KdMuted = Color(0xFF9AA1AC)
private val KdAccent = Color(0xFF3DAEE9)

private data class KdTimelineClip(
    val id: Long,
    val uri: Uri,
    val label: String,
    val sourceInMs: Long = 0L,
    val sourceOutMs: Long = Long.MAX_VALUE,
)

/**
 * Kdenlive-inspired Android workspace shell.
 * This is the first UI port layer; editing actions are connected to the native
 * ViTor engine incrementally and are not represented as complete Kdenlive parity.
 */
@Composable
fun KdenliveWorkspaceScreen(
    projectId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    mediaUris: List<Uri> = emptyList(),
    onImportMedia: () -> Unit = {},
    onRemoveMedia: (Uri) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf("Project Bin") }
    var selectedTool by remember { mutableStateOf("Select") }
    var playing by remember { mutableStateOf(false) }
    var selectedClip by remember { mutableStateOf("Clip 01") }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var timelineClips by remember { mutableStateOf<List<KdTimelineClip>>(emptyList()) }
    var selectedTimelineClipId by remember { mutableStateOf<Long?>(null) }
    var playheadMs by remember { mutableLongStateOf(0L) }
    var clipIdCounter by remember { mutableLongStateOf(1L) }
    val context = LocalContext.current
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val selectedMediaType = remember(selectedMedia) { selectedMedia?.let { context.contentResolver.getType(it) }.orEmpty() }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(mediaUris) {
        val existing = timelineClips.map { it.uri }.toSet()
        val added = mediaUris.filterNot { it in existing }.map { uri ->
            KdTimelineClip(clipIdCounter++, uri, uri.lastPathSegment?.substringAfterLast('/')?.takeLast(20) ?: "Media")
        }
        if (added.isNotEmpty()) timelineClips = timelineClips + added
        if (selectedMedia !in mediaUris) selectedMedia = mediaUris.firstOrNull()
    }
    LaunchedEffect(player, playing) {
        while (playing) {
            delay(120)
            playheadMs = player.currentPosition.coerceAtLeast(0L)
            if (!player.isPlaying) playing = false
        }
    }
    LaunchedEffect(selectedMedia) {
        player.stop()
        playheadMs = 0L
        if (!selectedMediaType.startsWith("image/")) selectedMedia?.let { player.setMediaItem(MediaItem.fromUri(it)); player.prepare() }
    }

    Column(Modifier.fillMaxSize().background(KdBg)) {
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF202329)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ViTor  /  Kdenlive Workspace", color = KdText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(projectId.take(18), color = KdMuted, fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            Text("−   □   ×", color = KdMuted, fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().height(38.dp).background(KdPanel).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("File", "Edit", "View", "Project", "Clip", "Timeline", "Monitor", "Settings", "Help").forEach {
                Text(it, color = KdText, fontSize = 11.sp, modifier = Modifier.clickable { selectedTab = it }.padding(horizontal = 8.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Projects", fontSize = 11.sp) }
            TextButton(onClick = onImportMedia, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Import Media", fontSize = 11.sp) }
            Button(onClick = onExport, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = KdAccent)) {
                Text("Render", fontSize = 11.sp, color = Color.Black)
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(0.26f).fillMaxHeight().background(KdPanel, RoundedCornerShape(5.dp))) {
                Row(Modifier.fillMaxWidth().height(34.dp).background(KdPanel2).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedTab, color = KdText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Add, null, tint = KdMuted, modifier = Modifier.size(17.dp))
                }
                Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Project Bin", "Effects", "History").forEach { tab ->
                        Text(tab, color = if (selectedTab == tab) KdAccent else KdMuted, fontSize = 9.sp,
                            modifier = Modifier.clickable { selectedTab = tab }.padding(4.dp))
                    }
                }
                if (selectedTab == "Effects") {
                    listOf("Audio", "Blur and Sharpen", "Color Correction", "Crop and Transform", "Motion", "Stylize").forEach { PanelEntry(it) }
                } else {
                    PanelEntry("All clips (${mediaUris.size})")
                    mediaUris.forEachIndexed { index, uri ->
                        val label = uri.lastPathSegment?.substringAfterLast('/')?.takeLast(28) ?: "Media ${index + 1}"
                        Row(Modifier.fillMaxWidth().clickable { selectedMedia = uri; selectedClip = label }.padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, tint = if (selectedMedia == uri) KdAccent else KdMuted, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(label, color = if (selectedMedia == uri) KdAccent else KdText, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Close, contentDescription = "Remove media", tint = KdMuted, modifier = Modifier.size(15.dp).clickable { onRemoveMedia(uri) })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(if (mediaUris.isEmpty()) "Import video, audio or image files" else "Select media to preview", color = KdMuted, fontSize = 9.sp, modifier = Modifier.padding(10.dp))
                }
            }
            Column(Modifier.weight(0.48f).fillMaxHeight().background(Color(0xFF101216), RoundedCornerShape(5.dp))) {
                Row(Modifier.fillMaxWidth().height(34.dp).background(KdPanel).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Project Monitor", color = KdText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("1920 × 1080  •  30 fps", color = KdMuted, fontSize = 9.sp)
                }
                Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp).background(Color(0xFF050608)), contentAlignment = Alignment.Center) {
                    if (selectedMediaType.startsWith("image/")) {
                        AsyncImage(model = selectedMedia, contentDescription = "Selected image", modifier = Modifier.fillMaxSize())
                    } else if (selectedMedia != null) {
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                            update = { it.player = player },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, null, tint = Color(0xFF414854), modifier = Modifier.size(42.dp))
                            Text("Project Monitor", color = Color(0xFF777F8C), fontSize = 12.sp)
                            Text("Import media to start preview", color = Color(0xFF555D68), fontSize = 9.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(42.dp).background(KdPanel).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play(); playing = player.isPlaying }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = KdText) }
                    IconButton(onClick = { player.seekTo(0); playheadMs = 0L }) { Icon(Icons.Default.SkipPrevious, null, tint = KdMuted) }
                    IconButton(onClick = { player.seekTo((player.duration.takeIf { it > 0 } ?: 0L)); playheadMs = player.currentPosition }) { Icon(Icons.Default.SkipNext, null, tint = KdMuted) }
                    Spacer(Modifier.weight(1f))
                    Text(formatTimecode(playheadMs), color = KdText, fontSize = 11.sp)
                }
            }
            Column(Modifier.weight(0.26f).fillMaxHeight().background(KdPanel, RoundedCornerShape(5.dp))) {
                Text("Properties", color = KdText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(KdPanel2).padding(9.dp))
                Text(selectedClip, color = KdAccent, fontSize = 11.sp, modifier = Modifier.padding(9.dp))
                listOf("Position", "Scale", "Rotation", "Opacity", "Volume").forEach { label ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = KdMuted, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Text(if (label == "Opacity" || label == "Volume") "100%" else "0.0", color = KdText, fontSize = 10.sp)
                    }
                    HorizontalDivider(color = KdLine, thickness = 0.5.dp)
                }
            }
        }
        Column(Modifier.fillMaxWidth().height(208.dp).padding(horizontal = 6.dp).background(KdPanel, RoundedCornerShape(5.dp))) {
            Row(Modifier.fillMaxWidth().height(36.dp).background(KdPanel2).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Timeline", color = KdText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                listOf("Select", "Razor", "Spacer").forEach { tool ->
                    Text(tool, color = if (selectedTool == tool) KdAccent else KdMuted, fontSize = 10.sp,
                        modifier = Modifier.clickable { selectedTool = tool }.padding(horizontal = 7.dp, vertical = 5.dp))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    val selected = timelineClips.firstOrNull { it.id == selectedTimelineClipId }
                    val splitAt = playheadMs
                    if (selected != null && splitAt > selected.sourceInMs && splitAt < selected.sourceOutMs) {
                        val left = selected.copy(sourceOutMs = splitAt)
                        val right = selected.copy(id = clipIdCounter++, sourceInMs = splitAt, label = selected.label + " (2)")
                        val index = timelineClips.indexOfFirst { it.id == selected.id }
                        timelineClips = timelineClips.toMutableList().also { it.removeAt(index); it.addAll(index, listOf(left, right)) }
                        selectedTimelineClipId = right.id
                    }
                }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Split", color = KdAccent, fontSize = 10.sp) }
                TextButton(onClick = {
                    timelineClips = timelineClips.filterNot { it.id == selectedTimelineClipId }
                    selectedTimelineClipId = null
                }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) { Text("Delete", color = Color(0xFFE57373), fontSize = 10.sp) }
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.width(76.dp).fillMaxHeight().background(Color(0xFF252930))) {
                    listOf("V2", "V1", "A1", "A2").forEach { track ->
                        Box(Modifier.fillMaxWidth().weight(1f).border(0.5.dp, KdLine).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                            Text(track, color = KdMuted, fontSize = 10.sp)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(5.dp)) {
                    repeat(4) { index ->
                        Box(Modifier.width(760.dp).weight(1f).border(0.5.dp, KdLine)) {
                            if (index == 1 && timelineClips.isNotEmpty()) {
                                Row(Modifier.fillMaxHeight().padding(start = 18.dp, top = 3.dp, bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    timelineClips.forEach { clip ->
                                        val selected = selectedTimelineClipId == clip.id
                                        TimelineClip(clip.label, Modifier.width(160.dp), selected) {
                                            selectedTimelineClipId = clip.id
                                            selectedMedia = clip.uri
                                            selectedClip = clip.label
                                            player.seekTo(clip.sourceInMs)
                                            playheadMs = clip.sourceInMs
                                        }
                                    }
                                }
                            } else if (index == 1) {
                                Text("Import media, then select a clip", color = KdMuted, fontSize = 9.sp, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(22.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ready • ${timelineClips.size} timeline clips", color = KdMuted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("ViTor Android • Kdenlive-inspired workspace", color = KdMuted, fontSize = 9.sp)
        }
    }
}

private fun formatTimecode(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val frames = (milliseconds.coerceAtLeast(0L) % 1000L) * 30L / 1000L
    return "%02d:%02d:%02d:%02d".format(hours, minutes, seconds, frames)
}

@Composable
private fun PanelEntry(label: String) {
    Row(Modifier.fillMaxWidth().clickable { }.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.InsertDriveFile, null, tint = KdMuted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = KdText, fontSize = 10.sp)
    }
}

@Composable
private fun TimelineClip(label: String, modifier: Modifier, selected: Boolean, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().background(if (selected) Color(0xFF286D91) else Color(0xFF455A70), RoundedCornerShape(3.dp))
        .border(1.dp, if (selected) KdAccent else Color(0xFF62788E), RoundedCornerShape(3.dp))
        .clickable(onClick = onClick).padding(6.dp), contentAlignment = Alignment.CenterStart) {
        Text(label, color = Color.White, fontSize = 9.sp, maxLines = 1)
    }
}
