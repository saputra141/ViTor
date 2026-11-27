package com.saputra.vitor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import com.zaitxcode.opencut.core.editor.newProject
import com.zaitxcode.opencut.core.model.Project
import com.zaitxcode.opencut.ui.editor.DockBar
import com.zaitxcode.opencut.ui.editor.EditorViewModel
import com.zaitxcode.opencut.ui.editor.PreviewPane
import com.zaitxcode.opencut.ui.editor.TimelineView
import com.zaitxcode.opencut.ui.editor.panel.PanelHost
import com.zaitxcode.opencut.ui.editor.panel.Tool

private val KdBg = Color(0xFF17191D)
private val KdPanel = Color(0xFF22252B)
private val KdPanel2 = Color(0xFF2B2F36)
private val KdText = Color(0xFFE5E7EB)
private val KdMuted = Color(0xFF9AA1AC)
private val KdAccent = Color(0xFF3DAEE9)

@Composable
fun KdenliveWorkspaceScreen(
    projectId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    mediaUris: List<Uri> = emptyList(),
    onImportMedia: () -> Unit = {},
    onRemoveMedia: (Uri) -> Unit = {},
    editorViewModel: EditorViewModel = viewModel(),
) {
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf<Tool?>(Tool.MEDIA) }
    var selectedTab by remember { mutableStateOf("Project Bin") }
    var loadedProjectId by remember { mutableStateOf<String?>(null) }
    var importedUris by remember(projectId) { mutableStateOf<Set<Uri>>(emptySet()) }

    val project by editorViewModel.project.collectAsStateWithLifecycle()
    val playing by editorViewModel.playing.collectAsStateWithLifecycle()
    val playheadUs by editorViewModel.playheadUs.collectAsStateWithLifecycle()
    val selectedSegmentId by editorViewModel.selectedSegmentId.collectAsStateWithLifecycle()
    val canUndo by editorViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by editorViewModel.canRedo.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, mediaUris) {
        if (loadedProjectId != projectId) {
            val existing = (context.applicationContext as ViTorApplication).projectStore.readProject(projectId)
            editorViewModel.open(existing ?: newProject("ViTor Project").copy(id = projectId))
            loadedProjectId = projectId
        }
        val fresh = mediaUris.filterNot { it in importedUris }
        if (fresh.isNotEmpty()) {
            editorViewModel.importMedia(fresh)
            importedUris = importedUris + fresh
        }
    }

    val selectedSegment = editorViewModel.selectedSegment
    val materials = project.materials.all

    Column(Modifier.fillMaxSize().background(KdBg)) {
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF202329)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ViTor  /  Kdenlive Workspace", color = KdText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(project.name.take(24), color = KdMuted, fontSize = 11.sp)
            TextButton(onClick = { editorViewModel.undo() }, enabled = canUndo) { Text("Undo", fontSize = 10.sp) }
            TextButton(onClick = { editorViewModel.redo() }, enabled = canRedo) { Text("Redo", fontSize = 10.sp) }
            TextButton(onClick = { editorViewModel.save(onBack) }) { Text("Projects", fontSize = 10.sp) }
            Button(
                onClick = { editorViewModel.save(onExport) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KdAccent),
            ) { Text("Render", fontSize = 11.sp, color = Color.Black) }
        }
        Row(
            Modifier.fillMaxWidth().height(34.dp).background(KdPanel).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("File", "Edit", "View", "Project", "Clip", "Timeline", "Monitor", "Settings", "Help").forEach {
                Text(it, color = KdText, fontSize = 10.sp, modifier = Modifier.clickable { selectedTab = it }.padding(horizontal = 7.dp, vertical = 7.dp))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onImportMedia, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Import Media", fontSize = 10.sp) }
        }
        Row(Modifier.fillMaxWidth().weight(1f).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(0.23f).fillMaxHeight().background(KdPanel)) {
                Row(Modifier.fillMaxWidth().height(32.dp).background(KdPanel2).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedTab, color = KdText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(materials.size.toString(), color = KdMuted, fontSize = 10.sp)
                }
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("Project Bin", "Effects").forEach { tab ->
                        Text(tab, color = if (selectedTab == tab) KdAccent else KdMuted, fontSize = 9.sp, modifier = Modifier.clickable { selectedTab = tab }.padding(4.dp))
                    }
                }
                if (selectedTab == "Effects") {
                    listOf("Color filters", "Transitions", "Speed", "Crop", "Adjust", "Audio").forEach { name ->
                        Text(name, color = KdText, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().clickable {
                            selectedTool = when(name) {
                                "Color filters" -> Tool.FILTER
                                "Transitions" -> Tool.TRANSITION
                                "Speed" -> Tool.SPEED
                                "Crop" -> Tool.CROP
                                "Adjust" -> Tool.ADJUST
                                else -> Tool.AUDIO
                            }
                        }.padding(horizontal = 9.dp, vertical = 8.dp))
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        materials.forEach { material ->
                            val uri = editorViewModel.uris.value[material.id] ?: material.path.takeIf { it.isNotBlank() }?.let(Uri::parse)
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    val segment = project.tracks.asSequence().flatMap { it.segments.asSequence() }.firstOrNull { it.materialId == material.id }
                                    if (segment != null) editorViewModel.select(segment.id)
                                    uri?.let { editorViewModel.seek(0L) }
                                }.padding(horizontal = 7.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Movie, null, tint = KdAccent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(material.name, color = KdText, fontSize = 9.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                        }
                        if (materials.isEmpty()) Text("Import video, audio or images", color = KdMuted, fontSize = 9.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            Column(Modifier.weight(0.52f).fillMaxHeight().background(Color(0xFF101216))) {
                Row(Modifier.fillMaxWidth().height(32.dp).background(KdPanel).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Project Monitor", color = KdText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${project.canvas.width} × ${project.canvas.height}  •  ${project.frameRate.toInt()} fps", color = KdMuted, fontSize = 9.sp)
                }
                PreviewPane(
                    project = project,
                    playheadUs = playheadUs,
                    playing = playing,
                    onTogglePlay = { editorViewModel.setPlaying(!playing) },
                    onSeek = editorViewModel::seek,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(10.dp),
                )
                DockBar(activeTool = selectedTool, onSelect = { selectedTool = if (selectedTool == it) null else it })
            }
            Column(Modifier.weight(0.25f).fillMaxHeight().background(KdPanel)) {
                Text("Properties / Effects", color = KdText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().background(KdPanel2).padding(9.dp))
                Text(selectedSegment?.let { project.materials.find(it.materialId)?.name } ?: "No clip selected", color = KdAccent, fontSize = 10.sp, modifier = Modifier.padding(9.dp))
                if (selectedTool != null) {
                    PanelHost(
                        tool = selectedTool!!,
                        project = project,
                        selectedSegment = selectedSegment,
                        viewModel = editorViewModel,
                    )
                } else {
                    Text("Select a tool to edit", color = KdMuted, fontSize = 10.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 6.dp).background(KdPanel)) {
            Row(Modifier.fillMaxWidth().height(30.dp).background(KdPanel2).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Timeline", color = KdText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Tracks: ${project.tracks.size}", color = KdMuted, fontSize = 9.sp)
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                TimelineView(
                    project = project,
                    playheadUs = playheadUs,
                    selectedSegmentId = selectedSegmentId,
                    onSelect = { segmentId -> editorViewModel.select(segmentId) },
                    onSeek = editorViewModel::seek,
                    onSplit = editorViewModel::split,
                    onDelete = editorViewModel::delete,
                    onDuplicate = editorViewModel::duplicate,
                    onMoveSegment = editorViewModel::moveSegment,
                    onTrimSegment = editorViewModel::trimSegment,
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ready • Autosave enabled", color = KdMuted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("ViTor Android • Kdenlive-inspired workspace", color = KdMuted, fontSize = 9.sp)
        }
    }
}
