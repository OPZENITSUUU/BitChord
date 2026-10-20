package com.music.bitchord.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-local visibility choices for the Home feed.
 *
 * This is deliberately independent of the account/library stores: hiding a song
 * only changes recommendation shelves on this device and must not affect library,
 * search, queue, or downloaded files.
 */
class HomeSongVisibility(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _hiddenVideoIds = MutableStateFlow(
        preferences.getStringSet(HIDDEN_VIDEO_IDS, emptySet()).orEmpty().toSet(),
    )

    val hiddenVideoIds: StateFlow<Set<String>> = _hiddenVideoIds.asStateFlow()

    fun setHidden(videoId: String, hidden: Boolean) {
        val next = _hiddenVideoIds.value.toMutableSet().apply {
            if (hidden) add(videoId) else remove(videoId)
        }.toSet()
        if (next == _hiddenVideoIds.value) return
        _hiddenVideoIds.value = next
        preferences.edit().putStringSet(HIDDEN_VIDEO_IDS, next).apply()
    }

    private companion object {
        const val PREFERENCES = "home_song_visibility"
        const val HIDDEN_VIDEO_IDS = "hidden_video_ids"
    }
}
