package com.saputra.vitor

import android.os.Bundle
import androidx.activity.ComponentActivity
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
                var showExport by remember { mutableStateOf(false) }
                if (showExport) {
                    ExportScreen(projectId = "vitor-main", onBack = { showExport = false })
                } else {
                    EditorScreen(
                        projectId = "vitor-main",
                        onBack = { },
                        onExport = { showExport = true }
                    )
                }
            }
        }
    }
}
