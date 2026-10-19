package com.music.bitchord.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.music.bitchord.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * BitChord ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * polls the repo's "latest release" once per launch and compares its tag
 * against the running build.
 *
 * The update itself is also handled here: the release's `.apk` asset is
 * downloaded into the app's cache and handed to the system package installer,
 * so the whole round trip stays inside the app instead of bouncing out to a
 * browser.
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        val apkSha256: String?,
        /** The release's own Markdown body, shown as this update's "what's new". */
        val notes: String?,
    )

    private const val CACHE_SUBDIR = "updates"
    private const val UPDATE_PREFS = "bitchord_update_state"
    private const val KEY_DISMISSED_VERSION = "dismissed_release_version"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/OPZENITSUUU/BitChord/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(
            val fraction: Float?,
            val downloadedBytes: Long,
            val totalBytes: Long?,
        ) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    suspend fun check(context: Context) = withContext(Dispatchers.IO) {
        _available.value = null
        val dismissedVersion = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DISMISSED_VERSION, null)
        runCatching {
            val request = Request.Builder().url(LATEST_RELEASE_URL).build()
            val body = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            } ?: return@runCatching
            val release = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val url = release["html_url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            if (!isPersonalReleaseUrl(url)) return@runCatching
            val apk = verifiedApkAsset(release)
            val notes = release["body"]?.jsonPrimitive?.contentOrNull
            val latest = tag.removePrefix("v")
            if (shouldNotifyUpdate(latest, BuildConfig.VERSION_NAME, dismissedVersion)) {
                _available.value = UpdateInfo(latest, url, apk?.url, apk?.sha256, notes)
            }
        }
    }

    fun dismiss(context: Context, version: String) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISMISSED_VERSION, version)
            .apply()
        _available.value = null
    }

    /**
     * Clears completed APKs from a previous run, but preserves a partial file
     * so a later user-approved download can resume with HTTP Range when supported.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()
            ?.filterNot { it.name.endsWith(".apk.part") }
            ?.forEach { it.delete() }
    }

    /**
     * One release carries the phone app and the TV app, each as a universal
     * APK plus per-ABI splits:
     *
     *     BitChord-v1.8.apk     BitChord-v1.8_arm64-v8a.apk     ...
     *     BitChordTV-v1.8.apk   BitChordTV-v1.8_arm64-v8a.apk   ...
     *
     * so this app's APKs are the ones named with its own
     * [BuildConfig.UPDATE_APK_PREFIX] — "BitChord-" never matches a TV file,
     * since the prefix ends at the hyphen. The universal build (no `_ABI`
     * suffix) is taken first, as it installs anywhere; failing that, the split
     * for this device's ABI. A release with neither leaves
     * [UpdateInfo.apkUrl] null and the UI falls back to opening the releases
     * page.
     */
    internal data class VerifiedApkAsset(val name: String, val url: String, val sha256: String)

    internal fun normalizeSha256Digest(raw: String?): String? {
        val digest = raw?.let {
            if (it.startsWith("sha256:", ignoreCase = true)) it.substringAfter(':') else it
        }?.lowercase() ?: return null
        return digest.takeIf { it.length == 64 && it.all { character -> character in '0'..'9' || character in 'a'..'f' } }
    }

    internal fun shouldNotifyUpdate(latest: String, current: String, dismissed: String?): Boolean =
        latest != dismissed && isNewer(latest, current)

    private fun isPersonalReleaseUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        return url.scheme == "https" && url.host == "github.com" &&
            url.encodedPath.startsWith("/OPZENITSUUU/BitChord/releases/")
    }

    internal fun verifiedApkAsset(release: JsonObject): VerifiedApkAsset? = runCatching {
        val apks = release["assets"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.mapNotNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val digest = normalizeSha256Digest(asset["digest"]?.jsonPrimitive?.contentOrNull)
                    ?: return@mapNotNull null
                val parsedUrl = url.toHttpUrlOrNull() ?: return@mapNotNull null
                val safeGithubAsset = parsedUrl.scheme == "https" &&
                    parsedUrl.host == "github.com" &&
                    parsedUrl.encodedPath.startsWith("/OPZENITSUUU/BitChord/releases/download/")
                val isOurApk = name.startsWith(BuildConfig.UPDATE_APK_PREFIX, ignoreCase = true) &&
                    name.endsWith(".apk", ignoreCase = true) &&
                    asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
                if (safeGithubAsset && isOurApk) {
                    VerifiedApkAsset(name.substringBeforeLast('.'), url, digest)
                } else {
                    null
                }
            }
            .orEmpty()
        apks.firstOrNull { '_' !in it.name }
            ?: Build.SUPPORTED_ABIS.firstNotNullOfOrNull { abi ->
                apks.firstOrNull { it.name.endsWith("_$abi") }
            }
    }.getOrNull()

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download]. A finished file survives a cancelled dialog: until
     * the state is reset, "Install Now" comes straight back without a second
     * download.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        val expectedSha256 = normalizeSha256Digest(info.apkSha256)
            ?: run {
                _download.value = DownloadState.Failed("Release APK has no valid SHA-256 digest")
                return@withContext
            }
        downloadCancelled = false
        _download.value = DownloadState.Downloading(null, 0L, null)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            val partial = File(dir, "bitchord-${info.version}.apk.part")
            dir.listFiles()?.filter { it.name != partial.name }?.forEach { it.delete() }
            val target = File(dir, "bitchord-${info.version}.apk")
            target.delete()
            val offset = partial.length()
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept-Encoding", "identity")
            if (offset > 0L) requestBuilder.header("Range", "bytes=$offset-")
            val request = requestBuilder.build()
            Http.client.newCall(request).execute().use { response ->
                if (response.code == 416 && offset > 0L) {
                    partial.delete()
                    error("Saved partial download is no longer valid; retry to start from the beginning")
                }
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val append: Boolean
                val alreadyDownloaded: Long
                val total: Long?
                if (offset > 0L && response.code == 206) {
                    val range = parseContentRange(response.header("Content-Range"))
                        ?: error("Server returned an invalid Content-Range for resume")
                    check(range.start == offset) { "Server resumed at the wrong byte offset" }
                    append = true
                    alreadyDownloaded = offset
                    total = range.total
                } else if (response.code == 200) {
                    // The server ignored Range; restart safely from the full response.
                    if (offset > 0L) partial.delete()
                    append = false
                    alreadyDownloaded = 0L
                    total = body.contentLength().takeIf { it > 0 }
                } else {
                    error("Server did not honor the requested byte range")
                }

                var downloaded = alreadyDownloaded
                _download.value = DownloadState.Downloading(
                    fraction = total?.let { (downloaded.toFloat() / it).coerceIn(0f, 1f) },
                    downloadedBytes = downloaded,
                    totalBytes = total,
                )
                body.byteStream().use { input ->
                    FileOutputStream(partial, append).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (downloadCancelled) {
                                _download.value = DownloadState.Idle
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            _download.value = DownloadState.Downloading(
                                fraction = total?.let { (downloaded.toFloat() / it).coerceIn(0f, 1f) },
                                downloadedBytes = downloaded,
                                totalBytes = total,
                            )
                        }
                    }
                }
                if (total != null) check(downloaded == total) { "Download ended before all bytes arrived" }
            }
            if (!sha256(partial).equals(expectedSha256, ignoreCase = true)) {
                partial.delete()
                error("Downloaded APK failed SHA-256 verification")
            }
            if (!hasMatchingInstalledCertificate(context, partial)) {
                partial.delete()
                error("Downloaded APK package or signing certificate does not match the installed app")
            }
            check(partial.renameTo(target)) { "Could not finalize the verified APK" }
            _download.value = DownloadState.Ready(target)
        }.onFailure { error ->
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    private data class ParsedContentRange(val start: Long, val total: Long?)

    private fun parseContentRange(value: String?): ParsedContentRange? {
        val match = Regex("bytes (\\d+)-\\d+/(\\d+|\\*)").matchEntire(value.orEmpty()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val total = match.groupValues[2].takeUnless { it == "*" }?.toLongOrNull()
        return ParsedContentRange(start, total)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun hasMatchingInstalledCertificate(context: Context, apk: File): Boolean = runCatching {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val installed = packageManager.getPackageInfo(context.packageName, flags)
        val candidate = packageManager.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        if (candidate.packageName != context.packageName) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val installedInfo = installed.signingInfo ?: return false
            val candidateInfo = candidate.signingInfo ?: return false
            signingIdentityCompatible(
                installedHasMultipleSigners = installedInfo.hasMultipleSigners(),
                installedCurrentSigners = certificateFingerprints(installedInfo.apkContentsSigners),
                candidateHasMultipleSigners = candidateInfo.hasMultipleSigners(),
                candidateCurrentSigners = certificateFingerprints(candidateInfo.apkContentsSigners),
                candidateSigningHistory = certificateFingerprints(candidateInfo.signingCertificateHistory),
            )
        } else {
            @Suppress("DEPRECATION")
            installed.signatures.orEmpty().toSet() == candidate.signatures.orEmpty().toSet() &&
                installed.signatures.orEmpty().isNotEmpty()
        }
    }.getOrDefault(false)

    private fun certificateFingerprints(signatures: Array<Signature>): Set<String> = signatures.map { signature ->
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }.toSet()

    internal fun signingIdentityCompatible(
        installedHasMultipleSigners: Boolean,
        installedCurrentSigners: Set<String>,
        candidateHasMultipleSigners: Boolean,
        candidateCurrentSigners: Set<String>,
        candidateSigningHistory: Set<String>,
    ): Boolean {
        if (installedCurrentSigners.isEmpty() || candidateCurrentSigners.isEmpty()) return false
        return if (installedHasMultipleSigners || candidateHasMultipleSigners) {
            installedHasMultipleSigners && candidateHasMultipleSigners &&
                installedCurrentSigners == candidateCurrentSigners
        } else {
            installedCurrentSigners.size == 1 && candidateCurrentSigners.size == 1 &&
                installedCurrentSigners.single() in candidateSigningHistory
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload() {
        downloadCancelled = true
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /**
     * Hands a downloaded APK to the system installer.
     *
     * Sideloaded apps need the user's blessing per app ("install unknown apps");
     * without it the installer intent silently does nothing on most ROMs, so
     * the user is sent to that one switch first and taps Install again after.
     * Some TV builds have no such settings screen; there the installer is
     * launched anyway and shows its own "not allowed" prompt with a way in.
     */
    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val opened = runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.isSuccess
            if (opened) return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
        )
    }

    /** A version split into its numeric dotted parts and whether it carries a "-suffix" (e.g. "-beta2"). */
    private data class ParsedVersion(val parts: List<Int>, val isPreRelease: Boolean)

    private fun parseVersion(raw: String): ParsedVersion {
        val dash = raw.indexOf('-')
        val base = if (dash >= 0) raw.substring(0, dash) else raw
        return ParsedVersion(base.split(".").map { it.toIntOrNull() ?: 0 }, dash >= 0)
    }

    /**
     * Numeric, dot-separated comparison — "1.10" outranks "1.9" — with one
     * extra rule: a "-betaN" build (see the debug build type's
     * `versionNameSuffix` in app/build.gradle.kts) is treated as older than a
     * plain release of the same numbers, since the beta by definition predates
     * the tag it was testing toward. Without this, a beta and the release it
     * matches compare equal and testers never get nudged onto the real build.
     */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.parts.size, c.parts.size)) {
            val a = l.parts.getOrElse(i) { 0 }
            val b = c.parts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return c.isPreRelease && !l.isPreRelease
    }
}
