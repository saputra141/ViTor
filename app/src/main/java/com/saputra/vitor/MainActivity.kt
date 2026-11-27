package com.saputra.vitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
                        onBack = { projectId = null },
                        onExport = { showExport = true },
                    )
                    else -> ProjectLibraryScreen(onOpenProject = { projectId = it })
                }
            }
        }
    }
}
