package com.saputra.vitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF8B80F9),
                background = Color(0xFF101116),
                surface = Color(0xFF1A1B23)
            )) {
                ViTorHome()
            }
        }
    }
}

@Composable
private fun ViTorHome() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101116)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("ViTor", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Studio editor video untuk Android", color = Color(0xFFAAAAB8), fontSize = 14.sp)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222330))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFAAA0FF), modifier = Modifier.size(34.dp))
                    Text("Mulai proyek baru", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Ruang kerja editing sedang disiapkan. Import media, timeline, efek, dan ekspor akan ditambahkan bertahap.",
                        color = Color(0xFFB8B8C5), fontSize = 13.sp
                    )
                }
            }
            Text("FITUR YANG AKAN HADIR", color = Color(0xFFAAAAB8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FeatureRow(Icons.Default.VideoLibrary, "Media", "Import video dan gambar")
            FeatureRow(Icons.Default.AudioFile, "Audio", "Musik, suara, dan pengaturan audio")
            FeatureRow(Icons.Default.TextFields, "Teks & efek", "Layer teks, animasi, dan efek visual")
            Spacer(Modifier.weight(1f))
            Text("ViTor • Android ARM64 • Versi 0.1.0", color = Color(0xFF777887), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Color(0xFF252637), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFAAA0FF))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF9696A5), fontSize = 12.sp)
        }
    }
}
