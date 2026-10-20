package com.music.bitchord.data.settings

import com.music.bitchord.data.model.HomeShelf

/** Removes hidden YouTube tracks from Home without changing any other surface's data. */
internal fun List<HomeShelf>.withoutHiddenHomeSongs(hiddenVideoIds: Set<String>): List<HomeShelf> =
    map { shelf ->
        shelf.copy(items = shelf.items.filter { item ->
            item.videoId == null || item.videoId !in hiddenVideoIds
        })
    }.filter { it.items.isNotEmpty() }
