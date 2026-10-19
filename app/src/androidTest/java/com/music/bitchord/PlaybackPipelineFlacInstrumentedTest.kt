package com.music.bitchord

import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.AudioQuality
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.StreamRequest
import com.music.bitchord.data.sources.WikimediaCommonsSource
import com.music.bitchord.playback.PlaybackService
import com.music.bitchord.playback.toMediaItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Drives a local 24-bit/96-kHz FLAC through the app's actual MediaSession,
 * ResolvingDataSource, AudioCache, Media3 decoder and configured audio sink.
 * Run on an Android device/emulator; a JVM unit test cannot verify AudioTrack.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPipelineFlacInstrumentedTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var browser: MediaBrowser
    private var fixture: File? = null
    private val playbackStarted = CountDownLatch(1)
    private val playbackError = AtomicReference<PlaybackException?>()

    @Before
    fun connectToPlaybackService() {
        val context = instrumentation.targetContext
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val latch = CountDownLatch(1)
        val result = AtomicReference<MediaBrowser?>()

        instrumentation.runOnMainSync {
            val future = MediaBrowser.Builder(context, token).buildAsync()
            future.addListener(
                {
                    runCatching { future.get() }.onSuccess(result::set)
                    latch.countDown()
                },
                { command -> command.run() },
            )
        }

        assertTrue("PlaybackService connection timed out", latch.await(15, TimeUnit.SECONDS))
        browser = requireNotNull(result.get()) { "MediaBrowser connection returned no browser" }
    }

    @After
    fun releaseBrowser() {
        if (::browser.isInitialized) {
            instrumentation.runOnMainSync {
                browser.pause()
                browser.clearMediaItems()
                browser.release()
            }
        }
        fixture?.delete()
    }

    @Test
    fun localFlacPlaysReportsContainerFactsAndSupportsSeeking() {
        val context = instrumentation.targetContext
        val localFixture = File(context.cacheDir, "bitchord-flac-fixture.flac")
        fixture = localFixture
        instrumentation.context.assets.open("flac/fixture-24bit-96k.flac").use { input ->
            localFixture.outputStream().use { output -> input.copyTo(output) }
        }

        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError.set(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) playbackStarted.countDown()
            }
        }
        instrumentation.runOnMainSync {
            browser.addListener(listener)
            browser.setMediaItem(
                MediaItem.Builder()
                    .setMediaId("bitchord-local-flac-fixture")
                    .setUri(Uri.fromFile(localFixture))
                    .setMimeType(MimeTypes.AUDIO_FLAC)
                    .build(),
            )
            browser.prepare()
        }

        await("FLAC decoder ready") {
            browser.playbackState == Player.STATE_READY && browser.duration > 0
        }
        playbackError.get()?.let { throw AssertionError("FLAC playback failed", it) }

        val durationMs = onMain { browser.duration }
        assertTrue("Expected about 8 seconds, got $durationMs ms", durationMs in 7_500L..8_500L)

        await("FLAC container title metadata") {
            onMain { browser.currentMediaItem?.mediaMetadata?.title?.toString() } == "BitChord FLAC Pipeline Fixture"
        }
        assertEquals(
            "FLAC container metadata should reach the session",
            "BitChord FLAC Pipeline Fixture",
            onMain { browser.currentMediaItem?.mediaMetadata?.title?.toString() },
        )

        await("Media3 encoded input telemetry") {
            val snapshot = NerdStats.current.value
            snapshot?.mimeType?.contains("flac", ignoreCase = true) == true &&
                snapshot.sampleRateHz == 96_000 && snapshot.channels == 2
        }
        val snapshot = requireNotNull(NerdStats.current.value)
        assertTrue(
            "Expected renderer input codec FLAC, got ${snapshot.mimeType}",
            snapshot.mimeType?.contains("flac", ignoreCase = true) == true,
        )
        assertEquals(96_000, snapshot.sampleRateHz)
        assertEquals(2, snapshot.channels)
        // Container depth is not guaranteed to be surfaced by every decoder.
        // If BitChord reports it, it must match the fixture rather than be guessed.
        snapshot.bitDepth?.let { assertEquals(24, it) }
        assertTrue("A local file must not inherit an addon quality claim", snapshot.claimed == null)

        instrumentation.runOnMainSync { browser.seekTo(3_000L) }
        await("seek to three seconds") {
            val position = onMain { browser.currentPosition }
            position in 2_500L..3_800L
        }

        instrumentation.runOnMainSync { browser.play() }
        assertTrue("The service player never entered the playing state", playbackStarted.await(10, TimeUnit.SECONDS))
        playbackError.get()?.let { throw AssertionError("FLAC playback failed", it) }
        instrumentation.runOnMainSync { browser.removeListener(listener) }
    }

    @Test
    fun onlineCommonsFlacReachesTheBitChordMedia3Renderer() {
        val config = requireNotNull(
            SourceRegistry.configs.value.firstOrNull { it.kind == SourceKind.WIKIMEDIA_COMMONS },
        ) { "Commons source was not seeded by BitChordApplication" }
        val wasEnabled = config.enabled
        val oldWifiQuality = AppSettings.audioQualityWifi.value
        val oldCellularQuality = AppSettings.audioQualityCellular.value
        var listener: Player.Listener? = null

        try {
            SourceRegistry.setEnabled(config.id, true)
            AppSettings.setAudioQualityWifi(AudioQuality.LOSSLESS)
            AppSettings.setAudioQualityCellular(AudioQuality.LOSSLESS)

            val source = requireNotNull(SourceRegistry.instance(config.id) as? WikimediaCommonsSource) {
                "Commons source instance is unavailable"
            }
            // This is a live Commons API query, not a fixture. The recording is
            // a public-domain Schubert performance with source metadata for
            // FLAC, 16-bit/44.1kHz and its measured file bitrate.
            val song = runBlocking {
                source.search("Gute Nacht", limit = 10, request = StreamRequest.Lossless).firstOrNull()
            } ?: throw AssertionError("Commons returned no eligible live FLAC result for Gute Nacht")
            val trackId = requireNotNull(SourceRegistry.parseTrackKey(song.videoId)?.second) {
                "Commons search result did not carry a source track key"
            }
            val resolved = runBlocking { source.stream(trackId, StreamRequest.Lossless) }
                ?: throw AssertionError("Commons could not resolve the live search result to a playable stream")
            assertEquals("flac", resolved.format.codec?.lowercase())
            // Commons has no sample-rate/depth tags for this file; those facts
            // must come from the FLAC stream header/decoder, not be inferred.
            assertNotNull("Expected file bitrate metadata", resolved.format.kbps)

            val started = CountDownLatch(1)
            val error = AtomicReference<PlaybackException?>()
            val playbackListener = object : Player.Listener {
                override fun onPlayerError(playbackException: PlaybackException) {
                    error.set(playbackException)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) started.countDown()
                }
            }
            listener = playbackListener
            NerdStats.onTrackTransition()
            instrumentation.runOnMainSync {
                browser.addListener(playbackListener)
                browser.setMediaItem(song.toMediaItem())
                browser.prepare()
                browser.play()
            }

            assertTrue(
                "BitChord Media3 did not start the live Commons stream; error=${error.get()?.message}",
                started.await(30, TimeUnit.SECONDS),
            )
            await("decoded live FLAC input telemetry") {
                val measured = NerdStats.current.value
                measured?.mimeType?.contains("flac", ignoreCase = true) == true &&
                    measured.sampleRateHz == 44_100 && measured.channels == 2
            }
            error.get()?.let { throw AssertionError("Live Commons FLAC playback failed", it) }

            val measured = requireNotNull(NerdStats.current.value)
            assertTrue("Renderer did not report FLAC: ${measured.mimeType}", measured.isLossless)
            assertEquals(44_100, measured.sampleRateHz)
            assertEquals(2, measured.channels)
            measured.bitDepth?.let { assertEquals("Decoded bit depth must agree when Media3 exposes it", 16, it) }
            assertEquals(config.displayName, measured.sourceName)
            assertEquals("flac", measured.claimed?.codec?.lowercase())
            assertEquals("Missing source metadata must stay unknown", null, measured.claimed?.sampleRateHz)
            assertEquals("Missing source metadata must stay unknown", null, measured.claimed?.bitDepth)
            assertNotNull("Source bitrate claim should be retained separately", measured.claimed?.kbps)
        } finally {
            listener?.let { playbackListener ->
                instrumentation.runOnMainSync { browser.removeListener(playbackListener) }
            }
            SourceRegistry.setEnabled(config.id, wasEnabled)
            AppSettings.setAudioQualityWifi(oldWifiQuality)
            AppSettings.setAudioQualityCellular(oldCellularQuality)
        }
    }

    @Test
    fun missingLocalFileSurfacesAPlaybackError() {
        val missing = File(instrumentation.targetContext.cacheDir, "missing-bitchord-flac-${System.nanoTime()}.flac")
        val error = AtomicReference<PlaybackException?>()
        val errorLatch = CountDownLatch(1)
        val listener = object : Player.Listener {
            override fun onPlayerError(playbackException: PlaybackException) {
                error.set(playbackException)
                errorLatch.countDown()
            }
        }

        instrumentation.runOnMainSync {
            browser.addListener(listener)
            browser.setMediaItem(
                MediaItem.Builder()
                    .setMediaId("bitchord-missing-local-file")
                    .setUri(Uri.fromFile(missing))
                    .setMimeType(MimeTypes.AUDIO_FLAC)
                    .build(),
            )
            browser.prepare()
            browser.play()
        }

        assertTrue("Missing local audio should report a player error", errorLatch.await(15, TimeUnit.SECONDS))
        assertNotNull(error.get())
        instrumentation.runOnMainSync { browser.removeListener(listener) }
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(50L)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(block()) }
        return result.get()
    }
}
