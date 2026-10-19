package com.music.bitchord

import com.music.bitchord.playback.formatForMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackFormatOwnershipTest {
    @Test
    fun `format from previous track is not attributed to current track`() {
        val format = "audio/flac"

        assertNull(formatForMediaItem("track-b", "track-a", format))
    }

    @Test
    fun `format is exposed when its analytics event belongs to current track`() {
        val format = "audio/flac"

        assertEquals(format, formatForMediaItem("track-a", "track-a", format))
    }

    @Test
    fun `format is unknown before an owning track is available`() {
        assertNull(formatForMediaItem("track-a", null, "audio/flac"))
        assertNull(formatForMediaItem(null, "track-a", "audio/flac"))
        assertNull(formatForMediaItem("", "", "audio/flac"))
    }
}
