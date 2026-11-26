package com.saputra.vitor.timeline

import com.saputra.vitor.subtitles.SubtitleCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProjectFileStoreTest {
    @Test fun saveLoadAndListRoundTripAllTimelineTracks() {
        val directory = Files.createTempDirectory("vitor-project-test").toFile()
        try {
            val store = ProjectFileStore(directory)
            val project = TimelineProject(
                name = "Travel edit",
                videoClips = listOf(VideoClip("v1", "content://video/1", 10_000, 1_000, 9_000)),
                audioLayers = listOf(AudioLayer("a1", "content://audio/1", 500, 0, 8_000, 0.6f)),
                textLayers = listOf(TextLayer("t1", "Opening", 0, 2_000)),
                subtitleCues = listOf(SubtitleCue("s1", 2_000, 4_000, "Hello"))
            )

            val summary = store.save(project, "travel-edit")
            val restored = store.load("travel-edit")

            assertEquals(project, restored)
            assertEquals("Travel edit", summary.name)
            assertEquals(listOf("travel-edit"), store.list().map { it.projectId })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun rejectsPathTraversalProjectIds() {
        val store = ProjectFileStore(Files.createTempDirectory("vitor-project-test").toFile())
        assertThrows(IllegalArgumentException::class.java) { store.load("../outside") }
    }

    @Test fun deleteRemovesSavedProject() {
        val directory = Files.createTempDirectory("vitor-project-test").toFile()
        try {
            val store = ProjectFileStore(directory)
            store.save(TimelineProject(name = "Delete me"), "delete-me")
            assertTrue(store.delete("delete-me"))
            assertFalse(directory.resolve("delete-me.vitor.json").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
