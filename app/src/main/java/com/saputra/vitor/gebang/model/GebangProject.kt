package com.saputra.vitor.gebang.model

import java.util.UUID

/**
 * Android-side project model boundary.
 *
 * This is a new compatibility layer, not a claim that the original Gebang
 * project-file format has been decoded. Keep serialization behind a codec
 * until the creator's format specification/source is available.
 */
data class GebangProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val width: Int,
    val height: Int,
    val frameRate: Double,
    val tracks: List<EditorTrack> = emptyList(),
    val mediaReferences: List<MediaReference> = emptyList(),
    val schemaVersion: Int = 1
)

data class EditorTrack(
    val id: String,
    val kind: TrackKind,
    val order: Int,
    val clips: List<EditorClip> = emptyList()
)

enum class TrackKind { VIDEO, AUDIO, TEXT, OVERLAY, EFFECT }

data class EditorClip(
    val id: String,
    val mediaId: String?,
    val timelineStartUs: Long,
    val sourceInUs: Long,
    val sourceOutUs: Long,
    val speed: Double = 1.0,
    val enabled: Boolean = true
) {
    init {
        require(timelineStartUs >= 0)
        require(sourceInUs >= 0)
        require(sourceOutUs > sourceInUs)
        require(speed > 0.0)
    }
}

data class MediaReference(
    val id: String,
    val uri: String,
    val displayName: String,
    val mimeType: String? = null,
    val durationUs: Long? = null
)
