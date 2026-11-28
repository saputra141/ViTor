package com.zaitxcode.opencut.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaitxcode.opencut.core.editor.newProject
import com.zaitxcode.opencut.ui.editor.panel.PanelHost
import com.zaitxcode.opencut.ui.editor.panel.Tool

private val NovaBackground = Color(0xFF090A0E)
private val NovaSurface = Color(0xFF15171D)
private val NovaMuted = Color(0xFF858A98)
private val NovaAccent = Color(0xFF39D7D0)

@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val context = LocalContext.current
    var activeTool by remember { mutableStateOf<Tool?>(null) }
    val project by viewModel.project.collectAsStateWithLifecycle()
    val playheadUs by viewModel.playheadUs.collectAsStateWithLifecycle()
    val playing by viewModel.playing.collectAsStateWithLifecycle()
    val selectedSegmentId by viewModel.selectedSegmentId.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        if (!viewModel.isOpen) {
            val existing = (context.applicationContext as com.saputra.vitor.ViTorApplication)
                .projectStore.readProject(projectId)
            viewModel.open(existing ?: newProject("NovaCut Project").copy(id = projectId))
        }
    }

    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.save(onBack) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(project.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(12.dp))
                    Text(" Saved locally", color = NovaMuted, fontSize = 10.sp)
                }
            }
            IconButton(onClick = viewModel::undo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.DarkGray)
            }
            IconButton(onClick = viewModel::redo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.DarkGray)
            }
            Button(
                onClick = { viewModel.save(onExport) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaAccent, contentColor = Color(0xFF061314)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(5.dp))
                Text("Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color(0xFF050608)),
            contentAlignment = Alignment.Center,
        ) {
            PreviewPane(
                project = project,
                playheadUs = playheadUs,
                playing = playing,
                onTogglePlay = { viewModel.setPlaying(!playing) },
                onSeek = viewModel::seek,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            Modifier.fillMaxWidth().background(NovaSurface).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatNovaTimecode(playheadUs), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Surface(color = Color(0xFF242832), shape = CircleShape) {
                Text(" " + project.canvas.width + " × " + project.canvas.height + " ", color = NovaMuted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Text(formatNovaTimecode(project.durationUs), color = NovaMuted, fontSize = 12.sp)
        }

        Surface(color = Color(0xFF101217)) {
            TimelineView(
                project = project,
                playheadUs = playheadUs,
                selectedSegmentId = selectedSegmentId,
                onSelect = { segmentId -> viewModel.select(segmentId) },
                onSeek = viewModel::seek,
                onSplit = viewModel::split,
                onDelete = viewModel::delete,
                onDuplicate = viewModel::duplicate,
                onMoveSegment = viewModel::moveSegment,
                onTrimSegment = viewModel::trimSegment,
            )
        }

        DockBar(
            activeTool = activeTool,
            onSelect = { tool -> activeTool = if (activeTool == tool) null else tool },
        )

        activeTool?.let { tool ->
            Surface(color = NovaSurface, tonalElevation = 4.dp) {
                PanelHost(tool = tool, project = project, selectedSegment = viewModel.selectedSegment, viewModel = viewModel)
            }
        }
    }
}

private fun formatNovaTimecode(timeUs: Long): String {
    val totalMs = (timeUs / 1_000L).coerceAtLeast(0L)
    val minutes = totalMs / 60_000L
    val seconds = (totalMs / 1_000L) % 60L
    val millis = totalMs % 1_000L
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

@Composable
internal fun VSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp).size(width = 1.dp, height = 18.dp).background(Color(0xFF30343E)),
    )
}
