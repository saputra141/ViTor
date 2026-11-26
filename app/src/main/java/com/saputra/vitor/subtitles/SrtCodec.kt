package com.saputra.vitor.subtitles

import java.util.Locale

/** Strict, dependency-free SRT reader/writer. Keeps multi-line cue text intact. */
object SrtCodec {
    private val timestampPattern = Regex("^(\\d{2,}):(\\d{2}):(\\d{2})[,.](\\d{3})$")

    fun parse(source: String): List<SubtitleCue> {
        val normalized = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val blocks = normalized.trim().split(Regex("\\n[ \\t]*\\n+"))
        val cues = mutableListOf<SubtitleCue>()

        blocks.forEachIndexed { blockIndex, rawBlock ->
            val lines = rawBlock.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) return@forEachIndexed
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            require(timingIndex >= 0) { "SRT block ${blockIndex + 1} has no timing line" }
            val timing = lines[timingIndex].split("-->", limit = 2)
            require(timing.size == 2) { "Invalid timing line in block ${blockIndex + 1}" }
            val start = parseTimestamp(timing[0].trim())
            val endToken = timing[1].trim().substringBefore(' ').trim()
            val end = parseTimestamp(endToken)
            val text = lines.drop(timingIndex + 1).joinToString("\n").trim()
            require(text.isNotBlank()) { "SRT block ${blockIndex + 1} has no subtitle text" }
            cues += SubtitleCue(
                id = "srt-${cues.size + 1}",
                startMs = start,
                endMs = end,
                text = text
            )
        }
        return cues.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.endMs })
    }

    fun write(cues: List<SubtitleCue>): String = cues
        .sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.endMs })
        .mapIndexed { index, cue ->
            "${index + 1}\n${formatTimestamp(cue.startMs)} --> ${formatTimestamp(cue.endMs)}\n${cue.text}"
        }
        .joinToString("\n\n")
        .let { if (it.isEmpty()) "" else "$it\n" }

    private fun parseTimestamp(value: String): Long {
        val match = timestampPattern.matchEntire(value)
            ?: throw IllegalArgumentException("Invalid SRT timestamp: $value")
        val hours = match.groupValues[1].toLong()
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val millis = match.groupValues[4].toLong()
        require(minutes < 60 && seconds < 60) { "Invalid SRT timestamp: $value" }
        return (((hours * 60 + minutes) * 60 + seconds) * 1000) + millis
    }

    private fun formatTimestamp(milliseconds: Long): String {
        require(milliseconds >= 0)
        val hours = milliseconds / 3_600_000
        val minutes = (milliseconds / 60_000) % 60
        val seconds = (milliseconds / 1_000) % 60
        val millis = milliseconds % 1_000
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
