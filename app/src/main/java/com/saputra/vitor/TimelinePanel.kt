package com.saputra.vitor
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TimelinePanel(
    clipNames: List<String>,
    selectedIndex: Int,
    playheadMs: Long,
    durationMs: Long,
    onSelectClip: (Int) -> Unit,
    onSeek: (Long) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF171821), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TIMELINE / MULTI-TRACK", color = Color(0xFFE5E3F5), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${clipNames.size} klip", color = Color(0xFFAAAAB8), fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VIDEO", color = Color(0xFFAAA0FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                clipNames.forEachIndexed { index, name ->
                    Card(
                        onClick = { onSelectClip(index) },
                        colors = CardDefaults.cardColors(containerColor = if (index == selectedIndex) Color(0xFF51458B) else Color(0xFF292A39)),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(112.dp).padding(horizontal = 8.dp, vertical = 11.dp))
                    }
                }
                if (clipNames.isEmpty()) Text("Import video untuk mengisi track", color = Color(0xFF777987), fontSize = 10.sp)
            }
        }
        TrackPlaceholder("AUDIO", Color(0xFF56D6C2), "Track audio siap dikembangkan")
        TrackPlaceholder("TEKS", Color(0xFFFFC66D), "Track judul dan subtitle")
        Text("PLAYHEAD  ${formatTimelineTime(playheadMs)} / ${formatTimelineTime(durationMs)}", color = Color(0xFFB9B7CB), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = playheadMs.coerceIn(0L, safeDuration).toFloat(), onValueChange = { onSeek(it.toLong()) }, valueRange = 0f..safeDuration.toFloat(), enabled = durationMs > 0L)
    }
}

@Composable
private fun TrackPlaceholder(label: String, tint: Color, message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Box(modifier = Modifier.fillMaxWidth().height(28.dp).background(tint.copy(alpha = 0.10f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.CenterStart) {
            Text(message, color = tint.copy(alpha = 0.75f), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}

private fun formatTimelineTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
