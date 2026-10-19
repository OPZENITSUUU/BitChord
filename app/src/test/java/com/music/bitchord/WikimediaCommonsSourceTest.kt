package com.music.bitchord

import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.StreamRequest
import com.music.bitchord.data.sources.WikimediaCommonsSource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WikimediaCommonsSourceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun source() = WikimediaCommonsSource(
        config = SourceConfig(kind = SourceKind.WIKIMEDIA_COMMONS, enabled = true),
        apiEndpoint = server.url("/w/api.php").toString(),
    )

    @Test
    fun `search surfaces only public domain FLAC and sends Commons file search`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchResponse(
            page(123, "File:Public song.flac", license = "Public domain"),
            page(124, "File:Restricted song.flac", license = "CC BY-SA 4.0"),
            page(125, "File:Wrong codec.ogg", license = "Public domain", mime = "audio/ogg"),
        )))

        val songs = source().search("public song", limit = 10)
        val request = server.takeRequest()

        assertEquals(1, songs.size)
        assertEquals("Public song", songs.single().title)
        assertEquals("Hans Duhan", songs.single().artist)
        assertEquals("LOSSLESS", songs.single().sourceQuality)
        assertFalse(songs.single().sourceHiRes)
        assertTrue(request.requestUrl!!.queryParameter("gsrsearch").orEmpty().contains("filemime:audio/flac"))
        assertEquals("6", request.requestUrl!!.queryParameter("gsrnamespace"))
        assertTrue(request.getHeader("User-Agent").orEmpty().contains("Wikimedia Commons"))
    }

    @Test
    fun `stream rechecks license and returns only source-reported FLAC metadata`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchResponse(page(123, "File:Public song.flac", license = "Public domain"))))

        val stream = source().stream("123", StreamRequest.Lossless)
        val request = server.takeRequest()

        assertEquals("123", request.requestUrl!!.queryParameter("pageids"))
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/0/00/Public_song.flac", stream!!.url)
        assertEquals("flac", stream.format.codec)
        assertEquals(44100, stream.format.sampleRateHz)
        assertEquals(16, stream.format.bitDepth)
        assertEquals(795, stream.format.kbps)
        assertEquals(249, stream.durationSec)
    }

    @Test
    fun `stream refuses an unlicensed item and does not fetch arbitrary track ids`() = runBlocking {
        val source = source()
        server.enqueue(MockResponse().setBody(searchResponse(page(123, "File:Restricted.flac", license = "CC BY-NC 4.0"))))

        assertNull(source.stream("123", StreamRequest.Lossless))
        assertEquals(1, server.requestCount)
        assertNull(source.stream("https://attacker.invalid/file.flac", StreamRequest.Lossless))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `stream refuses a public domain entry whose media URL leaves Wikimedia uploads`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                searchResponse(page(123, "File:Public song.flac", license = "Public domain", url = "https://attacker.invalid/file.flac")),
            ),
        )

        assertNull(source().stream("123", StreamRequest.Lossless))
    }

    @Test
    fun `lossless file above a capped request is not offered`() = runBlocking {
        server.enqueue(MockResponse().setBody(searchResponse(page(123, "File:Public song.flac", license = "Public domain"))))
        server.enqueue(MockResponse().setBody(searchResponse(page(123, "File:Public song.flac", license = "Public domain"))))
        val source = source()

        assertTrue(source.search("public song", limit = 10, request = StreamRequest.Capped(320)).isEmpty())
        assertNull(source.stream("123", StreamRequest.Capped(320)))
    }

    private fun searchResponse(vararg pages: String) =
        """{"query":{"pages":{${pages.joinToString(",")}}}}"""

    private fun page(
        id: Long,
        title: String,
        license: String,
        mime: String = "audio/x-flac",
        url: String = "https://upload.wikimedia.org/wikipedia/commons/0/00/Public_song.flac",
    ) = """"$id":{"pageid":$id,"title":"$title","imageinfo":[{"url":"$url","mime":"$mime","extmetadata":{"LicenseShortName":{"value":"$license"},"Credit":{"value":"Hans Duhan"},"Artist":{"value":"Schubert"}},"metadata":[{"name":"sample_rate","value":44100},{"name":"bits_per_sample","value":16},{"name":"bitrate","value":795027.6},{"name":"playtime_string","value":"4:09"}]}]}"""
}
