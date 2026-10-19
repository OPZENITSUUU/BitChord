package com.music.bitchord.playback

/**
 * Media3's renderer format can outlive a queue transition. Only expose it as
 * current-track telemetry when the analytics event that produced it belongs to
 * the current media item; otherwise the honest value is unknown until the next
 * input-format callback arrives.
 */
internal fun <T : Any> formatForMediaItem(
    currentMediaId: String?,
    formatMediaId: String?,
    format: T?,
): T? = format?.takeIf {
    !currentMediaId.isNullOrBlank() && currentMediaId == formatMediaId
}
