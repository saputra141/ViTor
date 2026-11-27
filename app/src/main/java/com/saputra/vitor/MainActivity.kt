package com.saputra.vitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.zaitxcode.opencut.ui.editor.EditorScreen
import com.zaitxcode.opencut.ui.export.ExportScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF9B8CFF),
                secondary = Color(0xFF56D6C2),
                background = Color(0xFF101116),
                surface = Color(0xFF1B1D26)
            )) {
                var projectId by remember { mutableStateOf<String?>(null) }
                var showExport by remember { mutableStateOf(false) }

                BackHandler(enabled = showExport || projectId != null) {
                    if (showExport) showExport = false else projectId = null
                }

                when {
                    showExport && projectId != null -> ExportScreen(
                        projectId = projectId!!,
                        onBack = { showExport = false },
                    )
                    projectId != null -> EditorScreen(
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
