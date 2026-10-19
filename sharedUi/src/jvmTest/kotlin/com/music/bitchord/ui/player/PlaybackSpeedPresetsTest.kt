package com.music.bitchord.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedPresetsTest {
    @Test
    fun `speed dial contains the requested presets in order`() {
        assertEquals(
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
            PLAYBACK_SPEED_PRESETS,
        )
    }

    @Test
    fun `speed label shows the active multiplier`() {
        assertEquals("1×", playbackSpeedLabel(1f))
        assertEquals("1.75×", playbackSpeedLabel(1.75f))
    }
}
