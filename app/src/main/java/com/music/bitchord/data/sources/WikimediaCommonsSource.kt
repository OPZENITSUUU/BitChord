package com.music.bitchord.data.sources

import com.music.bitchord.data.Http
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.model.Song
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Search and stream FLAC audio that Wikimedia Commons labels Public Domain.
 *
 * Commons accepts audio only when the file is freely licensed, and this source
 * is narrower still: it exposes only files whose per-file metadata says
 * `Public domain` or `CC0`. It never fetches an arbitrary URL from a search
 * row; stream resolution re-reads the file's metadata and only accepts the
 * HTTPS Wikimedia upload host. The format values carried in [SourceStream]
 * are source metadata, not playback verification. Media3's decoder callback is
 * the authority for the format actually delivered to the player.
 */
class WikimediaCommonsSource(
    override val config: SourceConfig,
    private val apiEndpoint: String = API_ENDPOINT,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.WIKIMEDIA_COMMONS
    override val displayName: String get() = config.label.ifBlank { kind.label }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun health(): SourceHealth = try {
        val root = apiRequest(mapOf("action" to "query", "meta" to "siteinfo"))
        if (root["query"] is JsonObject) {
            SourceHealth.Ok("Commons API reachable; Public Domain/CC0 FLAC only")
        } else {
            SourceHealth.Rejected("Commons API returned an unexpected response")
        }
    } catch (e: Exception) {
        SourceHealth.Unreachable(e.message?.take(180) ?: "Commons API request failed")
    }

    override suspend fun search(
        query: String,
        limit: Int,
        waitForAll: Boolean,
        request: StreamRequest?,
    ): List<Song> {
        val terms = query.trim()
        if (terms.isEmpty()) return emptyList()
        return try {
            val page = apiRequest(
                mapOf(
                    "action" to "query",
                    "generator" to "search",
                    "gsrnamespace" to "6",
                    "gsrsearch" to "filemime:audio/flac $terms",
                    "gsrlimit" to limit.coerceIn(1, MAX_RESULTS).toString(),
                    "prop" to "imageinfo",
                    "iiprop" to "url|size|mime|metadata|extmetadata",
                ),
            )
            val pages = page["query"]?.jsonObject?.get("pages") as? JsonObject ?: return emptyList()
            pages.values.mapNotNull(::parseFile)
                .filter { meetsRequest(it.format, request) }
                .take(limit.coerceAtLeast(0))
                .map { it.song }
                .also { TrackLog.d(TAG, "Commons FLAC search returned ${it.size} Public Domain/CC0 results") }
        } catch (e: Exception) {
            TrackLog.w(TAG, "Commons FLAC search failed: ${e.message?.take(180) ?: e.javaClass.simpleName}")
            emptyList()
        }
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        val pageId = trackId.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return try {
            val root = apiRequest(
                mapOf(
                    "action" to "query",
                    "pageids" to pageId.toString(),
                    "prop" to "imageinfo",
                    "iiprop" to "url|size|mime|metadata|extmetadata",
                ),
            )
            val pages = root["query"]?.jsonObject?.get("pages") as? JsonObject ?: return null
            val file = pages.values.firstNotNullOfOrNull(::parseFile) ?: return null
            if (!meetsRequest(file.format, request)) return null
            SourceStream(
                url = file.url,
                format = file.format,
                durationSec = file.durationSec,
            )
        } catch (e: Exception) {
            TrackLog.w(TAG, "Commons FLAC stream resolution failed: ${e.message?.take(180) ?: e.javaClass.simpleName}")
            null
        }
    }

    private fun meetsRequest(format: StreamFormat, request: StreamRequest?): Boolean {
        val sourceKbps = format.kbps
        return when (request) {
            is StreamRequest.Capped -> sourceKbps != null && sourceKbps <= request.maxKbps
            else -> true
        }
    }

    private fun parseFile(page: JsonElement): CommonsFile? {
        val fields = page.jsonObject
        val pageId = fields["pageid"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val title = fields["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val imageInfo = (fields["imageinfo"] as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
        val mime = imageInfo["mime"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT).orEmpty()
        if (mime !in ACCEPTED_FLAC_MIME) return null

        val metadata = imageInfo["metadata"]
        val ext = imageInfo["extmetadata"]?.jsonObject
        val license = ext?.get("LicenseShortName")?.jsonObject?.get("value")
            ?.jsonPrimitive?.contentOrNull?.let(::plainText)?.trim().orEmpty()
        if (!isPermittedLicense(license)) return null

        val rawUrl = imageInfo["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val url = rawUrl.toHttpUrlOrNull()?.takeIf {
            it.scheme == "https" && it.host == UPLOAD_HOST
        }?.toString() ?: return null

        val sampleRate = findNamed(metadata, "sample_rate")?.jsonPrimitive?.doubleOrNull?.roundToInt()
        val bitDepth = findNamed(metadata, "bits_per_sample")?.jsonPrimitive?.doubleOrNull?.roundToInt()
        val bitrate = findNamed(metadata, "bitrate")?.jsonPrimitive?.doubleOrNull
            ?.takeIf { it > 0 }?.div(1000.0)?.roundToInt()
        val playbackTime = findNamed(metadata, "playtime_string")?.jsonPrimitive?.contentOrNull
        val durationSec = parseDuration(playbackTime)
        val credit = ext?.get("Credit")?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
            ?.let(::plainText)?.trim()?.takeIf { it.isNotBlank() }
        val artist = credit
            ?: ext?.get("Artist")?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
                ?.let(::plainText)?.trim()?.takeIf { it.isNotBlank() }
            ?: "Unknown Artist"
        val displayTitle = title.removePrefix("File:").removeSuffixIgnoreCase(".flac")
            .ifBlank { "Untitled Commons audio" }

        return CommonsFile(
            song = Song(
                videoId = SourceRegistry.trackKey(configId, pageId.toString()),
                title = displayTitle,
                artist = artist,
                thumbnailUrl = null,
                durationText = playbackTime,
                sourceQuality = "LOSSLESS",
                sourceHiRes = (bitDepth ?: 0) > 16 || (sampleRate ?: 0) > 48_000,
            ),
            url = url,
            format = StreamFormat(codec = "flac", kbps = bitrate, sampleRateHz = sampleRate, bitDepth = bitDepth),
            durationSec = durationSec,
        )
    }

    private fun apiRequest(parameters: Map<String, String>): JsonObject {
        val base = apiEndpoint.toHttpUrlOrNull() ?: throw IOException("Invalid Commons API endpoint")
        val builder = base.newBuilder()
        parameters.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        builder.addQueryParameter("format", "json")
        val httpRequest = Request.Builder()
            .url(builder.build())
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return Http.client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Commons API HTTP ${response.code}")
            val body = response.body ?: throw IOException("Commons API returned an empty body")
            if (body.contentLength() > MAX_RESPONSE_BYTES) throw IOException("Commons API response is too large")
            val text = body.string()
            if (text.toByteArray(Charsets.UTF_8).size > MAX_RESPONSE_BYTES) {
                throw IOException("Commons API response is too large")
            }
            json.parseToJsonElement(text).jsonObject
        }
    }

    private fun isPermittedLicense(value: String): Boolean =
        value.equals("Public domain", ignoreCase = true) ||
            value.equals("CC0", ignoreCase = true) ||
            value.startsWith("CC0 ", ignoreCase = true)

    private fun findNamed(element: JsonElement?, target: String): JsonElement? = when (element) {
        is JsonObject -> {
            val name = element["name"]?.jsonPrimitive?.contentOrNull
            if (name.equals(target, ignoreCase = true)) element["value"]
            else element.values.firstNotNullOfOrNull { findNamed(it, target) }
        }
        is JsonArray -> element.firstNotNullOfOrNull { findNamed(it, target) }
        else -> null
    }

    private fun parseDuration(value: String?): Int? {
        val parts = value?.split(':') ?: return null
        return when (parts.size) {
            2 -> parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: return null)
            3 -> parts[0].toIntOrNull()?.times(3600)
                ?.plus((parts[1].toIntOrNull() ?: return null) * 60)
                ?.plus(parts[2].toIntOrNull() ?: return null)
            else -> null
        }?.takeIf { it > 0 }
    }

    private fun plainText(value: String): String = value
        .replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.removeSuffixIgnoreCase(suffix: String): String =
        if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this

    private data class CommonsFile(
        val song: Song,
        val url: String,
        val format: StreamFormat,
        val durationSec: Int?,
    )

    private companion object {
        const val TAG = "BitChord"
        const val MAX_RESULTS = 50
        const val MAX_RESPONSE_BYTES = 1_048_576
        const val UPLOAD_HOST = "upload.wikimedia.org"
        const val API_ENDPOINT = "https://commons.wikimedia.org/w/api.php"
        const val USER_AGENT = "BitChord personal fork (Wikimedia Commons source; https://github.com/OPZENITSUUU/BitChord)"
        val ACCEPTED_FLAC_MIME = setOf("audio/flac", "audio/x-flac")
    }
}
