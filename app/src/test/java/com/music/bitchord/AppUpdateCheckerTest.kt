package com.music.bitchord

import com.music.bitchord.data.AppUpdateChecker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    private val digest = "a".repeat(64)

    @Test
    fun `direct APK selection requires SHA-256 digest`() {
        val release = release(asset(digestValue = null))

        assertNull(AppUpdateChecker.verifiedApkAsset(release))
    }

    @Test
    fun `direct APK selection rejects non-personal or insecure asset URLs`() {
        val wrongHost = asset(url = "https://example.invalid/BitChord.apk")
        val cleartext = asset(url = "http://github.com/OPZENITSUUU/BitChord/releases/download/v2/BitChord-v2.apk")

        assertNull(AppUpdateChecker.verifiedApkAsset(release(wrongHost)))
        assertNull(AppUpdateChecker.verifiedApkAsset(release(cleartext)))
    }

    @Test
    fun `universal personal release APK is selected with normalized digest`() {
        val split = asset(name = "Void.Music_arm64-v8a.apk")
        val universal = asset(name = "Void.Music.apk", digestValue = "sha256:$digest")

        val selected = AppUpdateChecker.verifiedApkAsset(release(split, universal))

        assertEquals("Void.Music", selected?.name)
        assertEquals("https://github.com/OPZENITSUUU/BitChord/releases/download/v2/Void.Music.apk", selected?.url)
        assertEquals(digest, selected?.sha256)
    }

    @Test
    fun `digest parser rejects malformed SHA-256 values`() {
        assertNull(AppUpdateChecker.normalizeSha256Digest("sha256:1234"))
        assertEquals(digest, AppUpdateChecker.normalizeSha256Digest("SHA256:$digest"))
    }

    @Test
    fun `dismissed release is not announced again`() {
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.9.0", "1.8.1", "1.9.0"))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.9.0", "1.8.1", null))
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.8.0", "1.8.1", null))
    }

    @Test
    fun `newer beta tags compare by prerelease sequence`() {
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.1-beta4", "1.8.1-beta1", null))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.1-beta10", "1.8.1-beta9", null))
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.8.1-beta2", "1.8.1-beta4", null))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.1", "1.8.1-beta4", null))
    }

    @Test
    fun `release checks are throttled to once per day`() {
        assertTrue(AppUpdateChecker.shouldCheckNow(0L, 100L))
        assertFalse(AppUpdateChecker.shouldCheckNow(10_000L, 10_000L + AppUpdateChecker.CHECK_INTERVAL_MS - 1))
        assertTrue(AppUpdateChecker.shouldCheckNow(10_000L, 10_000L + AppUpdateChecker.CHECK_INTERVAL_MS))
    }

    @Test
    fun `single signer update requires an installed certificate in candidate lineage`() {
        assertTrue(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = false,
                installedCurrentSigners = setOf("old"),
                candidateHasMultipleSigners = false,
                candidateCurrentSigners = setOf("new"),
                candidateSigningHistory = setOf("old", "new"),
            ),
        )
        assertFalse(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = false,
                installedCurrentSigners = setOf("old"),
                candidateHasMultipleSigners = false,
                candidateCurrentSigners = setOf("new"),
                candidateSigningHistory = setOf("new"),
            ),
        )
    }

    @Test
    fun `multi signer update requires the exact current signer set`() {
        assertTrue(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = true,
                installedCurrentSigners = setOf("a", "b"),
                candidateHasMultipleSigners = true,
                candidateCurrentSigners = setOf("a", "b"),
                candidateSigningHistory = emptySet(),
            ),
        )
        assertFalse(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = true,
                installedCurrentSigners = setOf("a", "b"),
                candidateHasMultipleSigners = true,
                candidateCurrentSigners = setOf("a"),
                candidateSigningHistory = setOf("a", "b"),
            ),
        )
    }

    private fun asset(
        name: String = "Void.Music.apk",
        url: String = "https://github.com/OPZENITSUUU/BitChord/releases/download/v2/$name",
        digestValue: String? = "sha256:$digest",
    ): String {
        val digestPart = digestValue?.let { "\"digest\":\"$it\"," }.orEmpty()
        return """{"name":"$name","browser_download_url":"$url",${digestPart}"state":"uploaded"}"""
    }

    private fun release(vararg assets: String): JsonObject =
        Json.parseToJsonElement("""{"assets":[${assets.joinToString(",")}] }""") as JsonObject
}
