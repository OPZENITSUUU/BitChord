package com.music.bitchord.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupIntroTimingTest {
    @Test
    fun `fresh launch shows intro for 3 point 2 seconds`() {
        assertEquals(3_200L, startupIntroRemainingMs(startedAtElapsedMs = 10_000L, nowElapsedMs = 10_000L))
    }

    @Test
    fun `rotation keeps only the original remaining time`() {
        assertEquals(2_000L, startupIntroRemainingMs(startedAtElapsedMs = 10_000L, nowElapsedMs = 11_200L))
    }

    @Test
    fun `expired intro does not restart after recreation`() {
        assertEquals(0L, startupIntroRemainingMs(startedAtElapsedMs = 10_000L, nowElapsedMs = 13_250L))
    }
}
