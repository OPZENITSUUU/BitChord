package com.music.bitchord

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.settings.withoutHiddenHomeSongs
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeShelfVisibilityTest {
    @Test
    fun removesOnlyHiddenVideoIdsFromHomeShelves() {
        val shelves = listOf(
            HomeShelf(
                title = "For you",
                items = listOf(
                    ShelfItem("Same title", "Artist A", null, "video-hidden", null),
                    ShelfItem("Same title", "Artist B", null, "video-visible", null),
                    ShelfItem("Playlist", "Playlist", null, null, "VLplaylist"),
                ),
            ),
        )

        val result = shelves.withoutHiddenHomeSongs(setOf("video-hidden"))

        assertEquals(listOf("video-visible", null), result.single().items.map { it.videoId })
        assertEquals("For you", result.single().title)
    }

    @Test
    fun removesEmptyShelvesButDoesNotTouchOtherShelves() {
        val shelves = listOf(
            HomeShelf("Only hidden", listOf(ShelfItem("Hidden", "Artist", null, "id", null))),
            HomeShelf("Unrelated", listOf(ShelfItem("Visible", "Artist", null, "other", null))),
        )

        val result = shelves.withoutHiddenHomeSongs(setOf("id"))

        assertEquals(listOf("Unrelated"), result.map { it.title })
        assertEquals("other", result.single().items.single().videoId)
    }
}
