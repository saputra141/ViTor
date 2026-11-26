package com.saputra.vitor.timeline

import com.saputra.vitor.subtitles.SubtitleCue

/**
 * Serializable-by-fields project model. All timeline positions and source ranges use milliseconds.
 * URIs are stored as strings so the model remains independent of Android framework classes.
 */
data class VideoClip(
    val id: String,
    val sourceUri: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    val volume: Float = 1f
) {
    init {
        require(id.isNotBlank())
        require(sourceUri.isNotBlank())
        require(sourceDurationMs > 0L)
        require(trimStartMs >= 0L && trimEndMs > trimStartMs)
        require(trimEndMs <= sourceDurationMs)
        require(volume in 0f..1f)
    }

    val durationMs: Long get() = trimEndMs - trimStartMs
}

data class AudioLayer(
    val id: String,
    val sourceUri: String,
    val startMs: Long = 0L,
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val loop: Boolean = false
) {
    init {
        require(id.isNotBlank() && sourceUri.isNotBlank())
        require(startMs >= 0L && sourceStartMs >= 0L)
        require(sourceEndMs > sourceStartMs)
        require(volume in 0f..1f)
    }

    val durationMs: Long get() = sourceEndMs - sourceStartMs
    val timelineEndMs: Long get() = startMs + durationMs
}

data class TextLayer(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val x: Float = 0.5f,
    val y: Float = 0.82f
) {
    init {
        require(id.isNotBlank())
        require(text.isNotBlank())
        require(startMs >= 0L && endMs > startMs)
        require(x in 0f..1f && y in 0f..1f)
    }
}

data class TimelineProject(
    val name: String = "Untitled project",
    val videoClips: List<VideoClip> = emptyList(),
    val audioLayers: List<AudioLayer> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val subtitleCues: List<SubtitleCue> = emptyList()
) {
    init {
        require(name.isNotBlank())
        require(videoClips.map { it.id }.distinct().size == videoClips.size)
        require(audioLayers.map { it.id }.distinct().size == audioLayers.size)
        require(textLayers.map { it.id }.distinct().size == textLayers.size)
        require(subtitleCues.map { it.id }.distinct().size == subtitleCues.size)
    }

    /** Video clips are concatenated in list order; audio and text layers use absolute timeline time. */
    val videoDurationMs: Long get() = videoClips.sumOf { it.durationMs }
    val durationMs: Long get() = maxOf(
        videoDurationMs,
        audioLayers.filterNot { it.muted }.maxOfOrNull { it.timelineEndMs } ?: 0L,
        textLayers.maxOfOrNull { it.endMs } ?: 0L,
        subtitleCues.maxOfOrNull { it.endMs } ?: 0L
    )

    fun moveVideoClip(fromIndex: Int, toIndex: Int): TimelineProject {
        require(fromIndex in videoClips.indices)
        require(toIndex in videoClips.indices)
        if (fromIndex == toIndex) return this
        val reordered = videoClips.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        return copy(videoClips = reordered)
    }

    fun replaceVideoClip(updated: VideoClip): TimelineProject {
        val index = videoClips.indexOfFirst { it.id == updated.id }
        require(index >= 0) { "Video clip not found: ${updated.id}" }
        return copy(videoClips = videoClips.toMutableList().apply { this[index] = updated })
    }

    fun upsertAudioLayer(layer: AudioLayer): TimelineProject {
        val index = audioLayers.indexOfFirst { it.id == layer.id }
        val updated = audioLayers.toMutableList()
        if (index >= 0) updated[index] = layer else updated.add(layer)
        return copy(audioLayers = updated)
    }

    fun removeAudioLayer(id: String): TimelineProject =
        copy(audioLayers = audioLayers.filterNot { it.id == id })

    fun upsertTextLayer(layer: TextLayer): TimelineProject {
        val index = textLayers.indexOfFirst { it.id == layer.id }
        val updated = textLayers.toMutableList()
        if (index >= 0) updated[index] = layer else updated.add(layer)
        return copy(textLayers = updated)
    }

    fun removeTextLayer(id: String): TimelineProject =
        copy(textLayers = textLayers.filterNot { it.id == id })
}
