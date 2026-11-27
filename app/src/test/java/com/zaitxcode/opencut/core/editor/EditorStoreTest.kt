package com.zaitxcode.opencut.core.editor

import com.zaitxcode.opencut.core.model.Material
import com.zaitxcode.opencut.core.model.MaterialKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStoreTest {

    @Test
    fun addTextCanUndoAndRedo() {
        val store = EditorStore(newProject("Unit test"))

        store.addText("Hello ViTor", 0L)

        assertEquals(3_000_000L, store.project.durationUs)
        assertNotNull(store.selectedSegment)
        assertTrue(store.canUndo)

        store.undo()
        assertEquals(0L, store.project.durationUs)
        assertTrue(store.project.tracks.isEmpty())
        assertTrue(store.canRedo)

        store.redo()
        assertEquals(3_000_000L, store.project.durationUs)
        assertEquals("Hello ViTor", store.selectedSegment?.let {
            store.project.materials.find(it.materialId)?.content
        })
    }

    @Test
    fun splitCreatesContiguousSourceAndTimelineRanges() {
        val store = EditorStore(newProject("Split test"))
        store.addText("Caption", 0L)
        val originalId = store.selectedSegment!!.id

        store.seek(1_250_000L)
        assertTrue(store.splitAtPlayhead())

        val track = store.project.tracks.single()
        assertEquals(2, track.segments.size)
        val left = track.segments[0]
        val right = track.segments[1]
        assertEquals(originalId, left.id)
        assertEquals(1_250_000L, left.target.duration)
        assertEquals(1_250_000L, right.target.start)
        assertEquals(1_250_000L, right.source.start)
        assertEquals(1_750_000L, right.source.duration)
    }

    @Test
    fun speedChangeUpdatesTimelineDurationAndCanBeUndone() {
        val store = EditorStore(newProject("Speed test"))
        val material = Material(
            id = "video-1",
            kind = MaterialKind.VIDEO,
            path = "/tmp/video.mp4",
            durationUs = 8_000_000L,
        )
        val segmentId = store.addMedia(material, startUs = 0L)

        store.setSpeed(segmentId, 2f)

        assertEquals(4_000_000L, store.selectedSegment?.target?.duration)
        store.undo()
        assertEquals(8_000_000L, store.selectedSegment?.target?.duration)
    }
}
