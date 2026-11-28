package com.zaitxcode.opencut.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saputra.vitor.R
import com.zaitxcode.opencut.ui.editor.panel.Tool

private data class DockEntry(val tool: Tool, val labelRes: Int, val icon: ImageVector)

private val dockEntries = listOf(
    DockEntry(Tool.MEDIA, R.string.tool_media, Icons.Default.OndemandVideo),
    DockEntry(Tool.AUDIO, R.string.tool_audio, Icons.Default.MusicNote),
    DockEntry(Tool.TEXT, R.string.tool_text, Icons.Default.TextFields),
    DockEntry(Tool.STICKER, R.string.tool_sticker, Icons.Default.EmojiEmotions),
    DockEntry(Tool.FILTER, R.string.tool_filter, Icons.Default.FilterAlt),
    DockEntry(Tool.EFFECT, R.string.tool_effect, Icons.Default.AutoAwesome),
    DockEntry(Tool.ADJUST, R.string.tool_adjust, Icons.Default.Adjust),
    DockEntry(Tool.TRANSITION, R.string.tool_transition, Icons.Default.Autorenew),
    DockEntry(Tool.SPEED, R.string.tool_speed, Icons.Default.Speed),
    DockEntry(Tool.CANVAS, R.string.tool_canvas, Icons.Default.AspectRatio),
    DockEntry(Tool.CROP, R.string.tool_crop, Icons.Default.Crop),
    DockEntry(Tool.KEYFRAME, R.string.tool_keyframe, Icons.Default.Videocam),
)

@Composable
fun DockBar(activeTool: Tool?, onSelect: (Tool) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF15171D)).padding(top = 8.dp, bottom = 5.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            dockEntries.forEach { entry ->
                val selected = activeTool == entry.tool
                val label = stringResource(entry.labelRes)
                Column(
                    Modifier.width(62.dp).clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(entry.tool) }
                        .background(if (selected) Color(0xFF203638) else Color.Transparent)
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(entry.icon, contentDescription = label, tint = if (selected) Color(0xFF39D7D0) else Color(0xFFB7BBC5), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = if (selected) Color(0xFF39D7D0) else Color(0xFFB7BBC5), style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(1.dp).background(Color(0xFF292D36)))
    }
}

@Composable
internal fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF39D7D0) else Color(0xFF292D36))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color(0xFF061314) else Color(0xFFD4D7DE))
    }
}
