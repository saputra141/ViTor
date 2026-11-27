package com.saputra.vitor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.saputra.vitor.ViTorApplication
import com.zaitxcode.opencut.core.editor.newProject
import com.zaitxcode.opencut.core.model.Project
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/** Local project browser. Projects and their media stay on the device. */
@Composable
fun ProjectLibraryScreen(onOpenProject: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as ViTorApplication
    val scope = rememberCoroutineScope()
    val projects = remember { mutableStateListOf<Project>() }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Project?>(null) }

    suspend fun refresh() {
        loading = true
        projects.clear()
        projects.addAll(app.projectStore.listProjects())
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("ViTor Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Video editor • Proyek lokal", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = {
                    scope.launch {
                        val project = newProject("Project ${projects.size + 1}")
                        if (app.projectStore.writeProject(project)) {
                            projects.add(0, project)
                            onOpenProject(project.id)
                        } else {
                            message = "Proyek baru gagal disimpan. Coba lagi."
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Proyek baru")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Proyek saya", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                loading -> CircularProgressIndicator()
                projects.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp))
                            Text("Belum ada proyek", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp))
                            Text("Buat proyek untuk mulai mengedit video, foto, dan audio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp))
                            Button(onClick = {
                                scope.launch {
                                    val project = newProject("Project 1")
                                    if (app.projectStore.writeProject(project)) {
                                        projects.add(project)
                                        onOpenProject(project.id)
                                    } else message = "Proyek gagal dibuat."
                                }
                            }, modifier = Modifier.padding(top = 16.dp)) { Text("Buat proyek pertama") }
                        }
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(projects, key = { it.id }) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenProject(project.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${project.tracks.sumOf { it.segments.size }} klip • ${project.materials.videos.size + project.materials.images.size + project.materials.audios.size} media • ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.updatedAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        val copy = app.projectStore.duplicateProject(
                                            project, UUID.randomUUID().toString(), "${project.name} copy"
                                        )
                                        if (copy != null) refresh() else message = "Duplikasi proyek gagal."
                                    }
                                }) { Icon(Icons.Default.ContentCopy, contentDescription = "Duplikasi proyek") }
                                IconButton(onClick = { deleteTarget = project }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus proyek",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.padding(bottom = 4.dp))
            OutlinedButton(onClick = { scope.launch { refresh() } }, modifier = Modifier.align(Alignment.End)) {
                Text("Muat ulang")
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus proyek?") },
            text = { Text("Proyek \"${target.name}\" dan file timeline-nya akan dihapus dari perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val deleted = app.projectStore.deleteProject(target.id)
                        if (deleted) projects.removeAll { it.id == target.id }
                        else message = "Proyek gagal dihapus."
                        deleteTarget = null
                    }
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } },
        )
    }
}
