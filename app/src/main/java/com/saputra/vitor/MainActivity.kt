package com.saputra.vitor

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF3DAEE9),
                secondary = Color(0xFF56D6C2),
                background = Color(0xFF17191D),
                surface = Color(0xFF22252B)
            )) {
                var projectId by remember { mutableStateOf<String?>(null) }
                var showExport by remember { mutableStateOf(false) }
                var importedMedia by remember { mutableStateOf<List<Uri>>(emptyList()) }
                val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                    uris.forEach { uri -> runCatching { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                    importedMedia = (importedMedia + uris).distinct()
                }

                BackHandler(enabled = showExport || projectId != null) {
                    if (showExport) showExport = false else projectId = null
                }

                when {
                    showExport && projectId != null -> com.zaitxcode.opencut.ui.export.ExportScreen(
                        projectId = projectId!!,
                        onBack = { showExport = false },
                    )
                    projectId != null -> KdenliveWorkspaceScreen(
                        projectId = projectId!!,
                        mediaUris = importedMedia,
                        onImportMedia = { mediaPicker.launch(arrayOf("video/*", "audio/*", "image/*")) },
                        onRemoveMedia = { uri -> importedMedia = importedMedia.filterNot { it == uri } },
                        onBack = { projectId = null; importedMedia = emptyList() },
                        onExport = { showExport = true },
                    )
                    else -> ProjectLibraryScreen(onOpenProject = { projectId = it })
                }
            }
        }
    }
}
