package com.saputra.vitor.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SrtCodecTest {
    @Test
    fun parsesBomCrLfAndMultilineText() {
        val source = "\uFEFF1\r\n00:00:01,250 --> 00:00:03,500\r\nHalo semua\r\nSelamat datang\r\n\r\n2\r\n00:00:04,000 --> 00:00:05,000\r\nApa kabar?"
        val cues = SrtCodec.parse(source)
        assertEquals(2, cues.size)
        assertEquals(1_250L, cues[0].startMs)
        assertEquals(3_500L, cues[0].endMs)
        assertEquals("Halo semua\nSelamat datang", cues[0].text)
    }

    @Test
    fun writesAndReadsBackCues() {
        val cues = listOf(
            SubtitleCue("a", 1_250, 3_500, "Halo"),
            SubtitleCue("b", 4_000, 5_000, "Apa kabar?")
        )
        assertEquals(cues.map { it.copy(id = "") }, SrtCodec.parse(SrtCodec.write(cues)).map { it.copy(id = "") })
    }

    @Test
    fun rejectsInvalidTimestamp() {
        assertThrows(IllegalArgumentException::class.java) {
            SrtCodec.parse("1\n00:75:00,000 --> 00:75:01,000\nTeks")
        }
    }

    @Test
    fun rejectsEmptyCueText() {
        assertThrows(IllegalArgumentException::class.java) {
            SrtCodec.parse("1\n00:00:01,000 --> 00:00:02,000")
        }
    }
}
