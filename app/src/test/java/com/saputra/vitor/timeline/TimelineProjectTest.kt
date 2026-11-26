package com.saputra.vitor.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineProjectTest {
    private fun clip(id: String, duration: Long) = VideoClip(
        id = id,
        sourceUri = "content://video/$id",
        sourceDurationMs = duration
    )

    @Test fun videoClipsReorderWithoutChangingClipData() {
        val first = clip("a", 1_000)
        val second = clip("b", 2_000)
        val third = clip("c", 3_000)
        val project = TimelineProject(videoClips = listOf(first, second, third))

        val moved = project.moveVideoClip(2, 0)

        assertEquals(listOf("c", "a", "b"), moved.videoClips.map { it.id })
        assertEquals(6_000L, moved.videoDurationMs)
        assertEquals(listOf(first, second, third), project.videoClips)
    }

    @Test fun trimRangeDeterminesClipDuration() {
        val trimmed = clip("a", 10_000).copy(trimStartMs = 2_000, trimEndMs = 7_500)
        assertEquals(5_500L, trimmed.durationMs)
    }

    @Test fun invalidTrimRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            clip("a", 5_000).copy(trimStartMs = 4_000, trimEndMs = 6_000)
        }
    }

    @Test fun audioAndTextLayersContributeToProjectDuration() {
        val project = TimelineProject(
            videoClips = listOf(clip("v", 8_000)),
            audioLayers = listOf(
                AudioLayer("music", "content://audio/music", startMs = 1_000, sourceEndMs = 12_000)
            ),
            textLayers = listOf(TextLayer("title", "Hello", 500, 4_000))
        )
        assertEquals(13_000L, project.durationMs)
    }

    @Test fun mutedAudioDoesNotExtendRenderedProjectDuration() {
        val project = TimelineProject(
            videoClips = listOf(clip("v", 8_000)),
            audioLayers = listOf(
                AudioLayer("music", "content://audio/music", startMs = 0, sourceEndMs = 20_000, muted = true)
            )
        )
        assertEquals(8_000L, project.durationMs)
    }

    @Test fun audioAndTextLayersCanBeUpdatedAndRemoved() {
        val project = TimelineProject()
            .upsertAudioLayer(AudioLayer("music", "content://audio/music", sourceEndMs = 5_000))
            .upsertTextLayer(TextLayer("caption", "Hi", 0, 2_000))
        val updated = project
            .upsertAudioLayer(AudioLayer("music", "content://audio/music", sourceEndMs = 7_000, volume = 0.5f))
            .removeTextLayer("caption")

        assertEquals(1, updated.audioLayers.size)
        assertEquals(7_000L, updated.audioLayers.single().durationMs)
        assertTrue(updated.textLayers.isEmpty())
        assertFalse(updated.audioLayers.single().muted)
    }
}
