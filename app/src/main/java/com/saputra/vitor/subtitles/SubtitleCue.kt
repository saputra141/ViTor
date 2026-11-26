package com.saputra.vitor.subtitles

/** A subtitle segment on the project timeline. All timestamps are milliseconds. */
data class SubtitleCue(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<WordTiming> = emptyList()
) {
    init {
        require(startMs >= 0) { "Subtitle start cannot be negative" }
        require(endMs > startMs) { "Subtitle end must be after its start" }
        require(text.isNotBlank()) { "Subtitle text cannot be empty" }
        require(words.all { it.startMs >= startMs && it.endMs <= endMs && it.endMs > it.startMs }) {
            "Word timings must be inside their subtitle segment"
        }
    }
}

/** Optional word-level alignment, used by karaoke-style lyric highlighting. */
data class WordTiming(
    val text: String,
    val startMs: Long,
    val endMs: Long
) {
    init {
        require(text.isNotBlank())
        require(startMs >= 0 && endMs > startMs)
    }
}

enum class TranscriptionMode { SPEECH_CAPTIONS, SONG_LYRICS }

enum class SubtitleExportFormat { SRT, WEBVTT }
